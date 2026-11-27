package core.presentation.component

import androidx.compose.runtime.Composable

/**
 * A typealias for a composable function that takes no parameters and returns Unit.
 *
 * This typealias is used throughout the component library as a concise way to represent
 * a slot/content area in composable functions. It's commonly used for optional content
 * parameters where you want to allow consumers to provide custom composable content.
 *
 * **Usage example:**
 * ```
 * @Composable
 * fun MyComponent(
 *     content: Slot? = null
 * ) {
 *     content?.invoke()
 * }
 * ```
 *
 * This allows callers to provide custom content like:
 * ```
 * MyComponent(
 *     content = {
 *         Text("Custom content")
 *         Icon(...)
 *     }
 * )
 * ```
 */
typealias Slot = @Composable () -> Unit
