import SwiftUI
import FirebaseCore
import FirebaseMessaging
import FirebaseAnalytics
import UserNotifications
import AVFoundation
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        #if DEBUG
        let filePath = Bundle.main.path(forResource: "GoogleService-Info-Dev", ofType: "plist")
        #else
        let filePath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist")
        #endif
        
        if let path = filePath, let options = FirebaseOptions(contentsOfFile: path) {
            FirebaseApp.configure(options: options)
            print("Configured Firebase with \(path)")
        } else {
            FirebaseApp.configure()
            print("Configured Firebase with default GoogleService-Info.plist")
        }
        
        // Register for remote notifications
        UNUserNotificationCenter.current().delegate = self
        
        // Removed automatic requestAuthorization. Handled by IosPermissionHandler.
        
        application.registerForRemoteNotifications()
        Messaging.messaging().delegate = self
        
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Map APNs token to Firebase
        Messaging.messaging().apnsToken = deviceToken
        print("APNs Token mapped to Firebase")
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error)")
    }
    
    // MARK: - MessagingDelegate
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token: \(String(describing: fcmToken))")
        
        let dataDict: [String: String] = ["token": fcmToken ?? ""]
        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: dataDict
        )
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .badge, .sound])
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        print("User tapped notification: \(userInfo)")
        
        if let type = userInfo["type"] as? String {
            let userId = userInfo["userId"] as? String
            let username = userInfo["username"] as? String
            let connectionId = userInfo["connectionId"] as? String
            let referralCode = userInfo["referralCode"] as? String ?? userInfo["code"] as? String
            let navigateTo = userInfo["navigate_to"] as? String ?? userInfo["navigateTo"] as? String
            
            DeepLinkHandlerKt.handleDeepLinkFromSwift(type: type, userId: userId, username: username, connectionId: connectionId, referralCode: referralCode, navigateTo: navigateTo)
        }
        
        completionHandler()
    }

}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        // Bridge Swift Camera to Kotlin
        print("SWIFT_BRIDGE: Registering IOSCameraBridge factory")
        
        // Permission Checker
        IOSCameraBridge.shared.permissionStatus = {
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            return KotlinInt(int: Int32(status.rawValue))
        }
        
        // Request Access
        IOSCameraBridge.shared.requestAccess = { onResult in
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    onResult(KotlinBoolean(bool: granted))
                }
            }
        }
        
        // Settings Opener
        IOSCameraBridge.shared.openSettings = {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url, options: [:], completionHandler: nil)
                }
            }
        }
        
        IOSCameraBridge.shared.scannerFactory = { onScanned, onPermissionGranted in
            print("SWIFT_BRIDGE: Factory called. Creating QRScannerView.")
            let view = QRScannerView(frame: .zero)
            
            // Wire Scanning Callback
            view.onScanned = { code in
                print("SWIFT_BRIDGE: Callback to Kotlin with code: \(code)")
                onScanned(code)
            }
            
            // Wire Permission Callback
            view.onPermissionStatus = { granted in
                print("SWIFT_BRIDGE: Permission status to Kotlin: \(granted)")
                onPermissionGranted(KotlinBoolean(bool: granted))
            }
            
            // Wire Torch Control
            // Using Weak reference to view to avoid retain cycles
            IOSCameraBridge.shared.torchController = { [weak view] enabled in
                view?.toggleTorch(on: enabled.boolValue)
            }
            
            return view
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Track deep link source
                    if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
                       let queryItems = components.queryItems {
                        
                        var properties: [String: String] = [:]
                        
                        if let source = queryItems.first(where: { $0.name == "utm_source" })?.value {
                            properties["install_source"] = source
                        } else if let source = queryItems.first(where: { $0.name == "source" })?.value {
                            properties["install_source"] = source
                        } else if let referrer = queryItems.first(where: { $0.name == "referrer" })?.value {
                            properties["install_source"] = referrer
                        }
                        
                        if let installSource = properties["install_source"] {
                            Analytics.setUserProperty(installSource, forName: "install_source")
                            
                            // Log event
                            Analytics.logEvent("deep_link_opened", parameters: [
                                "url": url.absoluteString,
                                "source": installSource
                            ])
                        }
                        
                        // Handle Referral Code Deep Link
                        var referralCode: String? = nil
                        if let code = queryItems.first(where: { $0.name == "referral_code" })?.value {
                            referralCode = code
                        } else if let code = queryItems.first(where: { $0.name == "code" })?.value {
                            referralCode = code
                        }
                        
                        var navigateTo: String? = nil
                        if let nav = queryItems.first(where: { $0.name == "navigate_to" })?.value {
                            navigateTo = nav
                        }
                        
                        if let referralCode = referralCode {
                             DeepLinkHandlerKt.handleDeepLinkFromSwift(type: "referral", userId: nil, username: nil, connectionId: nil, referralCode: referralCode, navigateTo: navigateTo)
                        } else {
                            // If it's another type of deep link, handle generic parsing if needed, 
                            // but currently DeepLinkHandler is parameter specific.
                            // We can try to parse other params if they exist in query items standard way
                        }
                    }
                }
        }
    }
}