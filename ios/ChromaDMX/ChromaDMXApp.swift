import SwiftUI
import Shared

@main
struct ChromaDMXApp: App {
    init() {
        IosApp.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
