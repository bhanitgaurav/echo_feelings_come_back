# Contributing to Echo

Thank you for your interest in contributing to Echo! We welcome contributions from everyone.

## Getting Started

### Prerequisites
- JDK 17 or higher
- Android Studio (for Android development)
- Xcode (for iOS development, macOS only)
- Docker (optional, for running the database locally)

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

## Development Workflow

1.  **Fork the repository**.
2.  **Create a branch** for your feature or fix (`git checkout -b feature/amazing-feature`).
3.  **Make your changes**.
4.  **Commit your changes** (`git commit -m 'feat: add some amazing feature'`).
5.  **Push to the branch** (`git push origin feature/amazing-feature`).
6.  **Open a Pull Request**.

## Code Style

Please follow the existing code style. We use the standard Kotlin coding conventions.

## Code of Conduct

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.
