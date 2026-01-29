# Push Notification Flows

This document details the triggers, flows, and payload structures for all notifications in Echo.

## 1. Authentication Flow
Notifications sent during the login/signup authentication process.

| Trigger | Description | Notification Type | Mechanism |
| :--- | :--- | :--- | :--- |
| **Login Start** | User enters phone number. | **WhatsApp OTP** | Sidecar Service (`Baileys`) |
| **Login Success** | User verifies OTP. | None | N/A |
| **Demo Login** | Phone: `+919988776655`. | None | Skipped intentionally |

> **Note**: This is the *only* notification sent during authentication. No welcome emails or "new login" alerts are sent.

---

## 2. Retention & Engagement
Notifications designed to bring users back into the app.

### A. Streak Reminders (Automated)
*   **Trigger**: Hourly Job (approx 8 PM check).
*   **Condition**: User has `Streak >= 2` AND `LastActive < Today`.
*   **Content**: Rotational gentle messages (e.g., *"Here if you want a quiet moment"*).
*   **Payload Type**: `STREAK_REMINDER`

### B. Unopened App Reminders (Automated)
*   **Trigger**: Hourly Job (checks for 10 PM Local Time).
*   **Condition**: User has received a message (`isRead=false`, age > 24h) AND `LocalTime == 10 PM`.
*   **Content**: "Someone shared a feeling with you." or "Kindness found its way to you."
*   **Payload Type**: `UNOPENED_REMINDER`

### C. Engagement Prompts (Automated)
*   **Trigger**: Hourly Job (checks for 8 PM Local Time).
*   **Condition**: User is active but has sent 0 echoes today (`sentCount == 0`).
*   **Content**: Social prompts like "Who made you smile today? Let them know."
*   **Payload Type**: `SOCIAL_PROMPT`
*   **Note**: Uses a pool of 25 prompts to ensure variety.

    <details>
    <summary>Click to view Engagement Prompt Messages</summary>

    1. Who made you smile today? Let them know.
    2. A small kind word can change someone's evening.
    3. Feeling grateful for someone? Send an echo.
    4. Has someone crossed your mind today?
    5. Share a feeling, make a connection.
    6. Someone might appreciate hearing from you tonight.
    7. A quiet message can mean more than you think.
    8. Thinking of someone? You can let them know.
    9. Even a small feeling can carry warmth.
    10. A moment of kindness can travel far.
    11. Did today bring up a feeling worth sharing?
    12. Is there something you’ve been meaning to say?
    13. Some feelings feel lighter when shared.
    14. What did today leave you feeling?
    15. Noticing someone is sometimes enough.
    16. Did someone do something you appreciated today?
    17. Gratitude doesn’t need perfect words.
    18. A simple thank-you can brighten an evening.
    19. Someone may have made your day a little better.
    20. Appreciation shared gently still counts.
    21. You don’t need the right words to share a feeling.
    22. A feeling shared quietly can still matter.
    23. There’s no rush — just a moment, if you want.
    24. Sharing how you feel can be simple.
    25. A small echo can create a warm moment.
    
    </details>

### D. In-App Reflections (Not Push)
Daily reflections based on user sentiment (e.g., *"You carried something heavy today"*) are **generated on app open**, not pushed.
*   **Mechanism**: `GET /api/notifications/in-app` called by client on cold start.
*   **Effect**: App shows a modal with the message; no external system alert.

---

## 3. Social & Interaction Flow
Real-time notifications triggered by user-to-user actions.

### A. New Echo
*   **Trigger**: User sends a feeling (`sendMessage`).
*   **Title**: "New Echo" (Anonymous/Public)
*   **Body**: "Someone shared a feeling with you."
*   **Data Type**: `ECHO`

### B. Reply
*   **Trigger**: User replies to an echo (`replyToMessage`).
*   **Title**: "Echo Reply"
*   **Body**: "{Username} replied to your echo!"
*   **Data Type**: `REPLY`
*   **Extras**: `username`, `userId` included in data payload for deep linking.

### C. Connections
*   **Request**: "New Connection Request" - "{Username} wants to connect..." (`CONNECTION`)
*   **Accept**: "Connection Accepted" - "{Username} accepted your request!" (`CONNECTION`)

---

## 4. Technical Implementation details

### FCM Service (`FCMService.kt`)
Centralized object handling logic for all pushes.

#### Android
*   **Priority**: High
*   **Structure**: **Data-Only** payload.
*   **Behavior**: App background service is responsible for creating the visible system tray notification.

#### iOS
*   **Priority**: High
*   **Structure**:
    *   `notification`: `{ title: "...", body: "..." }` (Required for APNs to show alert)
    *   `apns`: `{ aps: { sound: "echo_notification.wav" } }`
    *   `data`: Full payload map.

#### Error Handling
*   **Invalid Token**: If FCM returns `UNREGISTERED`, the token is **immediately deleted** from the `users` table to maintain hygiene.
