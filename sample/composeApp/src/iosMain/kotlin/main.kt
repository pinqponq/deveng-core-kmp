import androidx.compose.ui.window.ComposeUIViewController
import global.deveng.core.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
