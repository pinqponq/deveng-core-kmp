package core.presentation.component.reviewstack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.theme.LocalComponentTheme
import core.util.debouncedCombinedClickable
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import core.presentation.theme.ReviewStackTheme
import core.presentation.component.mediaviewer.zoom.ZoomableConfig
import core.presentation.component.mediaviewer.zoom.internal.ZoomableBox
import core.presentation.component.mediaviewer.zoom.rememberZoomableState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class ReviewStackUndoSession<T>(
    val id: Long,
    val item: T,
    val decision: ReviewDecision,
    /** Wall time when this undo banner was created; used so auto-dismiss survives slot reorder. */
    val openedAtEpochMillis: Long,
)

/** Quick fade when the user taps undo on the banner. */
private const val UndoBannerFadeOutMs = 320

/** Slower, more visible fade for timer dismiss, oldest-row eviction at capacity, and overflow ghost. */
private const val UndoBannerStackSlotFadeOutMs = 520

/** Slide + [animateContentSize] duration when a new undo banner appears (newest row). */
private const val UndoBannerSlideInMs = 320

/** Banners older than the two newest start this fade immediately (see [onUndoBannerSuperseded]). */
private const val UndoBannerOlderFadeOutMs = 420

private val UndoBannerStackSpacing = 8.dp

/**
 * A stacked viewer for browsing items one at a time using arrow buttons or horizontal swipe, while
 * collecting a positive / negative review decision per item. The front card supports pinch and
 * double-tap zoom when [itemZoomEnabled] is true.
 *
 * When the user marks a decision, the front card animates toward the corresponding decision button
 * (slight rotation + scale-down + translation) while a semi-transparent green / red overlay fades
 * in over the card. The decision callback fires after the animation completes.
 *
 * @param items The items to review.
 * @param key Stable key for an item; used to persist its decision in [state].
 * @param modifier Modifier for the outer container.
 * @param state Component state; use [rememberReviewStackState].
 * @param previousIcon Drawable for the previous-arrow button.
 * @param nextIcon Drawable for the next-arrow button.
 * @param negativeIcon Drawable for the negative (reject) button.
 * @param positiveIcon Drawable for the positive (accept) button.
 * @param previousIconDescription Accessibility label for the previous button.
 * @param nextIconDescription Accessibility label for the next button.
 * @param negativeIconDescription Accessibility label for the negative button.
 * @param positiveIconDescription Accessibility label for the positive button.
 * @param autoAdvanceOnDecision When true (default), marking a decision moves to the next item.
 * @param expandCardArea When true, the card area fills the remaining vertical space inside [modifier]
 *        (the parent must have a bounded height, e.g. [Modifier.fillMaxSize]). When false (default),
 *        the card uses [ReviewStackTheme.cardAspectRatio] to size itself.
 * @param showDecisionCounts When true (default), each decision button shows the running count of
 *        positive / negative marks next to its icon. When false, the buttons render as icon-only
 *        (same circular shape as arrow buttons).
 * @param topBarPadding Optional override for the top bar padding inside the card area. When null,
 *        uses [ReviewStackTheme.topBarPadding].
 * @param indexIndicatorAtEnd When true, the "n / total" label is aligned to the end of the top bar
 *        (after [topEndContent] if present). Default keeps the label at the start.
 * @param zoomableConfig Pinch / double-tap zoom behavior for the front card ([itemZoomEnabled]).
 * @param itemZoomEnabled When true, the front card wraps [itemContent] with zoom gestures.
 * @param onDecision Called when the user marks the front item, AFTER the exit animation completes.
 * @param undoMessage Optional message on the in-stack undo banner after a negative decision.
 *        Pass non-null (or set [undoLabel]) to enable undo banner support.
 * @param undoLabel Action label on the undo banner (e.g. "Undo"). Shown when undo is enabled.
 * @param onUndoDecision Called when the user taps undo on the banner before it auto-dismisses.
 * @param onUndoBannerSuperseded When more than two undo banners are visible, the third-oldest and older
 *        fade out immediately; this is invoked for each such banner so the host can commit the decision
 *        (e.g. cancel a delayed delete). Optional; omit if the stack is display-only.
 * @param topEndContent Optional slot rendered on the end side of the top bar (e.g. an overflow menu).
 *        Ignored when [topBar] is non-null.
 * @param topBar When non-null, replaces the default index [Row] entirely. Receives the 0-based current
 *        pager page and total item count (use for a custom bar, e.g. back + “n / total” in one panel).
 * @param onFrontCardZoomedChanged When [itemZoomEnabled], called with whether the **settled** page’s
 *        front card is zoomed in (scale above minimum). Resets to `false` when the settled page changes.
 * @param restartPagerToFirstKey Increment (e.g. after an undo that prepends the item) to snap the pager
 *        and [state] back to page 0 so the restored front card is visible.
 * @param decisionGate Optional synchronous check consulted right before a decision starts its exit
 *        animation. Return `false` to veto the decision outright (e.g. a free-tier quota was reached) —
 *        the card is left exactly as-is with no animation played at all, avoiding the need to reset any
 *        exit-transform state afterward. When `null` (default) every decision is allowed, matching prior
 *        behavior.
 * @param itemContent Composable used to render each item card's content.
 */
@Composable
fun <T> ReviewStack(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    restartPagerToFirstKey: Int = 0,
    state: ReviewStackState = rememberReviewStackState(),
    previousIcon: DrawableResource,
    nextIcon: DrawableResource,
    negativeIcon: DrawableResource,
    positiveIcon: DrawableResource,
    mirrorPreviousIcon: Boolean = false,
    previousIconDescription: String = "Previous",
    nextIconDescription: String = "Next",
    negativeIconDescription: String = "Reject",
    positiveIconDescription: String = "Accept",
    autoAdvanceOnDecision: Boolean = true,
    expandCardArea: Boolean = false,
    showDecisionCounts: Boolean = true,
    topBarPadding: PaddingValues? = null,
    indexIndicatorAtEnd: Boolean = false,
    zoomableConfig: ZoomableConfig = ZoomableConfig(),
    itemZoomEnabled: Boolean = true,
    onDecision: ((item: T, decision: ReviewDecision) -> Unit)? = null,
    undoMessage: String? = null,
    undoLabel: String? = null,
    onUndoDecision: ((item: T, decision: ReviewDecision) -> Unit)? = null,
    onUndoBannerSuperseded: ((item: T) -> Unit)? = null,
    topEndContent: (@Composable () -> Unit)? = null,
    topBar: (@Composable (currentPage: Int, totalCount: Int) -> Unit)? = null,
    onFrontCardZoomedChanged: ((Boolean) -> Unit)? = null,
    decisionGate: ((item: T, decision: ReviewDecision) -> Boolean)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    val theme = LocalComponentTheme.current.reviewStack
    val onZoomedChangedRef = rememberUpdatedState(onFrontCardZoomedChanged)
    state.clampIndex(items.size)
    val itemCount = items.size
    val hasItems = itemCount > 0
    val pagerState = rememberPagerState(
        initialPage = state.currentIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0)),
        pageCount = { itemCount },
    )
    val cardShape = RoundedCornerShape(theme.cardCornerRadius)

    LaunchedEffect(restartPagerToFirstKey) {
        if (restartPagerToFirstKey == 0) return@LaunchedEffect
        repeat(6) { attempt ->
            if (items.isNotEmpty()) {
                state.currentIndex = 0
                pagerState.scrollToPage(0)
                return@LaunchedEffect
            }
            if (attempt < 5) delay(16)
        }
    }

    LaunchedEffect(pagerState, itemCount) {
        if (itemCount <= 0) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (state.currentIndex != page) {
                    state.currentIndex = page
                }
            }
    }

    LaunchedEffect(state.currentIndex, itemCount) {
        if (itemCount <= 0) return@LaunchedEffect
        val target = state.currentIndex.coerceIn(0, itemCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.settledPage, itemZoomEnabled) {
        if (itemZoomEnabled) {
            onZoomedChangedRef.value?.invoke(false)
        }
    }

    // Position tracking (in root coordinates) for animating the front card toward a decision button.
    var cardCenter by remember { mutableStateOf<Offset?>(null) }
    var negButtonCenter by remember { mutableStateOf<Offset?>(null) }
    var posButtonCenter by remember { mutableStateOf<Offset?>(null) }

    // Animation state. `pendingDecision` is non-null while the exit animation runs.
    var pendingDecision by remember { mutableStateOf<ReviewDecision?>(null) }
    var pendingItem by remember { mutableStateOf<T?>(null) }
    /** Keeps exit transform on the card until the host removes it from [items] (avoids a rest-pose flash). */
    var exitingItemKey by remember { mutableStateOf<Any?>(null) }

    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val translationXAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(0f) }
    val overlayAlphaAnim = remember { Animatable(0f) }
    val composableScope = rememberCoroutineScope()

    val onDecisionRef = rememberUpdatedState(onDecision)
    val onUndoDecisionRef = rememberUpdatedState(onUndoDecision)
    val onUndoBannerSupersededRef = rememberUpdatedState(onUndoBannerSuperseded)
    val decisionGateRef = rememberUpdatedState(decisionGate)
    var undoBannerStack by remember { mutableStateOf<List<ReviewStackUndoSession<T>>>(emptyList()) }
    var undoSessionId by remember { mutableStateOf(0L) }

    LaunchedEffect(pendingDecision, pendingItem) {
        val decision = pendingDecision
        val item = pendingItem
        if (decision == null || item == null) return@LaunchedEffect
        val cardC = cardCenter
        val targetC = when (decision) {
            ReviewDecision.NEGATIVE -> negButtonCenter
            ReviewDecision.POSITIVE -> posButtonCenter
        }
        val translateX = if (cardC != null && targetC != null) targetC.x - cardC.x else 0f
        val translateY = if (cardC != null && targetC != null) targetC.y - cardC.y else 0f
        val rotationTarget = when (decision) {
            ReviewDecision.NEGATIVE -> -theme.exitRotationDegrees
            ReviewDecision.POSITIVE -> theme.exitRotationDegrees
        }

        // Phase 1: tilt, shrink slightly, and start overlay fade — card stays in place so user sees the rotation.
        val phase1Spec = tween<Float>(durationMillis = theme.decisionAnimationPhase1Ms)
        val phase1Scale = 1f - (1f - theme.exitScale) * 0.55f
        coroutineScope {
            launch { rotationAnim.animateTo(rotationTarget, phase1Spec) }
            launch { scaleAnim.animateTo(phase1Scale, phase1Spec) }
            launch { overlayAlphaAnim.animateTo(theme.overlayAlpha, phase1Spec) }
        }

        // Phase 2: fly toward the decision button and shrink.
        val phase2Spec = tween<Float>(durationMillis = theme.decisionAnimationPhase2Ms)
        coroutineScope {
            launch { translationXAnim.animateTo(translateX, phase2Spec) }
            launch { translationYAnim.animateTo(translateY, phase2Spec) }
            launch { scaleAnim.animateTo(theme.exitScale, phase2Spec) }
        }

        // Animation done — commit decision, advance, then snap back so the next card renders cleanly.
        val committedItem = item
        val committedDecision = decision
        println(
            "ReviewStackRevert where=decisionCommitted itemKey=${key(committedItem)} decision=$committedDecision " +
                "pagerCurrent=${pagerState.currentPage} pagerSettled=${pagerState.settledPage} " +
                "stateIndex=${state.currentIndex} itemCount=$itemCount",
        )
        state.setDecision(key(committedItem), committedDecision)
        val committedKey = key(committedItem)
        exitingItemKey = committedKey
        overlayAlphaAnim.snapTo(0f)
        onDecisionRef.value?.invoke(committedItem, committedDecision)
        if (autoAdvanceOnDecision) state.goNext(itemCount)
        // Do not snap translation/scale here — the item may still be in [items] for a frame.
        pendingItem = null
        pendingDecision = null
        // In-stack undo banner(s) for negative decisions (when undo UI is configured); unlimited stack,
        // with the two newest kept on the normal timer and older rows fading via [UndoBannerOlderFadeOutMs].
        if ((undoMessage != null || undoLabel != null) && committedDecision == ReviewDecision.NEGATIVE) {
            undoSessionId += 1L
            val openedAt = Clock.System.now().toEpochMilliseconds()
            val newSession = ReviewStackUndoSession(
                id = undoSessionId,
                item = committedItem,
                decision = committedDecision,
                openedAtEpochMillis = openedAt,
            )
            println(
                "ReviewStackRevert where=undoBannerPushed sessionId=${newSession.id} itemKey=${key(committedItem)}",
            )
            undoBannerStack = undoBannerStack + (newSession as ReviewStackUndoSession<T>)
        }
    }

    val itemKeys = items.map { key(it) }
    LaunchedEffect(itemKeys, exitingItemKey) {
        val exitKey = exitingItemKey ?: return@LaunchedEffect
        if (itemKeys.none { it == exitKey }) {
            translationXAnim.snapTo(0f)
            translationYAnim.snapTo(0f)
            scaleAnim.snapTo(1f)
            rotationAnim.snapTo(0f)
            overlayAlphaAnim.snapTo(0f)
            exitingItemKey = null
        }
    }

    var undoEntryAnimating by remember { mutableStateOf(false) }
    val isAnimating = pendingDecision != null || undoEntryAnimating
    val pagerPage = pagerState.currentPage
    val pagerIdle = pagerState.currentPageOffsetFraction == 0f
    val canGoPrevious = hasItems && pagerPage > 0 && !isAnimating
    val canGoNext = hasItems && pagerPage < itemCount - 1 && !isAnimating
    val canDecide = hasItems && !isAnimating && pagerIdle

    val bannerUndoPreFade: (suspend (ReviewStackUndoSession<T>) -> Unit)? =
        if (onUndoDecision != null && (undoMessage != null || undoLabel != null)) {
            { session ->
                executeBannerUndoPreFade(
                    session = session,
                    state = state,
                    itemKey = key,
                    onUndoDecision = onUndoDecision,
                    theme = theme,
                    cardCenter = cardCenter,
                    negButtonCenter = negButtonCenter,
                    translationXAnim = translationXAnim,
                    translationYAnim = translationYAnim,
                    scaleAnim = scaleAnim,
                    rotationAnim = rotationAnim,
                    overlayAlphaAnim = overlayAlphaAnim,
                    setUndoEntryAnimating = { undoEntryAnimating = it },
                )
            }
        } else {
            null
        }

    Box(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = (
                if (expandCardArea) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(theme.cardAspectRatio)
                }
                ).onGloballyPositioned { coords ->
                val bounds = coords.boundsInRoot()
                cardCenter = Offset(
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f,
                )
            },
        ) {
            if (itemCount > 0) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true,
                    key = { it },
                ) { page ->
                    // One full-bleed card per page. Multi-layer stack peek (theme.visibleStackDepth)
                    // is intentionally not used here: it only appeared on earlier indices (several items
                    // still "ahead") and made the front photo look inset vs the last item where depth was 1.
                    val item = items[page]
                    val itemKey = key(item)
                    val animatingItem = pendingItem
                    val isExitAnimatedPage = when {
                        animatingItem != null && itemKey == key(animatingItem) -> true
                        exitingItemKey != null && itemKey == exitingItemKey -> true
                        else -> false
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isExitAnimatedPage) {
                                    Modifier.graphicsLayer {
                                        val s = scaleAnim.value
                                        scaleX = s
                                        scaleY = s
                                        translationX = translationXAnim.value
                                        translationY = translationYAnim.value
                                        rotationZ = rotationAnim.value
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (theme.cardShadowElevation > 0.dp) {
                                    Modifier.shadow(theme.cardShadowElevation, cardShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clip(cardShape)
                            .background(theme.cardColor),
                    ) {
                        if (itemZoomEnabled) {
                            val zoomableState = rememberZoomableState(
                                config = zoomableConfig,
                                resetKey = item,
                            )
                            val settledPage = pagerState.settledPage
                            LaunchedEffect(page, settledPage, zoomableState) {
                                if (page != settledPage) return@LaunchedEffect
                                snapshotFlow { zoomableState.isZoomed }
                                    .distinctUntilChanged()
                                    .collect { zoomed ->
                                        onZoomedChangedRef.value?.invoke(zoomed)
                                    }
                            }
                            ZoomableBox(
                                zoomableState = zoomableState,
                                config = zoomableConfig,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    itemContent(item)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                itemContent(item)
                            }
                        }
                        // Decision overlay (green for positive, red for negative).
                        val overlayColor = when (pendingDecision) {
                            ReviewDecision.POSITIVE -> theme.positiveOverlayColor
                            ReviewDecision.NEGATIVE -> theme.negativeOverlayColor
                            null -> theme.negativeOverlayColor
                        }
                        if (isExitAnimatedPage && pendingDecision != null && overlayAlphaAnim.value > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = overlayAlphaAnim.value }
                                    .background(overlayColor),
                            )
                        }
                    }
                }
            }

            if (topBar != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(topBarPadding ?: theme.topBarPadding),
                ) {
                    topBar(pagerPage, itemCount)
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(topBarPadding ?: theme.topBarPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (indexIndicatorAtEnd) {
                        Spacer(modifier = Modifier.weight(1f))
                        if (topEndContent != null) {
                            topEndContent()
                        }
                        Text(
                            text = if (hasItems) "${pagerPage + 1} / $itemCount" else "0 / 0",
                            style = theme.indexIndicatorTextStyle,
                        )
                    } else {
                        Text(
                            text = if (hasItems) "${pagerPage + 1} / $itemCount" else "0 / 0",
                            style = theme.indexIndicatorTextStyle,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (topEndContent != null) {
                            topEndContent()
                        }
                    }
                }
            }

            // Over the media (same card area as the pager), not in the outer column above controls.
            if (undoBannerStack.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = theme.undoBannerHorizontalPadding)
                        .padding(bottom = theme.undoBannerBottomPadding)
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = UndoBannerSlideInMs,
                                easing = FastOutSlowInEasing,
                            ),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UndoBannerStackSpacing, Alignment.Bottom),
                ) {
                    undoBannerStack.forEachIndexed { index, session ->
                        val ageFromNewest = undoBannerStack.lastIndex - index
                        val isNewest = index == undoBannerStack.lastIndex
                        key(session.id) {
                            UndoBannerRowWithSlideIn(
                                session = session,
                                itemKeyForLog = key(session.item).toString(),
                                theme = theme,
                                undoMessage = undoMessage.orEmpty(),
                                undoLabel = undoLabel.orEmpty(),
                                composableScope = composableScope,
                                animateSlideInFromBottom = isNewest,
                                ageFromNewest = ageFromNewest,
                                onRemoveFromStack = { id ->
                                    undoBannerStack = undoBannerStack.filter { it.id != id }
                                },
                                onUndoDecisionRef = onUndoDecisionRef,
                                suspendOnUndoFromBanner = bannerUndoPreFade,
                                onUndoBannerSuperseded = { item ->
                                    onUndoBannerSupersededRef.value?.invoke(item)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(theme.controlsTopPadding))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArrowButton(
                icon = previousIcon,
                contentDescription = previousIconDescription,
                enabled = canGoPrevious,
                size = theme.arrowButtonSize,
                background = theme.arrowButtonBackgroundColor,
                borderColor = theme.arrowButtonBorderColor,
                iconTint = theme.arrowButtonIconTint,
                shadowElevation = theme.arrowButtonShadowElevation,
                mirrorIcon = mirrorPreviousIcon,
                onClick = { state.goPrevious() },
            )

            DecisionButton(
                icon = negativeIcon,
                contentDescription = negativeIconDescription,
                count = state.negativeCount,
                showCount = showDecisionCounts,
                enabled = canDecide,
                size = theme.decisionButtonSize,
                shape = theme.decisionButtonShape,
                background = theme.decisionButtonBackgroundColor,
                borderColor = theme.negativeBorderColor,
                borderWidth = theme.decisionBorderWidth,
                iconTint = theme.negativeIconTint,
                countColor = theme.negativeCountColor,
                countTextStyle = theme.decisionCountTextStyle,
                contentSpacing = theme.decisionContentSpacing,
                onPositioned = { center -> negButtonCenter = center },
                onClick = {
                    if (canDecide) {
                        val candidateItem = items[pagerPage]
                        val isAllowed = decisionGateRef.value?.invoke(candidateItem, ReviewDecision.NEGATIVE) ?: true
                        if (isAllowed) {
                            pendingItem = candidateItem
                            pendingDecision = ReviewDecision.NEGATIVE
                        }
                    }
                },
            )

            DecisionButton(
                icon = positiveIcon,
                contentDescription = positiveIconDescription,
                count = state.positiveCount,
                showCount = showDecisionCounts,
                enabled = canDecide,
                size = theme.decisionButtonSize,
                shape = theme.decisionButtonShape,
                background = theme.decisionButtonBackgroundColor,
                borderColor = theme.positiveBorderColor,
                borderWidth = theme.decisionBorderWidth,
                iconTint = theme.positiveIconTint,
                countColor = theme.positiveCountColor,
                countTextStyle = theme.decisionCountTextStyle,
                contentSpacing = theme.decisionContentSpacing,
                leadingCount = true,
                onPositioned = { center -> posButtonCenter = center },
                onClick = {
                    if (canDecide) {
                        val candidateItem = items[pagerPage]
                        val isAllowed = decisionGateRef.value?.invoke(candidateItem, ReviewDecision.POSITIVE) ?: true
                        if (isAllowed) {
                            pendingItem = candidateItem
                            pendingDecision = ReviewDecision.POSITIVE
                        }
                    }
                },
            )

            ArrowButton(
                icon = nextIcon,
                contentDescription = nextIconDescription,
                enabled = canGoNext,
                size = theme.arrowButtonSize,
                background = theme.arrowButtonBackgroundColor,
                borderColor = theme.arrowButtonBorderColor,
                iconTint = theme.arrowButtonIconTint,
                shadowElevation = theme.arrowButtonShadowElevation,
                onClick = { state.goNext(itemCount) },
            )
        }

        Spacer(modifier = Modifier.height(theme.controlsBottomPadding))
    } // Column
    } // Box
}

private suspend fun animateNegativeUndoReveal(
    theme: ReviewStackTheme,
    cardCenter: Offset?,
    negButtonCenter: Offset?,
    translationXAnim: Animatable<Float, AnimationVector1D>,
    translationYAnim: Animatable<Float, AnimationVector1D>,
    scaleAnim: Animatable<Float, AnimationVector1D>,
    rotationAnim: Animatable<Float, AnimationVector1D>,
    overlayAlphaAnim: Animatable<Float, AnimationVector1D>,
) {
    val cardC = cardCenter
    val targetC = negButtonCenter
    val translateX = if (cardC != null && targetC != null) targetC.x - cardC.x else 0f
    val translateY = if (cardC != null && targetC != null) targetC.y - cardC.y else 0f
    val rotationTarget = -theme.exitRotationDegrees
    val phase1Scale = 1f - (1f - theme.exitScale) * 0.55f
    translationXAnim.snapTo(translateX)
    translationYAnim.snapTo(translateY)
    scaleAnim.snapTo(theme.exitScale)
    rotationAnim.snapTo(rotationTarget)
    overlayAlphaAnim.snapTo(theme.overlayAlpha)
    val phase2Spec = tween<Float>(durationMillis = theme.decisionAnimationPhase2Ms)
    coroutineScope {
        launch { translationXAnim.animateTo(0f, phase2Spec) }
        launch { translationYAnim.animateTo(0f, phase2Spec) }
        launch { scaleAnim.animateTo(phase1Scale, phase2Spec) }
    }
    val phase1Spec = tween<Float>(durationMillis = theme.decisionAnimationPhase1Ms)
    coroutineScope {
        launch { rotationAnim.animateTo(0f, phase1Spec) }
        launch { scaleAnim.animateTo(1f, phase1Spec) }
        launch { overlayAlphaAnim.animateTo(0f, phase1Spec) }
    }
}

@Composable
private fun <T> UndoBannerRowWithSlideIn(
    session: ReviewStackUndoSession<T>,
    itemKeyForLog: String,
    theme: ReviewStackTheme,
    undoMessage: String,
    undoLabel: String,
    composableScope: CoroutineScope,
    animateSlideInFromBottom: Boolean,
    ageFromNewest: Int,
    onRemoveFromStack: (Long) -> Unit,
    onUndoDecisionRef: State<((T, ReviewDecision) -> Unit)?>,
    suspendOnUndoFromBanner: (suspend (ReviewStackUndoSession<T>) -> Unit)?,
    onUndoBannerSuperseded: (T) -> Unit,
) {
    val density = LocalDensity.current
    val slidePx = with(density) { 56.dp.toPx() }
    val offsetY = remember(session.id) { Animatable(0f) }
    val enterAlpha = remember(session.id) { Animatable(1f) }
    LaunchedEffect(session.id, animateSlideInFromBottom) {
        if (animateSlideInFromBottom) {
            offsetY.snapTo(slidePx)
            enterAlpha.snapTo(0.55f)
            val spec = tween<Float>(UndoBannerSlideInMs, easing = FastOutSlowInEasing)
            coroutineScope {
                launch { offsetY.animateTo(0f, spec) }
                launch { enterAlpha.animateTo(1f, spec) }
            }
        } else {
            offsetY.snapTo(0f)
            enterAlpha.snapTo(1f)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetY.value
                alpha = enterAlpha.value
            },
    ) {
        UndoBannerRow(
            session = session,
            itemKeyForLog = itemKeyForLog,
            theme = theme,
            undoMessage = undoMessage,
            undoLabel = undoLabel,
            composableScope = composableScope,
            ageFromNewest = ageFromNewest,
            onRemoveFromStack = onRemoveFromStack,
            onUndoDecisionRef = onUndoDecisionRef,
            suspendOnUndoFromBanner = suspendOnUndoFromBanner,
            onUndoBannerSuperseded = onUndoBannerSuperseded,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private suspend fun <T> executeBannerUndoPreFade(
    session: ReviewStackUndoSession<T>,
    state: ReviewStackState,
    itemKey: (T) -> Any,
    onUndoDecision: ((T, ReviewDecision) -> Unit)?,
    theme: ReviewStackTheme,
    cardCenter: Offset?,
    negButtonCenter: Offset?,
    translationXAnim: Animatable<Float, AnimationVector1D>,
    translationYAnim: Animatable<Float, AnimationVector1D>,
    scaleAnim: Animatable<Float, AnimationVector1D>,
    rotationAnim: Animatable<Float, AnimationVector1D>,
    overlayAlphaAnim: Animatable<Float, AnimationVector1D>,
    setUndoEntryAnimating: (Boolean) -> Unit,
) {
    setUndoEntryAnimating(true)
    try {
        println(
            "ReviewStackRevert where=undoExecuteStart sessionId=${session.id} itemKey=${itemKey(session.item)} decision=${session.decision}",
        )
        onUndoDecision?.invoke(session.item, session.decision)
        state.clearDecision(itemKey(session.item))
        if (session.decision == ReviewDecision.NEGATIVE) {
            repeat(2) { withFrameNanos { } }
            animateNegativeUndoReveal(
                theme = theme,
                cardCenter = cardCenter,
                negButtonCenter = negButtonCenter,
                translationXAnim = translationXAnim,
                translationYAnim = translationYAnim,
                scaleAnim = scaleAnim,
                rotationAnim = rotationAnim,
                overlayAlphaAnim = overlayAlphaAnim,
            )
        }
    } finally {
        setUndoEntryAnimating(false)
    }
}

@Composable
private fun <T> UndoBannerRow(
    session: ReviewStackUndoSession<T>,
    itemKeyForLog: String,
    theme: ReviewStackTheme,
    undoMessage: String,
    undoLabel: String,
    composableScope: CoroutineScope,
    ageFromNewest: Int,
    onRemoveFromStack: (Long) -> Unit,
    onUndoDecisionRef: State<((T, ReviewDecision) -> Unit)?>,
    suspendOnUndoFromBanner: (suspend (ReviewStackUndoSession<T>) -> Unit)?,
    onUndoBannerSuperseded: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowFadeAnim = remember(session.id) { Animatable(1f) }
    LaunchedEffect(session.id, ageFromNewest, session.openedAtEpochMillis, theme.undoBannerVisibleMs) {
        if (ageFromNewest >= 2) {
            rowFadeAnim.snapTo(1f)
            rowFadeAnim.animateTo(
                0f,
                tween(
                    durationMillis = UndoBannerOlderFadeOutMs,
                    easing = FastOutSlowInEasing,
                ),
            )
            onUndoBannerSuperseded(session.item)
            onRemoveFromStack(session.id)
            rowFadeAnim.snapTo(1f)
            return@LaunchedEffect
        }
        val visibleMs = theme.undoBannerVisibleMs.coerceAtLeast(250).toLong()
        val elapsed = Clock.System.now().toEpochMilliseconds() - session.openedAtEpochMillis
        delay((visibleMs - elapsed).coerceAtLeast(0L))
        rowFadeAnim.animateTo(
            0f,
            tween(
                durationMillis = UndoBannerStackSlotFadeOutMs,
                easing = FastOutSlowInEasing,
            ),
        )
        onRemoveFromStack(session.id)
        rowFadeAnim.snapTo(1f)
    }
    Box(modifier = modifier.graphicsLayer { alpha = rowFadeAnim.value }) {
        ReviewStackUndoBanner(
            modifier = Modifier.fillMaxWidth(),
            message = undoMessage,
            label = undoLabel,
            onUndoClick = {
                composableScope.launch {
                    println(
                        "ReviewStackRevert where=undoBannerClick sessionId=${session.id} itemKey=$itemKeyForLog",
                    )
                    val preFade = suspendOnUndoFromBanner
                    if (preFade != null) {
                        preFade(session)
                    } else {
                        onUndoDecisionRef.value?.invoke(session.item, session.decision)
                    }
                    rowFadeAnim.animateTo(0f, tween(durationMillis = UndoBannerFadeOutMs))
                    onRemoveFromStack(session.id)
                    rowFadeAnim.snapTo(1f)
                }
            },
            theme = theme,
        )
    }
}

@Composable
private fun ReviewStackUndoBanner(
    modifier: Modifier,
    message: String,
    label: String,
    onUndoClick: () -> Unit,
    theme: ReviewStackTheme,
    interactive: Boolean = true,
) {
    val shape = RoundedCornerShape(theme.undoBannerCornerRadius)
    val maxW = theme.undoBannerMaxWidth
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (maxW != null) Modifier.widthIn(max = maxW) else Modifier)
            .clip(shape)
            .background(theme.undoBannerContainerColor)
            .then(
                if (interactive) {
                    Modifier.clickable(onClick = onUndoClick, onClickLabel = label)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (message.isNotBlank()) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Center
            },
        ) {
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = theme.undoBannerMessageTextStyle.copy(color = theme.undoBannerContentColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
            }
            Text(
                text = label,
                style = theme.undoBannerActionTextStyle.copy(color = theme.undoBannerActionColor),
            )
        }
    }
}

@Composable
private fun ArrowButton(
    icon: DrawableResource,
    contentDescription: String,
    enabled: Boolean,
    size: Dp,
    background: Color,
    borderColor: Color,
    iconTint: Color,
    shadowElevation: Dp,
    mirrorIcon: Boolean = false,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, CircleShape) else Modifier,
            )
            .clip(CircleShape)
            .background(background, CircleShape)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .debouncedCombinedClickable(
                debounceMillis = 300L,
                shape = CircleShape,
            ) {
                if (enabled) onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = if (mirrorIcon) Modifier.graphicsLayer { scaleX = -1f } else Modifier,
        )
    }
}

@Composable
private fun DecisionButton(
    icon: DrawableResource,
    contentDescription: String,
    count: Int,
    showCount: Boolean,
    enabled: Boolean,
    size: Dp,
    shape: Shape,
    background: Color,
    borderColor: Color,
    borderWidth: Dp,
    iconTint: Color,
    countColor: Color,
    countTextStyle: TextStyle,
    contentSpacing: Dp,
    leadingCount: Boolean = false,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    val effectiveShape = if (showCount) shape else CircleShape
    val containerModifier = Modifier
        .graphicsLayer { this.alpha = alpha }
        .then(if (!showCount) Modifier.size(size) else Modifier)
        .clip(effectiveShape)
        .background(background, effectiveShape)
        .border(width = borderWidth, color = borderColor, shape = effectiveShape)
        .onGloballyPositioned { coords ->
            val bounds = coords.boundsInRoot()
            onPositioned(
                Offset(
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f,
                ),
            )
        }
        .debouncedCombinedClickable(shape = effectiveShape) {
            if (enabled) onClick()
        }
    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (!showCount) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = iconTint,
            )
        } else {
            Row(
                modifier = Modifier
                    .height(size)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingCount) {
                    Text(
                        text = count.toString(),
                        style = countTextStyle.copy(color = countColor),
                    )
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = contentDescription,
                        tint = iconTint,
                    )
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = contentDescription,
                        tint = iconTint,
                    )
                    Text(
                        text = count.toString(),
                        style = countTextStyle.copy(color = countColor),
                    )
                }
            }
        }
    }
}
