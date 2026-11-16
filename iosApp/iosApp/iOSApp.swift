import SwiftUI
import Shared

// iOS app: the whole game is in the shared Kotlin module (Shared.framework); this is just the window.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            GameView().ignoresSafeArea(.all)
        }
    }
}

struct GameView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosPlatformKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
