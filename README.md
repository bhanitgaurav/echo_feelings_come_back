# Echo

### Identity & Vision

> *A quiet space where feelings return without names.*
> *A place to appreciate, to reflect, and to understand how we affect each other—gently, anonymously, and honestly.*

**What is Echo?**
Echo is a calm, anonymous way to share emotions with people you already know—making it easier to express appreciation, acknowledge feelings, and build emotional awareness without confrontation or pressure.

**The Mission**
To help people express feelings they might otherwise keep silent—kindly, anonymously, and without fear—so emotional awareness becomes part of everyday life.

---

### 📲 Download Echo

<a href="https://play.google.com/store/apps/details?id=com.bhanit.apps.echo&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="50">
</a>
<a href="https://apps.apple.com/in/app/echo-feelings-come-back/id6757484280?ct=github_readme">
  <img src="https://developer.apple.com/app-store/marketing/guidelines/images/badge-download-on-the-app-store.svg" alt="Download on the App Store" height="35" style="padding-left: 10px; padding-bottom: 3px;">
</a>

---

**Technical Overview**
**Echo** is a cross-platform mobile application and server targeting Android, iOS, and Backend (Ktor).
Built with **Kotlin Multiplatform (KMP)**.

### 📚 Documentation
- [Engineering Deep Dive (Medium Article)](./docs/MEDIUM_ARTICLE.md): A tech-first breakdown of KMP, Ktor, and optimizations.
- [App Architecture](./docs/APP_ARCHITECTURE.md): Detailed breakdown of screens, logic, and data flow.
- [Feature Implementation Flows](./docs/FEATURE_IMPLEMENTATION_FLOWS.md): Deep dive into specific features like UTM tracking and In-App Ratings.
- [Notification System](./docs/NOTIFICATION_SYSTEM.md): Architecture of the notification ecosystem, from toggles to server triggers.
- [UTM Strategy](./docs/UTM_STRATEGY.md): Attribution tracking strategy.

### Project Composition

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/echo_website](./echo_website) is the **Vite** landing page project.

### 🤝 Contributing
We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on how to get started, build the project, and submit pull requests.

### 📜 License
This project is licensed under the [MIT License](LICENSE).

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

---

## Features

### � Core Experience
- **Echoes**: Send and receive feelings/emotions to stay connected.
- **Habits & Streaks**: Track daily consistency and build meaningful habits.
- **Privacy**: Block users to maintain a safe environment.

### �📊 Analytics & Crashlytics
This project uses a unified **EchoAnalytics** interface shared between Android and iOS.
- **Android**: Uses Google Analytics for Firebase directly.
- **iOS**: Uses a Swift wrapper (`IosEchoAnalytics`) injected into the KMP module via Koin.

### 🔔 Push Notifications
- **Android**: Handled via `AndroidFcmTokenProvider`.
- **iOS**:
    - Handled via `IosFcmTokenProvider` and `AppDelegate`.
    - Supports deep linking (e.g., tapping a notification navigates to specific screens).
    - **Note**: Notifications do not work on the iOS Simulator (APNs limitation). Test on a physical device.

### � Custom Notifications (iOS & Android)
The app uses a custom notification sound (`echo_notification.wav`) to provide a unique, gentle user experience.
- **Android**:
    - Custom sound is configured via Notification Channels (Channel ID: `echo_messages`).
    - The server sends **Data-Only** payloads with `priority: 'high'` to ensure the Android app's background service (`onMessageReceived`) always wakes up to build the notification locally with the custom sound URI.
- **iOS**:
    - Custom sound is bundled in the app (`echo_notification.wav`).
    - The server sends standard **Notification** payloads (`aps` dictionary) specifying the sound file.
    - **Server Logic**: The backend (`FCMService`) detects the user's platform and dynamically constructs the appropriate payload (Data-Only for Android vs. Notification+APNs for iOS).

### �💬 WhatsApp OTP Service
The project includes a Node.js sidecar service to send OTPs via WhatsApp using [Baileys](https://github.com/WhiskeySockets/Baileys).
- **Location**: `/server/baileys_service`
- **Function**: Bridges the Main Ktor Server and WhatsApp Web API.
- **Setup**: Requires running `npm install` and `npm start` in the `baileys_service` directory.
- **Integration**: The Kotlin server communicates with this service to send verification codes.

- **Integration**: The Kotlin server communicates with this service to send verification codes.

### 🖼️ Media Uploads (Cloudinary)
- **Profile Pictures**: Users can upload profile images.
- **Backend**: Handled via `CloudinaryService` on the server.
- **Client**: `CloudinaryRepository` handles upload requests from Android/iOS.

### 🎫 Support System
- **Ticketing**: Users can create and view support tickets.
- **Architecture**: Handled via `SupportRepository` and `SupportViewModel`.
- **Backend**: Stores ticket history and status updates.

### 🔎 Global User Search
- **Functionality**: Search for users by username or phone number.
- **Privacy**: Implements strict phone number matching logic (requires complete match of at least 10 digits) at the database layer to prevent contact enumeration and privacy leaks.

### 🎨 Theme & Customization
- **Themes**: Users can switch between Light, Dark, and System Default themes.
- **Dynamic**: Real-time theme application across the Compose Multiplatform UI.

### 🔗 Referrals & Attribution
- **Auto-Referral**: Automatically parses referral codes from installation sources (Google Play Install Referrer) or the clipboard (iOS) to smooth the onboarding process.
- **Tracking**: Integrated UTM logic for better user acquisition insights.

### 📜 Dynamic Content
Policies (Terms, Privacy) and "About Us" content are fetched dynamically from the server, allowing for real-time updates without app releases.

### 🍎 iOS Specific Setup
The project handles different environments (Dev vs Prod) for Firebase:
- `GoogleService-Info-Dev.plist` (Development)
- `GoogleService-Info.plist` (Production)

**Important**: A custom build script in Xcode automatically copies the correct plist based on the build configuration. If you see "Bundle ID inconsistent" warnings in debug logs, verify that the `Debug` configuration in Xcode has `PRODUCT_BUNDLE_IDENTIFIER` set to `com.bhanit.apps.echo.dev`.
