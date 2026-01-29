package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.data.table.ContentType
import com.bhanit.apps.echo.domain.repository.ContentRepository
import java.util.concurrent.ConcurrentHashMap

class ContentService(private val repository: ContentRepository) {
    // Simple in-memory cache to reduce DB hits for static content
    private val cache = ConcurrentHashMap<ContentType, String>()

    suspend fun getContent(type: ContentType): String {
        return cache.getOrPut(type) {
             repository.getContent(type)?.content ?: ""
        }
    }

    suspend fun updateContent(type: ContentType, content: String) {
        repository.updateContent(type, content)
        cache[type] = content // Update cache
    }

    suspend fun seedDefaults() {
        // Always update ABOUT to ensure latest text is applied
        val aboutText = """
Echo is a quiet space to share feelings—without names, pressure, or confrontation.

It allows you to express appreciation, honesty, and reflection toward people you already know, in a way that feels safe and respectful. Messages are anonymous by design, helping emotions be shared without fear of judgment or expectation.

Echo is not a chat app.
It’s not about conversation or performance.

It’s about:

• letting appreciation be felt,

• allowing honesty without conflict,

• and building emotional awareness through small, intentional moments.

Some feelings don’t need words.
Echo exists for the ones that do.
        """.trimIndent()
        
        updateContent(ContentType.ABOUT, aboutText)
        println("Seeded/Updated Default About Text")

        // Always update PRIVACY to ensure latest text
        val privacyText = """
Privacy Policy

Last updated: Dec 31, 2025

Echo is built with privacy as a core principle.
This policy explains what data we collect, why we collect it, and how we protect it.

1. What We Collect

Information You Provide:
• Basic account information (such as phone number for login)
• Support requests or feedback you submit

Usage Data (Anonymous):
• App usage patterns
• Feature interactions
• Aggregated activity (e.g., number of messages sent per day)

We do not collect:
• Message content for analysis
• Emotion text for profiling
• Contact names or personal relationships
• Behavioral data for advertising

2. Messages & Anonymity

• Messages are anonymous by design
• Recipients cannot see who sent a message
• Echo does not surface sender identity

Message data is stored securely to enable delivery, replies, and history.

3. Analytics & Stability

We use anonymous analytics and crash reporting to:
• Improve app performance
• Understand feature usage
• Fix bugs and crashes

This data:
• Is aggregated
• Cannot identify you personally
• Is not sold or shared for advertising

4. Notifications

Notifications are sent to support:
• Emotional presence
• Replies and updates
• Important system messages

You can control most notification categories in Settings.
Some essential notifications may always remain enabled.

5. Data Storage & Security

We take reasonable steps to protect your data using:
• Secure storage
• Encrypted communication
• Access controls

No system is perfect, but privacy and safety guide every decision.

6. Sharing of Data

We do not sell personal data.

We may share limited data:
• With service providers required to operate Echo
• When legally required
• To protect users from harm or abuse

7. Your Choices

You can:
• Control notification preferences
• Contact support for help
• Stop using Echo at any time

If you uninstall the app, your data remains protected and is handled according to retention policies.

8. Children’s Privacy

Echo is not intended for children under 13.
If you believe a child is using Echo, please contact support.

9. Changes to This Policy

If we make meaningful changes, we’ll update this policy and notify users when appropriate.

10. Contact

For privacy questions or concerns, use the in-app support feature.
        """.trimIndent()
        updateContent(ContentType.PRIVACY, privacyText)
        println("Seeded/Updated Default Privacy Text")
        
        // Always update TERMS to ensure latest text
        val termsText = """
Terms & Conditions

Last updated: Dec 31, 2025

Welcome to Echo. By using this app, you agree to the following terms. Please read them carefully.

1. Use of Echo

Echo is designed for personal, respectful emotional expression.
You agree to use Echo in a way that is lawful, considerate, and aligned with its purpose.

You must not:

• Harass, threaten, or abuse others

• Send repeated or harmful messages

• Attempt to identify anonymous senders

• Use Echo for spam, manipulation, or surveillance

Echo reserves the right to restrict or suspend access if these rules are violated.

2. Anonymity & Responsibility

Echo allows anonymous messages.
Anonymity is meant to protect emotional safety—not to excuse harm.

While identities are hidden, misuse can still be detected through patterns and reports.
Users are responsible for how they use the platform.

3. Content & Moderation

Echo does not actively read personal messages, but we may:

• Review reported content

• Act on abuse reports

• Enforce safety limits

We may remove content or restrict accounts that compromise user safety.

4. Accounts & Access

You are responsible for:

• Maintaining access to your account

• Keeping your device secure

Echo is not responsible for issues caused by device loss, third-party access, or system misuse.

5. Credits & Features

Echo may offer credits or features that:

• Restore streaks

• Unlock usage flexibility

• Are granted via referrals or support

Credits have no cash value and may change over time.

6. Changes to Echo

Echo may evolve.
Features, limits, or behaviors may change to improve safety and experience.

We will make reasonable efforts to communicate important updates.

7. Limitation of Liability

Echo is provided “as is.”
We are not responsible for emotional outcomes, misunderstandings, or decisions made based on messages received.

Use Echo as a tool for awareness—not as a substitute for direct communication or professional support.

8. Contact

If you have questions or concerns, you can reach us through the in-app support system.
        """.trimIndent()
        updateContent(ContentType.TERMS, termsText)
        println("Seeded/Updated Default Terms Text")
    }
}
