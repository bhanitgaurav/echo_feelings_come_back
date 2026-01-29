# Feature Implementation Flows

This document outlines the technical implementation and user flows for **App Download Tracking**, **Auto-Referral**, and **In-App Ratings**. It is designed to help developers and stakeholders understand how these features work on Android and iOS.

---

## 1. App Download Tracking (UTM & Source)
**Goal**: Identify the acquisition source of a user (e.g., Facebook Ad, Twitter, Friend Invite).

### Android
*   **Technology**: Google Play Install Referrer API.
*   **Implementation**:
    *   **Logic**: `AndroidEchoAnalytics.kt`
    *   **Dependency**: `com.android.installreferrer:installreferrer`
*   **Flow**:
    1.  **Link Click**: User clicks a Play Store link with a `referrer` parameter (e.g., `https://play.google.com/store/apps/details?id=com.bhanit.apps.echo&referrer=utm_source%3Dtwitter`).
    2.  **Install & Open**: User installs and launches the app.
    3.  **Detection**: On first launch, the app connects to the Install Referrer service.
    4.  **Logging**: The app extracts the full referrer URL and logs it as a **User Property** (`install_referrer`) to Firebase Analytics.
    5.  **Result**: In Firebase Console, events can be filtered by this property to attribute users to their source.

### iOS
*   **Technology**: Firebase Default Attribution / SKAdNetwork.
*   **Implementation**:
    *   **Logic**: Handled automatically by Firebase SDK.
*   **Flow**:
    1.  **Link Click**: User clicks a tracked link (e.g., Firebase Dynamic Link).
    2.  **Attribution**: Firebase SDK collects campaign data (`source`, `medium`, `campaign`).
    3.  **Result**: Data appears in the "User Acquisition" report in Firebase Dashboard.

---

## 2. Auto-Referral Code
**Goal**: Automatically populate the "Referral Code" field during onboarding to reduce friction.

### Android (Seamless Integration)
*   **Technology**: Play Store Install Referrer (Custom Parameter).
*   **Implementation**:
    *   **Logic**: `AndroidEchoAnalytics.kt` (extraction) -> `SessionRepository.kt` (storage) -> `OnboardingViewModel.kt` (autofill).
*   **Flow**:
    1.  **Invite Link Setup**: The invite link includes the code in the referrer param: `&referrer=MY_CODE` (or `referrer=referral_code%3DMY_CODE`).
    2.  **Extraction**: `AndroidEchoAnalytics` parses the referrer string. It looks for `referral_code` or uses the raw string if it appears to be a standalone code.
    3.  **Persistence**: The extracted code is securely saved to `SessionRepository` (DataStore).
    4.  **Autofill**: When `OnboardingViewModel` loads, it checks the repository. If a code exists, it automatically fills the input field.

### iOS (Clipboard Fallback)
*   **Technology**: Clipboard (UIPasteboard).
*   **Implementation**:
    *   **Logic**: `IosPlatformUtils.kt` (clipboard access) -> `OnboardingViewModel.kt`.
*   **Flow**:
    1.  **Landing Page**: The invite link directs to a web landing page.
    2.  **Copy Action**: The landing page uses JavaScript to copy the referral code to the user's clipboard before redirecting to the App Store.
    3.  **Detection**: Upon app launch, `OnboardingViewModel` checks the clipboard for a valid alphanumeric code (pattern matching).
    4.  **Autofill**: If a match is found, the input field is populated.

---

## 3. In-App Ratings
**Goal**: Prompt high-engagement users to rate the app without leaving the experience.

### Shared Strategy (Smart Trigger)
*   **Trigger**: The prompt is requested **immediately after a successful Echo is sent**. This targets users at a moment of high satisfaction/engagement.
*   **Manual Access**: A "Rate Us" button is located in **Settings > Support**.

### Android
*   **Technology**: Google Play In-App Review API.
*   **Implementation**:
    *   **Logic**: `AndroidReviewManager.kt` references `CurrentActivityHolder` to launch the review flow on the active Activity.
*   **Important Constraints**:
    *   **Quotas**: Google strictly limits display frequency. The API may return success, but no UI will show if the user was prompted recently.
    *   **Testing**: In Debug builds, the UI **will not appear** unless the app is downloaded via **Internal App Sharing** on the Play Console. Logs will explicitly confirm usage: `ReviewManager: Launching flow`.

### iOS
*   **Technology**: StoreKit (`SKStoreReviewController`).
*   **Implementation**:
    *   **Logic**: `SwiftReviewManager.swift` requests the review on the active `UIWindowScene`.
*   **Important Constraints**:
    *   **Quotas**: Limited to 3 prompts per year per user.
    *   **Testing**: In Xcode (Debug), the prompt always appears but the "Submit" button is disabled. In Production (TestFlight/App Store), standard quotas apply.
