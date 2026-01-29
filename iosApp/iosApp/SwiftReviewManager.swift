import Foundation
import StoreKit
import ComposeApp

class SwiftReviewManager: ReviewManager {
    func tryRequestReview(completionHandler: @escaping (Error?) -> Void) {
        // iOS 14+
        if let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
            SKStoreReviewController.requestReview(in: scene)
        } else {
             // Fallback or just log
             print("ReviewManager: No active scene found")
        }
        completionHandler(nil)
    }
}
