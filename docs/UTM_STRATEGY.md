# UTM & Attribution Tracking Strategy for Echo

## 1. Overview
To understand user acquisition channels, Echo will implement a basic attribution strategy using Install Referrers and Deep Links.

## 2. Android Attribution (Google Play)
We will use the **Google Play Install Referrer API**.
*   **Mechanism**: When a user installs the app via a Play Store link with UTM parameters (e.g., `&referrer=utm_source%3Dgoogle%26utm_medium%3Dcpc`), the app can retrieve these upon first launch.
*   **Implementation**:
    1.  Add `com.android.installreferrer:installreferrer` dependency.
    2.  On `Application` onCreate (or first Onboarding launch), query `InstallReferrerClient`.
    3.  Send `referrerUrl` to the backend `/auth/register` or `/stats/install`.

## 3. iOS Attribution
*   **App Store Connect**: Use Apple's Campaign Links (pt/ct parameters) for basic analytics in App Store Connect.
*   **Deep Linking (Universal Links)**:
    *   For invites: `https://echoapp.com/invite?code=XYZ&utm_source=whatsapp`.
    *   The app will parse `URLQueryItem` when opened via Universal Links.

## 4. Internal Analytics Events
We will log the following events with source parameters:
*   `app_install` (with referrer)
*   `signup_complete`
*   `subscription_started`

## 5. Privacy
*   Attribution data will be used solely for analytics.
*   We will strictly adhere to Google Play and App Store privacy guidelines regarding IDFA/advertising IDs.
