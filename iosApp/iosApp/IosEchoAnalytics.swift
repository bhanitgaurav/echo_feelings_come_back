import Foundation
import FirebaseAnalytics
import FirebaseCrashlytics
import ComposeApp

class IosEchoAnalytics: EchoAnalytics {
    
    private let env: String
    
    init(env: String) {
        self.env = env
        Analytics.setUserProperty(env, forName: "env")
    }
    
    func logEvent(name: String, params: [String : Any]) {
        Analytics.logEvent(name, parameters: params)
    }
    
    func setUserId(id: String) {
        Analytics.setUserID(id)
        Crashlytics.crashlytics().setUserID(id)
    }
    
    func setUserProperty(name: String, value: String) {
        Analytics.setUserProperty(value, forName: name)
    }
    
    func logCrash(message: String) {
        Crashlytics.crashlytics().log(message)
    }
}
