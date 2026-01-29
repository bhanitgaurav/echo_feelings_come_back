package com.bhanit.apps.echo.data.service

import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest
import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.domain.service.AuthService
import com.bhanit.apps.echo.domain.service.OtpSender
import com.bhanit.apps.echo.core.domain.model.ServiceException
import com.bhanit.apps.echo.core.util.ErrorCode
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.Otps
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.time.Instant
import java.time.temporal.ChronoUnit

class RealAuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val otpSender: OtpSender,
    private val creditService: com.bhanit.apps.echo.domain.service.CreditService
) : AuthService {

    // Thread-safe map for Rate Limiting (Keeping this in memory is fine for now, or could be moved to Redis later)
    private data class RateLimitData(
        val lastOtpTime: Long = 0,
        val generationsWithoutLogin: Int = 0,
        val blockedUntil: Long = 0
    )
    private val rateLimitStorage = ConcurrentHashMap<String, RateLimitData>()

    override suspend fun sendOtp(request: LoginRequest): Boolean {

        val currentTime = System.currentTimeMillis()
        val stats = rateLimitStorage.getOrPut(request.phone) { RateLimitData() }

        // 1. Check if Blocked
        if (currentTime < stats.blockedUntil) {
            throw ServiceException(ErrorCode.OTP_MAX_ATTEMPTS_BLOCKED)
        }

        // 2. Check Rate Limit (30 seconds)
        if (currentTime - stats.lastOtpTime < 30_000) {
            throw ServiceException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED)
        }

        // 3. Abuse Prevention (3 strikes)
        if (stats.generationsWithoutLogin >= 3) {
            val newStats = stats.copy(
                blockedUntil = currentTime + (5 * 60 * 1000), // 5 minutes block
                generationsWithoutLogin = 0 // Reset counter so they can try again after block expires
            )
            rateLimitStorage[request.phone] = newStats
            throw ServiceException(ErrorCode.OTP_MAX_ATTEMPTS_BLOCKED)
        }

        // Generate random 6-digit OTP
        val otp = Random.nextInt(100000, 999999).toString()
        
        // Persist OTP in Database
        dbQuery {
            // Remove existing OTP for this phone if any
            Otps.deleteWhere { phoneNumber eq request.phone }
            
            // Insert new OTP
            Otps.insert {
                it[Otps.phoneNumber] = request.phone
                it[Otps.otpCode] = otp
                // Set expiry to 5 minutes from now (Instant)
                it[Otps.expiresAt] = Instant.now().plus(5, ChronoUnit.MINUTES)
            }
        }
        
        // Update Stats
        rateLimitStorage[request.phone] = stats.copy(
            lastOtpTime = currentTime,
            generationsWithoutLogin = stats.generationsWithoutLogin + 1
        )
        
        // Log to console (simulating SMS)
        println("---------- OTP GENERATED ----------")
        println("Phone: ${request.phone}")
        println("OTP: $otp")
        println("-----------------------------------")
        
        try {
            val sent = otpSender.sendOtp(request.phone, otp)
            if (!sent) {
                println("WARNING: Failed to send OTP via Sidecar to ${request.phone}")
            }
        } catch (e: Exception) {
             println("ERROR: Exception sending OTP: ${e.message}")
        }
        
        return true
    }

    override suspend fun verifyOtp(request: VerifyOtpRequest): AuthResponse {

        val otpRecord = dbQuery {
            Otps.selectAll().where { Otps.phoneNumber eq request.phone }
                .map { 
                    Triple(
                        it[Otps.otpCode], 
                        it[Otps.expiresAt], 
                        it[Otps.phoneNumber]
                    ) 
                }
                .singleOrNull()
        }
        
        // Check if OTP exists and matches
        if (otpRecord != null) {
            val (storedOtp, expiry, _) = otpRecord
            
            if (Instant.now().isAfter(expiry)) {
                 dbQuery { Otps.deleteWhere { phoneNumber eq request.phone } }
                 throw ServiceException(ErrorCode.INVALID_OTP)
            }

            if (storedOtp == request.otp) {
                // Success!
                // Remove OTP from DB to prevent reuse
                dbQuery { Otps.deleteWhere { phoneNumber eq request.phone } }
                
                // Reset Rate Limit Stats on successful login
                rateLimitStorage.remove(request.phone)
                
                return completeLogin(request.phone)
            }
        }
        
        throw ServiceException(ErrorCode.INVALID_OTP)
    }

    private suspend fun completeLogin(phone: String): AuthResponse {
        var userId = userRepository.findUserByPhone(phone)
        var isNew = false
        
        if (userId == null) {
            userId = userRepository.createUser(phone)
            isNew = true
        }
        
        // Increment Token Version (Single Session Enforcement)
        val version = userRepository.incrementTokenVersion(userId)
        val token = jwtService.generateToken(userId, version)

        val isOnboardingCompleted = userRepository.isOnboardingCompleted(userId)
        
        // Record Consent
        userRepository.updateConsent(userId)
        
        // Trigger Return Bonus (Comeback)
        try {
            creditService.checkAndAwardSeasonal(userId, java.time.LocalDate.now(), com.bhanit.apps.echo.util.SeasonalRuleType.COMEBACK)
        } catch (e: Exception) {
            println("Auth: Failed to check seasonal comeback: ${e.message}")
        }
        
        // Reset Notification Settings to Ensured Enabled State (User Requirement)
        try {
            userRepository.resetNotificationSettings(userId)
            println("Auth: Reset notification settings for user $userId")
        } catch (e: Exception) {
            println("Auth: Failed to reset notification settings: ${e.message}")
        }
        
        return AuthResponse(token, userId.toString(), isNew, isOnboardingCompleted)
    }

    override suspend fun logout(userId: String) {
        userRepository.clearFcmToken(java.util.UUID.fromString(userId))
    }

    override suspend fun deleteAccount(userId: String) {
        // Implement delete logic if needed
    }
    
    override suspend fun cleanupExpiredOtps() {
        val count = dbQuery {
            Otps.deleteWhere { Otps.expiresAt less java.time.Instant.now() }
        }
        if (count > 0) {
            println("Cleanup: Removed $count expired OTPs")
        }
    }

    // New method for Debug API (Optional: Read from DB)
    suspend fun getActiveOtps(): Map<String, String> {
        return dbQuery {
             Otps.selectAll().associate { it[Otps.phoneNumber] to it[Otps.otpCode] }
        }
    }
}
