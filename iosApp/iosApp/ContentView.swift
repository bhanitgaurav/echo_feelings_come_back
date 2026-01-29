import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let bundleId = Bundle.main.bundleIdentifier ?? ""

        // Manual Toggle for Internal Testing (TestFlight) with Prod Bundle ID
        // Set to TRUE when building for Internal Testers to separate analytics
        let isInternalTesting = false

        let env: String
        let baseUrl: String

        if bundleId.hasSuffix(".debug") {
            env = "debug"
            baseUrl = "https://dev-api-echo.bhanitgaurav.com"
        } else {
            // Production Bundle ID (com.bhanit.app.echo)
            if isInternalTesting {
                env = "preprod"
                baseUrl = "https://preprod-api-echo.bhanitgaurav.com"
            } else {
                env = "release"
                baseUrl = "https://api-echo.bhanitgaurav.com"
            }
        }

        let analytics = IosEchoAnalytics(env: env)
        let fcmTokenProvider = IosFcmTokenProvider()
        let configService = IosConfigService()
        let reviewManager = SwiftReviewManager()

        // Get Dynamic Version
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "0"
        let versionString = "\(version) (\(build))"

        return MainViewControllerKt.MainViewController(
            analytics: analytics,
            fcmTokenProvider: fcmTokenProvider,
            configService: configService,
            reviewManager: reviewManager,
            baseUrl: baseUrl,
            envName: env,
            version: versionString
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}



