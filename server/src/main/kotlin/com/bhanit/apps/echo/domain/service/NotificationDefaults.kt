package com.bhanit.apps.echo.domain.service

import com.bhanit.apps.echo.domain.repository.NotificationMessage
import java.util.UUID

object NotificationDefaults {
    fun getInitialMessages(): List<NotificationMessage> {
        val list = mutableListOf<NotificationMessage>()
        
        // Helper
        fun add(category: String, type: String, template: String, isRotational: Boolean) {
            list.add(NotificationMessage(UUID.randomUUID(), category, type, template, isRotational))
        }

        // 1. Gentle Streak Reminder (Rotational)
        val dailyStreak = "STREAK_REMINDER"
        val streakCategory = "STREAK"
        add(streakCategory, dailyStreak, "Here if you want a quiet moment.", true)
        add(streakCategory, dailyStreak, "Just stopping by — no pressure.", true)
        add(streakCategory, dailyStreak, "A small pause can be enough.", true)
        add(streakCategory, dailyStreak, "You don’t have to do anything today.", true)
        add(streakCategory, dailyStreak, "Still here, whenever you are.", true)
        add(streakCategory, dailyStreak, "Just checking in — nothing needed.", true)
        add(streakCategory, dailyStreak, "You can stop by for a moment, if you want.", true)
        add(streakCategory, dailyStreak, "This space is here when you are.", true)
        add(streakCategory, dailyStreak, "No rush — just presence.", true)
        add(streakCategory, dailyStreak, "A moment of awareness can be enough.", true)
        add(streakCategory, dailyStreak, "You don’t need the right words today.", true)
        add(streakCategory, dailyStreak, "It’s okay to arrive quietly.", true)

        // 2. Unopened App — Feeling Received (Rule: Curiosity-First)
        // Generic
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "Someone shared a feeling with you today.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "You were felt today.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "A quiet note is waiting for you.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "A feeling found its way to you.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "Someone thought of you today.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "There’s a quiet update waiting.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "A small moment is waiting for you.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "Someone shared something quietly.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "There’s a feeling waiting when you’re ready.", true)
        add("SOCIAL", "UNOPENED_REMINDER_GENERIC", "A note arrived earlier today.", true)

        // Positive
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Kindness found its way to you.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Appreciation showed up for you today 💜", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Some warm feelings came your way.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "You were appreciated today.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Kind words came your way.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Someone noticed something about you.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "Someone felt grateful for you today.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "A kind moment came your way.", true)
        add("SOCIAL", "UNOPENED_REMINDER_POSITIVE", "You were appreciated in a quiet way.", true)

        // 3. Reflection One-Liners (Universal)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Small emotions matter.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Presence counts.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Not every feeling needs words.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Showing up is enough.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Awareness is a quiet strength.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Some moments are meant to stay quiet.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "You’re allowed to just notice.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Stillness is also presence.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "You don’t need clarity today.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Some days are meant to be gentle.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "You’re allowed to move slowly.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Being present is already something.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "Quiet awareness still counts.", true)
        add("REFLECTION", "UNIVERSAL_ONE_LINER", "You don’t have to resolve anything today.", true)

        // 4. Same-Day Reflection (In-App)
        // Sent Positive
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You shared kindness today.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "Your words made a difference.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You expressed something kind.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You chose kindness today.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "Your presence felt warm.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You showed care through your words.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You brought warmth into someone’s day.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "Your kindness landed softly.", true)
        add("REFLECTION", "SAME_DAY_SENT_POSITIVE", "You showed care in your own way.", true)

        // Sent Difficult
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You were honest about how you felt.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You expressed something that mattered.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "Naming feelings isn’t easy — you did.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You didn’t avoid how you felt.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You gave honesty some space today.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "That kind of honesty matters.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You allowed space for a hard feeling.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "You stayed with what was real.", true)
        add("REFLECTION", "SAME_DAY_SENT_DIFFICULT", "That kind of honesty takes strength.", true)

        // Received Positive
        add("REFLECTION", "SAME_DAY_RECEIVED_POSITIVE", "You were appreciated today.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_POSITIVE", "Someone noticed you.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_POSITIVE", "Warm feelings came your way.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_POSITIVE", "You mattered to someone today.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_POSITIVE", "Someone felt warmth toward you.", true)

        // Received Difficult
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "Someone trusted you with a feeling.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "A feeling was shared with you.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "You were part of an honest moment.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "Someone felt safe sharing with you.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "You were trusted with honesty today.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "That kind of trust is meaningful.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "Someone felt safe being honest with you.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_DIFFICULT", "You were part of a real moment today.", true)
        
        // Sent Heavy
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "You carried something heavy today.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "Your honesty matters.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "It takes courage to feel this deeply.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "You acknowledged something that’s been sitting quietly.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "You didn’t rush past a hard feeling.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "You didn’t look away from something important.", true)
        add("REFLECTION", "SAME_DAY_SENT_HEAVY", "You gave weight to what you felt.", true)

        // Received Heavy
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "You held space for someone today.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "Someone trusted you with their weight.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "You were there for a hard moment.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "You were present for something difficult.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "Someone trusted you with something real.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "You were trusted with something meaningful.", true)
        add("REFLECTION", "SAME_DAY_RECEIVED_HEAVY", "You showed up for a difficult truth.", true)

        // 5. Weekly Recap
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "You made people feel appreciated this week.", true)
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "Kindness showed up around you.", true)
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "You shared warmth more than once.", true)
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "Your kindness showed up more than once.", true)
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "People felt your presence this week.", true)
        
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "It was an emotionally mixed week.", true)
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "Different feelings showed up — that’s human.", true)
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "You stayed present through it.", true)
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "It wasn’t one-note — and that’s okay.", true)
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "Different emotions shared the space this week.", true)
        
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "This week carried some weight.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "Not every feeling was easy.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "You still showed up.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "This week asked a little more from you.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "You stayed present even when it wasn’t easy.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "Some weeks take more from us.", true)
        add("REFLECTION", "WEEKLY_RECAP_HEAVY", "You remained present through a heavier stretch.", true)
        
        // Mixed Additions
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "This week held more than one feeling.", true)
        add("REFLECTION", "WEEKLY_RECAP_MIXED", "It was layered — and you stayed present.", true)
        
        // Positive Additions
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "Your presence brought warmth this week.", true)
        add("REFLECTION", "WEEKLY_RECAP_POSITIVE", "Kindness found room to exist this week.", true)

        // Operational (Fixed)
        // "Hi {name}, someone replied!"
        add("SOCIAL", "ECHO_REPLY", "Echo Reply: {name} replied to your echo!", false)
        add("SOCIAL", "CONNECTION_REQUEST", "New Connection: {name} wants to connect with you.", false)
        add("SOCIAL", "CONNECTION_ACCEPTED", "Connection Accepted: {name} accepted your request!", false)
        
        // Rewards / Referral
        add("REWARDS", "REFERRAL_JOINED", "{name} joined Echo using your invite.", false)
        add("REWARDS", "REFERRAL_IN_APP_SUB", "Credits can help restore streaks or send more feelings.", true)
        
        add("REWARDS", "REFERRAL_WELCOME", "Welcome to Echo. You’ve received credits to get started 💜", false)
        add("REWARDS", "REFERRAL_IN_APP_NEW_USER", "Credits can help you send more feelings or restore streaks.", true)

        // 6. Engagement Prompts (Sent at 8 PM if no echo sent today)
        // Original 5
        add("SOCIAL", "SOCIAL_PROMPT", "Who made you smile today? Let them know.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A small kind word can change someone's evening.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Feeling grateful for someone? Send an echo.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Has someone crossed your mind today?", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Share a feeling, make a connection.", true)
        
        // Warm & Human
        add("SOCIAL", "SOCIAL_PROMPT", "Someone might appreciate hearing from you tonight.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A quiet message can mean more than you think.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Thinking of someone? You can let them know.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Even a small feeling can carry warmth.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A moment of kindness can travel far.", true)

        // Reflective & Soft
        add("SOCIAL", "SOCIAL_PROMPT", "Did today bring up a feeling worth sharing?", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Is there something you’ve been meaning to say?", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Some feelings feel lighter when shared.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "What did today leave you feeling?", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Noticing someone is sometimes enough.", true)

        // Gratitude / Appreciation
        add("SOCIAL", "SOCIAL_PROMPT", "Did someone do something you appreciated today?", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Gratitude doesn’t need perfect words.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A simple thank-you can brighten an evening.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Someone may have made your day a little better.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Appreciation shared gently still counts.", true)

        // Calm Encouragement
        add("SOCIAL", "SOCIAL_PROMPT", "You don’t need the right words to share a feeling.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A feeling shared quietly can still matter.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "There’s no rush — just a moment, if you want.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "Sharing how you feel can be simple.", true)
        add("SOCIAL", "SOCIAL_PROMPT", "A small echo can create a warm moment.", true)
        
        // 7. Reward Notifications (Echo-Safe: Emotion First -> Number Second)
        // Streak
        // "You kept showing up"
        add("REWARDS", "REWARD_STREAK", "A small appreciation for your consistency\n+{amount} Credits", false)
        
        // Consistency
        // "Consistency noticed"
        add("REWARDS", "REWARD_CONSISTENCY", "Your steady presence matters\n+{amount} Credits", false)
        
        // Seasonal
        // "{season_name} Moment 💖"
        add("REWARDS", "REWARD_SEASONAL", "{season_name} Appreciation\n+{amount} Credits", false)
        
        // First Echo
        // "Your first echo"
        add("REWARDS", "REWARD_FIRST_ECHO", "Thanks for sharing how you feel\n+{amount} Credits", false)
        
        // First Response
        // "Your response mattered"
        add("REWARDS", "REWARD_FIRST_RESPONSE", "You showed up for someone\n+{amount} Credits", false)
        
        // Season Start
        add("REWARDS", "SEASON_START", "{season_name} starts today! 🌸", false)

        // Generic
        // "A small moment of appreciation"
        add("REWARDS", "REWARD_GENERIC", "Added to your journey\n+{amount} Credits", false)

        return list
    }
}
