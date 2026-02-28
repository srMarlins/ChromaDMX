package com.chromadmx.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.chromadmx.ui.ChromaDmxApp

/**
 * Creates the main UIViewController that hosts the Compose Multiplatform UI on iOS.
 *
 * This is the entry point called from the Swift/Objective-C side of the iOS app.
 * The Swift AppDelegate or SwiftUI App creates this view controller and sets it
 * as the root of the window.
 *
 * Usage from Swift:
 * ```swift
 * import Shared
 *
 * let controller = MainViewControllerKt.MainViewController()
 * window.rootViewController = controller
 * ```
 *
 * Or from SwiftUI via UIViewControllerRepresentable:
 * ```swift
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 * }
 * ```
 */
fun MainViewController() = ComposeUIViewController { ChromaDmxApp() }
