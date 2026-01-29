import Foundation
import FirebaseMessaging
import ComposeApp

class IosFcmTokenProvider: FcmTokenProvider {
    func getToken(completionHandler: @escaping (String?, (any Error)?) -> Void) {
        Messaging.messaging().token { token, error in
            if let error = error {
                print("Error fetching FCM registration token: \(error)")
                completionHandler(nil, error)
            } else if let token = token {
                print("FCM registration token: \(token)")
                completionHandler(token, nil)
            } else {
                completionHandler(nil, nil)
            }
        }
    }
}
