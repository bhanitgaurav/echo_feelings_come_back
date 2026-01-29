package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.domain.repository.UserRepository
import com.bhanit.apps.echo.shared.domain.model.Inspiration

class UserService(
    private val userRepository: UserRepository,
    private val contactService: ContactService
) {
    suspend fun connectOnboardingReferral(referrerId: java.util.UUID, refereeId: java.util.UUID) {
        contactService.connectOnboardingReferral(referrerId, refereeId)
    }

    suspend fun generateUniqueUsername(inspiration: Inspiration? = null): String {
        // Try up to 3 times to generate a unique name using the Generator's internal retries
        // The Generator itself has a retry loop for *format*, but here we check *uniqueness*
        
        repeat(5) {
            // Generator produces a candidate
            val candidate = UsernameGenerator.generate(inspiration)
            
            // Check DB
            if (!userRepository.isUsernameTaken(candidate)) {
                return candidate
            }
        }
        
        // Fallback: Append random digits to a base generated name if pure generation fails
        val base = UsernameGenerator.generate(inspiration)
        return "${base}_${(1000..9999).random()}"
    }
}
