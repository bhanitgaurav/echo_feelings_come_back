import Foundation
import FirebaseRemoteConfig
import ComposeApp

class IosConfigService: ConfigService {
    
    private let remoteConfig: RemoteConfig
    
    init() {
        self.remoteConfig = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600
        self.remoteConfig.configSettings = settings
    }
    
    func fetchAndActivate(completionHandler: @escaping (KotlinBoolean?, Error?) -> Void) {
        remoteConfig.fetchAndActivate { status, error in
            if let error = error {
                print("Config fetch failed: \(error.localizedDescription)")
                completionHandler(false, nil)
            } else {
                completionHandler(true, nil)
            }
        }
    }
    
    func getLong(key: String) -> Int64 {
        return remoteConfig.configValue(forKey: key).numberValue.int64Value
    }
    
    func getString(key: String) -> String {
        return remoteConfig.configValue(forKey: key).stringValue ?? ""
    }
    
    func getBoolean(key: String) -> Bool {
        return remoteConfig.configValue(forKey: key).boolValue
    }
}
