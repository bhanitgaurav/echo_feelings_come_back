package com.bhanit.apps.echo

import com.bhanit.apps.echo.data.model.LoginRequest
import com.bhanit.apps.echo.data.model.VerifyOtpRequest
import com.bhanit.apps.echo.data.model.AuthResponse
import com.bhanit.apps.echo.data.table.Otps
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

class FriendRequestFlowTest {

    @Test
    fun testFriendRequestFlow() = testApplication {
        environment {
            config = MapApplicationConfig(
                "storage.driverClassName" to "org.h2.Driver",
                "storage.jdbcUrl" to "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                "storage.username" to "root",
                "storage.password" to "",
                "jwt.issuer" to "http://0.0.0.0:8080/",
                "jwt.audience" to "http://0.0.0.0:8080/hello",
                "jwt.secret" to "secret123",
                "jwt.realm" to "Access to 'hello'"
            )
        }

        application {
            module()
        }

        val client = createClient {
            // No plugins needed, manual JSON handling
        }

        val json = Json { ignoreUnknownKeys = true }

        // 1. Register/Login User A
        val phoneA = "+15550000001"
        println("Sending OTP to $phoneA")
        client.post("/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest(phoneA)))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // Fetch OTP for A
        var otpA = ""
        transaction {
            // Need to match phone number. Otps.phoneNumber
            val record = Otps.selectAll().single { it[Otps.phoneNumber] == phoneA }
            otpA = record[Otps.otpCode]
        }
        println("OTP for $phoneA is $otpA")

        val responseBodyA = client.post("/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(VerifyOtpRequest(phoneA, otpA)))
        }.bodyAsText()
        
        val responseA = json.decodeFromString<AuthResponse>(responseBodyA)
        val tokenA = responseA.token
        val userIdA = responseA.userId
        println("User A ($userIdA) logged in.")


        // 2. Register/Login User B
        val phoneB = "+15550000002"
        println("Sending OTP to $phoneB")
        client.post("/auth/send-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest(phoneB)))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        var otpB = ""
        transaction {
             val record = Otps.selectAll().single { it[Otps.phoneNumber] == phoneB }
             otpB = record[Otps.otpCode]
        }
        println("OTP for $phoneB is $otpB")

         val responseBodyB = client.post("/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(VerifyOtpRequest(phoneB, otpB)))
        }.bodyAsText()
        
        val responseB = json.decodeFromString<AuthResponse>(responseBodyB)
        val tokenB = responseB.token
        val userIdB = responseB.userId
        println("User B ($userIdB) logged in.")

        // 3. User A Connects to User B
        println("User A requesting connection to User B")
        client.post("/contacts/connect/$userIdB") {
            header("Authorization", "Bearer $tokenA")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // 4. User B Accepts User A
        println("User B accepting connection from User A")
        client.post("/contacts/accept/$userIdA") {
            header("Authorization", "Bearer $tokenB")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // 5. Verify Connections List (User A should see User B)
        println("Verifying Connections List for User A")
        val connectionsResponseA = client.get("/contacts/connections?limit=10&offset=0") {
            header("Authorization", "Bearer $tokenA")
        }.bodyAsText()
        println("Connections A: $connectionsResponseA")
        assertTrue(connectionsResponseA.contains(userIdB), "User A's connection list should contain User B")

        // 6. Verify Connections List for User B (User B should see User A)
        println("Verifying Connections List for User B")
        val connectionsResponseB = client.get("/contacts/connections?limit=10&offset=0") {
            header("Authorization", "Bearer $tokenB")
        }.bodyAsText()
        println("Connections B: $connectionsResponseB")
        assertTrue(connectionsResponseB.contains(userIdA), "User B's connection list should contain User A")

        // 7. Verify Available Contacts (User A should NOT see User B in Add Friends)
        // Note: For this to work, we need User B to be "available" (i.e. in UserContacts).
        // Since we didn't sync contacts, User A's available list is empty by default unless we mock sync.
        // We can mock sync for User A containing User B's hash.
        
        // Mock Sync for A
        val hashB = "hash_of_phone_b" // We don't know the hash algo used in test context easily without utils.
        // Actually we do. We can use PhoneNumberUtils/HashUtils but they are in core/shared.
        // Or we can just insert into UserContacts manually.
        
        transaction {
            // Need Users.phoneHash for User B first. We didn't set it in Login?
            // Login creates user. User table has phoneHash.
            // Let's get User B's phoneHash.
            val userBRecord = com.bhanit.apps.echo.data.table.Users.selectAll().where { 
                com.bhanit.apps.echo.data.table.Users.id eq java.util.UUID.fromString(userIdB) 
            }.single()
            val phoneHashB = userBRecord[com.bhanit.apps.echo.data.table.Users.phoneHash]
            
            // Insert into UserContacts for User A
            val userIdAUuid = java.util.UUID.fromString(userIdA)
            try {
                com.bhanit.apps.echo.data.table.UserContacts.insert {
                    it[com.bhanit.apps.echo.data.table.UserContacts.userId] = userIdAUuid
                    it[com.bhanit.apps.echo.data.table.UserContacts.phoneHash] = phoneHashB
                    it[com.bhanit.apps.echo.data.table.UserContacts.localName] = "User B Local"
                    it[com.bhanit.apps.echo.data.table.UserContacts.createdAt] = java.time.Instant.now()
                }
            } catch (e: Exception) {
               // Ignore unique constraint if exists
               println("Insert UserContacts failed: ${e.message}")
            }
        }
        
        println("Verifying Available Contacts for User A (Should EXCLUDE User B)")
        val availableResponseA = client.get("/contacts/available?limit=10&offset=0") {
            header("Authorization", "Bearer $tokenA")
        }.bodyAsText()
        println("Available A: $availableResponseA")
        
        // Assert B is NOT present
        assertFalse(availableResponseA.contains(userIdB), "User A's available list should NOT contain User B since they are connected")

        println("Friend Request Flow Test Completed Successfully")
    }
}
