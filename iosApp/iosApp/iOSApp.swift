import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    DeepLinkBridgeKt.handleDeepLink(url: url.absoluteString)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL {
                        DeepLinkBridgeKt.handleDeepLink(url: url.absoluteString)
                    }
                }
        }
    }
}
