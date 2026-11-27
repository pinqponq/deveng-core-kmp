package core.presentation.component.reviewstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * State for [ReviewStack]. Tracks the current item index and per-item review decisions.
 *
 * Decisions are stored by stable item key (provided via [ReviewStack]'s `key` lambda) so prev/next
 * navigation re-shows the user's prior choice and re-marking the same item updates rather than duplicates.
 *
 * @param initialIndex The item index to show first.
 */
@Stable
class ReviewStackState internal constructor(
    initialIndex: Int = 0,
) {

    private val _currentIndex = mutableIntStateOf(initialIndex)
    var currentIndex: Int
        get() = _currentIndex.intValue
        internal set(value) {
            _currentIndex.intValue = value
        }

    internal val decisions = mutableStateMapOf<Any, ReviewDecision>()

    private val negativeCountState = derivedStateOf {
        decisions.count { it.value == ReviewDecision.NEGATIVE }
    }
    private val positiveCountState = derivedStateOf {
        decisions.count { it.value == ReviewDecision.POSITIVE }
    }

    /** Number of items currently marked [ReviewDecision.NEGATIVE]. */
    val negativeCount: Int by negativeCountState

    /** Number of items currently marked [ReviewDecision.POSITIVE]. */
    val positiveCount: Int by positiveCountState

    /** Returns the user's decision for [key], or null if the item has not been reviewed. */
    fun decisionFor(key: Any): ReviewDecision? = decisions[key]

    /** Read-only snapshot of all current decisions keyed by item key. */
    val allDecisions: Map<Any, ReviewDecision> get() = decisions.toMap()

    internal fun setDecision(key: Any, decision: ReviewDecision) {
        decisions[key] = decision
    }

    internal fun clearDecision(key: Any) {
        decisions.remove(key)
    }

    internal fun clampIndex(itemCount: Int) {
        if (itemCount <= 0) {
            currentIndex = 0
            return
        }
        if (currentIndex < 0) currentIndex = 0
        if (currentIndex > itemCount - 1) currentIndex = itemCount - 1
    }

    internal fun goNext(itemCount: Int) {
        if (currentIndex < itemCount - 1) currentIndex++
    }

    internal fun goPrevious() {
        if (currentIndex > 0) currentIndex--
    }

    companion object {
        internal val Saver: Saver<ReviewStackState, *> = Saver(
            save = { it.currentIndex },
            restore = { ReviewStackState(initialIndex = it) },
        )
    }
}

/**
 * Creates and remembers a [ReviewStackState].
 *
 * @param initialIndex Index of the item to show first.
 */
@Composable
fun rememberReviewStackState(
    initialIndex: Int = 0,
): ReviewStackState = rememberSaveable(saver = ReviewStackState.Saver) {
    ReviewStackState(initialIndex = initialIndex)
}
