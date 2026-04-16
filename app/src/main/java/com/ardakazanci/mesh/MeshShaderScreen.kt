package com.ardakazanci.mesh

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ardakazanci.mesh.ui.theme.MeshTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun MeshShaderScreen() {
    val view = LocalView.current
    var targetTilt by remember { mutableStateOf(Offset.Zero) }
    var smoothedTilt by remember { mutableStateOf(Offset.Zero) }
    var magneticTilt by remember { mutableStateOf(Offset.Zero) }
    var reboundTilt by remember { mutableStateOf(Offset.Zero) }
    var visualTilt by remember { mutableStateOf(Offset.Zero) }
    var shaderTilt by remember { mutableStateOf(Offset.Zero) }
    var inertia by remember { mutableStateOf(Offset.Zero) }
    var focusTarget by remember { mutableStateOf(Offset(0.76f, 0.22f)) }
    var focusVisual by remember { mutableStateOf(Offset(0.76f, 0.22f)) }
    var focusGlow by remember { mutableFloatStateOf(0.18f) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(false) }
    var flipPreview by remember { mutableFloatStateOf(0f) }
    var flipTarget by remember { mutableFloatStateOf(0f) }
    var spinTarget by remember { mutableFloatStateOf(0f) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var timeSeconds by remember { mutableFloatStateOf(0f) }
    var timeDilationPulse by remember { mutableFloatStateOf(0f) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdFeedbackStep by remember { mutableIntStateOf(0) }
    var explosionProgress by remember { mutableFloatStateOf(0f) }
    var explosionActive by remember { mutableStateOf(false) }
    var explosionLatched by remember { mutableStateOf(false) }
    var dustCollapseProgress by remember { mutableFloatStateOf(0f) }
    var dustCollapseActive by remember { mutableStateOf(false) }
    var dustCollapseDirection by remember { mutableFloatStateOf(0f) }
    var dustCollapseIntensity by remember { mutableFloatStateOf(0f) }
    var dustOriginOffset by remember { mutableStateOf(Offset.Zero) }
    var dustCardHidden by remember { mutableStateOf(false) }
    val energy by animateFloatAsState(
        targetValue = (
            smoothedTilt.magnitude() * 0.8f +
                inertia.magnitude() * 0.55f +
                timeDilationPulse * 0.18f +
                holdProgress * 0.42f
            ).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "energy"
    )
    val triggerCinematicPulse = { strength: Float ->
        timeDilationPulse = kotlin.math.max(timeDilationPulse, strength.coerceIn(0f, 1f))
    }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press"
    )
    val revealProgressRaw by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = MeshShaderDefaults.RevealDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "reveal"
    )
    val revealProgress = revealProgressRaw.coerceIn(0f, 1f)
    val flipRotation by animateFloatAsState(
        targetValue = flipTarget,
        animationSpec = tween(
            durationMillis = MeshShaderDefaults.FlipDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "flipRotation"
    )
    val spinRotation by animateFloatAsState(
        targetValue = spinTarget,
        animationSpec = tween(
            durationMillis = MeshShaderDefaults.SpinDurationMillis,
            easing = LinearOutSlowInEasing
        ),
        label = "spin"
    )
    val resetInteractionState = {
        targetTilt = Offset.Zero
        smoothedTilt = Offset.Zero
        magneticTilt = Offset.Zero
        reboundTilt = Offset.Zero
        visualTilt = Offset.Zero
        shaderTilt = Offset.Zero
        inertia = Offset.Zero
        flipPreview = 0f
        flipTarget = 0f
        spinTarget = 0f
    }

    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frame ->
                val deltaSeconds = ((frame - lastFrame) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastFrame = frame
                val timeScale = 1f - timeDilationPulse * 0.24f
                timeSeconds += deltaSeconds * timeScale.coerceIn(0.76f, 1f)
                timeDilationPulse *= exp(-deltaSeconds * 7.4f)

                val canChargeExplosion = isPressed &&
                    !isDragging &&
                    !isRevealed &&
                    !explosionLatched &&
                    !dustCollapseActive
                if (canChargeExplosion) {
                    holdProgress = (
                        holdProgress + deltaSeconds / MeshShaderDefaults.HoldToExplodeSeconds
                        ).coerceIn(0f, 1f)
                    val milestone = when {
                        holdProgress >= 0.75f -> 3
                        holdProgress >= 0.5f -> 2
                        holdProgress >= 0.25f -> 1
                        else -> 0
                    }
                    if (milestone > holdFeedbackStep) {
                        holdFeedbackStep = milestone
                        triggerCinematicPulse(0.2f + milestone * 0.1f)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    if (holdProgress >= 1f && !explosionActive) {
                        explosionActive = true
                        explosionLatched = true
                        explosionProgress = 0f
                        holdProgress = 0f
                        holdFeedbackStep = 0
                        resetInteractionState()
                        triggerCinematicPulse(1f)
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                } else {
                    holdProgress += (0f - holdProgress) * (1f - exp(-deltaSeconds * 6.8f))
                    if (holdProgress < 0.01f) {
                        holdProgress = 0f
                        holdFeedbackStep = 0
                    }
                    if (!isPressed && !explosionActive) {
                        explosionLatched = false
                    }
                }

                if (explosionActive) {
                    explosionProgress = (
                        explosionProgress + deltaSeconds / MeshShaderDefaults.ExplosionDurationSeconds
                        ).coerceIn(0f, 1f)
                    if (explosionProgress >= 1f) {
                        explosionActive = false
                        explosionProgress = 0f
                        if (!isPressed) {
                            explosionLatched = false
                        }
                    }
                }

                if (dustCollapseActive) {
                    dustCollapseProgress = (
                        dustCollapseProgress + deltaSeconds / MeshShaderDefaults.DustCollapseDurationSeconds
                        ).coerceIn(0f, 1f)
                    if (dustCollapseProgress >= MeshShaderDefaults.DustHideThreshold) {
                        dustCardHidden = true
                    }
                    if (dustCollapseProgress >= 1f) {
                        dustCollapseActive = false
                        dustCollapseProgress = 0f
                        dustCollapseDirection = 0f
                        dustCollapseIntensity = 0f
                    }
                }

                if (!isDragging) {
                    targetTilt *= exp(-deltaSeconds * 4.6f)
                }
                magneticTilt *= exp(-deltaSeconds * 1.9f)
                reboundTilt *= exp(-deltaSeconds * 8.5f)

                val targetFollow = 1f - exp(-deltaSeconds * if (isDragging) 7.4f else 5.2f)
                smoothedTilt += (targetTilt - smoothedTilt) * targetFollow

                inertia *= exp(-deltaSeconds * if (isDragging) 16f else 7.2f)

                val follow = 1f - exp(-deltaSeconds * 10.5f)
                val motionTarget = (smoothedTilt + magneticTilt + reboundTilt + inertia * 0.24f).limit(1f)
                visualTilt += (motionTarget - visualTilt) * follow

                val shaderFollow = 1f - exp(-deltaSeconds * if (isDragging) 3.4f else 2.6f)
                val shaderTarget = (
                    smoothedTilt * 0.42f +
                        magneticTilt * 0.18f +
                        reboundTilt * 0.12f +
                        inertia * 0.08f
                    ).limit(0.38f)
                shaderTilt += (shaderTarget - shaderTilt) * shaderFollow

                val idleFocus = Offset(
                    x = (0.76f - visualTilt.x * 0.12f).coerceIn(0.18f, 0.84f),
                    y = (0.22f + visualTilt.y * 0.09f).coerceIn(0.14f, 0.58f)
                )
                if (!isDragging) {
                    focusTarget = idleFocus
                }

                val focusFollow = 1f - exp(-deltaSeconds * if (isDragging) 14f else 6.4f)
                focusVisual += (focusTarget - focusVisual) * focusFollow
                val glowTarget = if (isDragging) 0.3f else 0.18f + energy * 0.08f + holdProgress * 0.12f
                focusGlow += (glowTarget - focusGlow) * (1f - exp(-deltaSeconds * 7.2f))
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val compactCardWidth = minOf(maxWidth * 0.82f, MeshShaderDefaults.CompactCardMaxWidth)
        val compactCardHeight = minOf(maxHeight * 0.62f, compactCardWidth * 1.34f)
        val transition = rememberShaderElementTransition(
            compactCardWidth = compactCardWidth,
            compactCardHeight = compactCardHeight,
            expandedWidth = maxWidth,
            expandedHeight = maxHeight,
            progress = revealProgress
        )
        val cardShape = RoundedCornerShape(transition.cornerRadius)
        val travelX = with(density) { 30.dp.toPx() }
        val travelY = with(density) { 20.dp.toPx() }
        val cameraDistance = with(density) { 36.dp.toPx() } * 10f
        val pulseLift = with(density) { 10.dp.toPx() }
        val pressSink = with(density) { 12.dp.toPx() }
        val shadowTravelX = with(density) { 24.dp.toPx() }
        val shadowTravelY = with(density) { 18.dp.toPx() }
        val cinematicGlow = (timeDilationPulse + holdProgress * 0.52f + pulseEnvelope(explosionProgress) * 0.8f)
            .coerceIn(0f, 1f)
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val backdropCenter = with(density) {
            Offset(
                x = maxWidth.toPx() * (0.5f + visualTilt.x * 0.028f),
                y = maxHeight.toPx() * (0.24f + visualTilt.y * 0.02f)
            )
        }
        val backdropRadius = with(density) { maxHeight.toPx() * 0.95f }
        val explosionFade = stagedProgress(explosionProgress, 0.02f, 0.18f)
        val displayPressProgress = pressProgress * (1f - explosionFade)
        val bobOffset = sin(timeSeconds * 1.35f) * pulseLift * (1f - displayPressProgress * 0.88f) * (1f - transition.progress)
        val interactionTransformFade = if (isRevealed) 0f else 1f - FastOutSlowInEasing.transform(transition.progress)
        val flipPresence = abs(sin(Math.toRadians(flipRotation.toDouble())).toFloat())
        val baseCardTranslationX = (visualTilt.x * travelX + inertia.x * travelX * 0.3f) * interactionTransformFade
        val baseCardTranslationY = (
            visualTilt.y * travelY +
                inertia.y * travelY * 0.24f +
                bobOffset +
                displayPressProgress * pressSink
            ) * interactionTransformFade
        val dustBurst = stagedProgress(dustCollapseProgress, 0f, 0.12f)
        val dustDrift = stagedProgress(dustCollapseProgress, 0.04f, 0.58f)
        val dustLateralThrow = dustCollapseDirection * maxWidthPx * (0.06f + dustCollapseIntensity * 0.08f) * dustDrift
        val dustFloat = -maxHeightPx * (0.025f + dustCollapseIntensity * 0.05f) * dustDrift
        val dustKick = -with(density) { 14.dp.toPx() } * pulseEnvelope(stagedProgress(dustCollapseProgress, 0f, 0.22f))
        val cardTranslationX = if (dustCollapseActive || dustCollapseProgress > 0f) {
            dustOriginOffset.x + dustLateralThrow
        } else {
            baseCardTranslationX
        }
        val cardTranslationY = if (dustCollapseActive || dustCollapseProgress > 0f) {
            dustOriginOffset.y + dustFloat + dustKick
        } else {
            baseCardTranslationY
        }
        val cardVisibility = (1f - explosionFade).coerceIn(0f, 1f)
        val dustFadeOut = 1f - stagedProgress(dustCollapseProgress, 0.1f, 0.24f)
        val effectiveCardVisibility = when {
            dustCardHidden -> 0f
            dustCollapseActive || dustCollapseProgress > 0f ->
                cardVisibility * dustFadeOut
            else -> cardVisibility
        }
        val interactionsEnabled = !explosionActive &&
            explosionProgress <= 0.001f &&
            !dustCollapseActive &&
            dustCollapseProgress <= 0.001f &&
            !dustCardHidden
        val cardFlipAngle = (flipRotation + flipPreview * 28f) * interactionTransformFade
        val cardSpinAngle = spinRotation * interactionTransformFade
        val dustRoll = dustCollapseDirection * (5f + dustCollapseIntensity * 8f) * dustDrift
        val dustPitch = 5f * dustBurst

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MeshBackdropLayer(
                modifier = Modifier.fillMaxSize(),
                backdropCenter = backdropCenter,
                backdropRadius = backdropRadius,
                tilt = visualTilt,
                energy = energy,
                cinematicGlow = cinematicGlow
            )

            CardShadowLayer(
                modifier = Modifier.size(transition.cardWidth * 1.45f, transition.cardHeight * 0.55f),
                transition = transition,
                tilt = visualTilt,
                energy = energy,
                pressProgress = pressProgress,
                cinematicGlow = cinematicGlow,
                shadowTravelX = shadowTravelX,
                shadowTravelY = shadowTravelY
            )

            MeteorOverflowLayer(
                modifier = Modifier.fillMaxSize(),
                cardWidth = transition.cardWidth,
                cardHeight = transition.cardHeight,
                cardOffset = Offset(cardTranslationX, cardTranslationY),
                tilt = visualTilt,
                energy = energy,
                pressProgress = displayPressProgress,
                revealProgress = transition.progress,
                flipProgress = flipPresence,
                timeSeconds = timeSeconds,
                explosionProgress = explosionProgress,
                dustCollapseProgress = dustCollapseProgress
            )

            InteractiveShaderCard(
                modifier = Modifier
                    .size(transition.cardWidth, transition.cardHeight)
                    .graphicsLayer {
                        shadowElevation = (
                            with(density) { 32.dp.toPx() } - displayPressProgress * with(density) { 14.dp.toPx() }
                            ) * (1f - transition.progress * 0.94f)
                        shape = cardShape
                        clip = true
                        rotationX = ((-visualTilt.y * 14f - inertia.y * 4f) * (1f - displayPressProgress * 0.18f) - dustPitch) * interactionTransformFade
                        rotationY = (visualTilt.x * 18f + inertia.x * 4.5f) * (1f - displayPressProgress * 0.18f) * interactionTransformFade + cardFlipAngle
                        rotationZ = (visualTilt.x * -2.4f * (1f - displayPressProgress * 0.15f) + dustRoll) * interactionTransformFade + cardSpinAngle
                        translationX = cardTranslationX
                        translationY = cardTranslationY
                        scaleX = 1f + (energy * 0.015f - displayPressProgress * 0.024f + pulseEnvelope(explosionProgress) * 0.018f) * interactionTransformFade
                        scaleY = 1f + (energy * 0.015f - displayPressProgress * 0.024f + pulseEnvelope(explosionProgress) * 0.018f - dustDrift * 0.04f) * interactionTransformFade
                        this.cameraDistance = cameraDistance
                        alpha = effectiveCardVisibility
                    },
                shape = cardShape,
                cornerRadius = transition.cornerRadius,
                tilt = shaderTilt,
                inertia = inertia,
                focus = focusVisual,
                focusGlow = focusGlow,
                pressProgress = displayPressProgress,
                flipRotation = flipRotation,
                transition = transition,
                targetTilt = targetTilt,
                onTiltChange = { targetTilt = it },
                onFocusChange = { focusTarget = it },
                onGestureStart = { isDragging = true },
                onGestureEnd = { release ->
                    isDragging = false
                    inertia = release.impulse.limit(0.12f)
                    magneticTilt = premiumRestingTilt(release.tilt)
                    reboundTilt = (release.impulse * -1.15f).limit(0.055f)
                },
                onPressChange = { pressed ->
                    isPressed = pressed
                    if (pressed) {
                        triggerCinematicPulse(0.16f)
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                },
                onFlipPreviewChange = { flipPreview = it },
                onFlipTrigger = { direction ->
                    flipTarget += 360f * direction
                    triggerCinematicPulse(0.42f)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                },
                onSpinTrigger = {
                    spinTarget += 1080f
                    triggerCinematicPulse(0.76f)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                },
                onDustCollapseTrigger = { direction, intensity ->
                    dustCardHidden = false
                    dustOriginOffset = Offset(baseCardTranslationX, baseCardTranslationY)
                    dustCollapseDirection = direction
                    dustCollapseIntensity = intensity
                    dustCollapseProgress = 0f
                    dustCollapseActive = true
                    tapCount = 0
                    holdProgress = 0f
                    holdFeedbackStep = 0
                    explosionLatched = false
                    resetInteractionState()
                    triggerCinematicPulse(0.9f)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                },
                onCardTap = {
                    val now = SystemClock.uptimeMillis()
                    tapCount = if (now - lastTapAt <= MeshShaderDefaults.TripleTapWindowMillis) {
                        tapCount + 1
                    } else {
                        1
                    }
                    lastTapAt = now
                    if (tapCount >= 3) {
                        val willReveal = !isRevealed
                        if (!isRevealed) {
                            resetInteractionState()
                        }
                        triggerCinematicPulse(if (willReveal) 1f else 0.5f)
                        view.performHapticFeedback(
                            if (willReveal) HapticFeedbackConstants.LONG_PRESS
                            else HapticFeedbackConstants.CLOCK_TICK
                        )
                        isRevealed = willReveal
                        tapCount = 0
                    }
                },
                interactionsEnabled = interactionsEnabled,
                holdProgress = holdProgress * (1f - transition.progress),
                timeSeconds = timeSeconds,
                energy = energy
            )

            CardExplosionLayer(
                modifier = Modifier
                    .size(transition.cardWidth, transition.cardHeight)
                    .graphicsLayer {
                        translationX = cardTranslationX
                        translationY = cardTranslationY
                        rotationX = (-visualTilt.y * 14f - inertia.y * 4f) * interactionTransformFade
                        rotationY = (visualTilt.x * 18f + inertia.x * 4.5f) * interactionTransformFade + cardFlipAngle
                        rotationZ = visualTilt.x * -2.4f * interactionTransformFade + cardSpinAngle
                        this.cameraDistance = cameraDistance
                    },
                progress = explosionProgress,
                chargeProgress = holdProgress * (1f - transition.progress),
                revealProgress = transition.progress
            )

            DustCollapseLayer(
                modifier = Modifier.fillMaxSize(),
                progress = dustCollapseProgress,
                direction = dustCollapseDirection,
                intensity = dustCollapseIntensity,
                revealProgress = transition.progress,
                cardWidth = transition.cardWidth,
                cardHeight = transition.cardHeight,
                originOffset = dustOriginOffset
            )

            if (dustCardHidden && !dustCollapseActive && dustCollapseProgress <= 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                dustCardHidden = false
                                dustCollapseDirection = 0f
                                dustCollapseIntensity = 0f
                                dustOriginOffset = Offset.Zero
                                triggerCinematicPulse(0.18f)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun MeshBackdropLayer(
    modifier: Modifier,
    backdropCenter: Offset,
    backdropRadius: Float,
    tilt: Offset,
    energy: Float,
    cinematicGlow: Float
) {
    Box(
        modifier = modifier.background(
            brush = Brush.radialGradient(
                colors = listOf(
                    MeshShaderPalette.ScreenBackgroundStart,
                    MeshShaderPalette.ScreenBackgroundMid,
                    MeshShaderPalette.ScreenBackgroundEnd
                ),
                center = backdropCenter,
                radius = backdropRadius
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MeshShaderPalette.AmbientCool.copy(alpha = 0.12f + cinematicGlow * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = size.width * (0.22f + tilt.x * 0.035f),
                        y = size.height * (0.16f + tilt.y * 0.028f)
                    ),
                    radius = size.minDimension * 0.9f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MeshShaderPalette.AmbientWarm.copy(alpha = 0.1f + energy * 0.06f + cinematicGlow * 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = size.width * (0.82f - tilt.x * 0.03f),
                        y = size.height * (0.28f - tilt.y * 0.022f)
                    ),
                    radius = size.minDimension * 0.74f
                )
            )
        }
    }
}

@Composable
private fun CardShadowLayer(
    modifier: Modifier,
    transition: ShaderElementTransition,
    tilt: Offset,
    energy: Float,
    pressProgress: Float,
    cinematicGlow: Float,
    shadowTravelX: Float,
    shadowTravelY: Float
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            translationX = (-tilt.x * shadowTravelX) * (1f - transition.progress * 0.82f)
            translationY = (transition.cardHeight * (0.42f - transition.progress * 0.18f)).toPx()
            translationY += (tilt.y * shadowTravelY) * (1f - transition.progress * 0.82f)
            alpha = transition.shadowAlpha * (0.88f + cinematicGlow * 0.16f)
            scaleX = 1f + abs(tilt.x) * 0.12f
            scaleY = 1f + abs(tilt.y) * 0.16f + pressProgress * 0.08f
        }
    ) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.28f + energy * 0.14f + cinematicGlow * 0.08f),
                    Color.Black.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.72f
            ),
            size = size
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    MeshShaderPalette.AmbientWarmShadow.copy(alpha = 0.14f + energy * 0.12f + cinematicGlow * 0.1f),
                    Color.Transparent
                ),
                center = Offset(
                    x = center.x + tilt.x * size.width * 0.12f,
                    y = center.y + tilt.y * size.height * 0.08f
                ),
                radius = size.minDimension * 0.44f
            ),
            size = Size(size.width * 0.72f, size.height * 0.68f),
            topLeft = Offset(size.width * 0.14f, size.height * 0.12f)
        )
    }
}

@Composable
private fun MeteorOverflowLayer(
    modifier: Modifier,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    cardOffset: Offset,
    tilt: Offset,
    energy: Float,
    pressProgress: Float,
    revealProgress: Float,
    flipProgress: Float,
    timeSeconds: Float,
    explosionProgress: Float,
    dustCollapseProgress: Float
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = (0.3f + energy * 0.85f) *
                (1f - revealProgress * 0.4f) *
                (1f - stagedProgress(explosionProgress, 0f, 0.12f)) *
                (1f - stagedProgress(dustCollapseProgress, 0.04f, 0.18f))
        }
    ) {
        val alphaScale = (
            0.08f +
                energy * 0.72f +
                pressProgress * 0.16f
            ).coerceIn(0f, 0.95f) * (1f - revealProgress * 0.34f) * (1f - flipProgress * 0.18f)
        if (alphaScale <= 0.01f) return@Canvas

        val cardCenter = Offset(
            x = size.width * 0.5f + cardOffset.x,
            y = size.height * 0.5f + cardOffset.y
        )
        val cardWidthPx = cardWidth.toPx()
        val cardHeightPx = cardHeight.toPx()
        val travelBase = min(size.width, size.height)
        val drift = Offset(
            x = tilt.x * cardWidthPx * 0.16f,
            y = tilt.y * cardHeightPx * 0.13f
        )

        overflowMeteorSpecs.forEach { spec ->
            val cycle = ((timeSeconds * spec.speed) + spec.phase) % 1f
            val eased = FastOutSlowInEasing.transform(cycle.coerceIn(0f, 1f))
            val envelope = pulseEnvelope(cycle)
            val brightness = envelope * alphaScale
            if (brightness <= 0.015f) return@forEach

            val origin = cardCenter +
                Offset(
                    x = spec.anchor.x * cardWidthPx,
                    y = spec.anchor.y * cardHeightPx
                ) +
                drift * spec.driftInfluence

            val span = travelBase * spec.span * (0.9f + energy * 0.26f)
            val head = origin + spec.direction * (span * eased)
            val trailLength = (cardWidthPx * 0.24f + span * 0.24f) * (0.82f + envelope * 0.36f)
            val tail = head - spec.direction * trailLength
            val strokeWidth = spec.strokeWidth.dp.toPx() * (0.82f + envelope * 0.58f)
            val headRadius = spec.headRadius.dp.toPx() * (0.88f + envelope * 0.72f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.color.copy(alpha = brightness * 0.34f),
                        Color.Transparent
                    ),
                    center = origin,
                    radius = headRadius * 3.2f
                ),
                radius = headRadius * 3.2f,
                center = origin
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        spec.color.copy(alpha = brightness * 0.2f),
                        spec.color.copy(alpha = brightness * 0.84f),
                        Color.White.copy(alpha = brightness)
                    ),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = brightness),
                        spec.color.copy(alpha = brightness * 0.52f),
                        Color.Transparent
                    ),
                    center = head,
                    radius = headRadius * 2.8f
                ),
                radius = headRadius * 2.8f,
                center = head
            )
        }
    }
}

@Composable
private fun DustCollapseLayer(
    modifier: Modifier,
    progress: Float,
    direction: Float,
    intensity: Float,
    revealProgress: Float,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    originOffset: Offset
) {
    if (progress <= 0f) return

    val particles = remember {
        List(7600) { index ->
            val seedA = abs(sin(index * 9.17f + 0.77f))
            val seedB = abs(cos(index * 6.13f + 1.91f))
            val seedC = abs(sin(index * 4.29f + 2.37f))
            val seedD = abs(cos(index * 11.73f + 0.53f))
            val seedE = abs(sin(index * 7.71f + 1.37f))
            val seedF = abs(cos(index * 13.47f + 2.11f))
            val angle = seedA * 6.2831855f
            val blobX = (((seedA + seedC + seedE) / 3f) - 0.5f) * 2f
            val blobY = (((seedB + seedD + seedF) / 3f) - 0.5f) * 2f
            DustParticleSpec(
                anchor = Offset(
                    x = blobX * (0.46f + seedA * 0.26f),
                    y = blobY * (0.26f + seedB * 0.18f) - 0.04f
                ),
                flow = Offset(
                    x = (-1f + seedD * 2f) * (0.2f + seedB * 0.36f),
                    y = -0.06f - seedE * 0.34f
                ),
                delay = seedC * 0.24f,
                size = 0.08f + seedA * 0.28f,
                phase = angle + seedF * 3.1415927f,
                color = when (index % 5) {
                    0 -> MeshShaderPalette.DustLight
                    1 -> MeshShaderPalette.DustSand
                    2 -> MeshShaderPalette.DustIvory
                    3 -> MeshShaderPalette.DustBrown
                    else -> MeshShaderPalette.DustStone
                }
            )
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = (1f - revealProgress * 0.94f).coerceIn(0f, 1f)
        }
    ) {
        val cardWidthPx = cardWidth.toPx()
        val cardHeightPx = cardHeight.toPx()
        val originCenter = Offset(
            x = size.width * 0.5f + originOffset.x,
            y = size.height * 0.5f + originOffset.y
        )
        val floorY = min(size.height * 0.9f, originCenter.y + cardHeightPx * 1.04f)
        val burst = stagedProgress(progress, 0f, 0.14f)
        val fall = stagedProgress(progress, 0.06f, 0.82f)
        val settle = stagedProgress(progress, 0.42f, 0.9f)
        val dissolve = 1f - stagedProgress(progress, 0.74f, 1f)
        val globalDrift = Offset(
            x = direction * size.width * (0.02f + intensity * 0.03f),
            y = size.height * (0.2f + intensity * 0.18f)
        )
        val hazeAlpha = (0.08f + intensity * 0.12f) * dissolve

        repeat(18) { index ->
            val seedA = abs(sin(index * 2.73f + 0.41f))
            val seedB = abs(cos(index * 3.91f + 1.17f))
            val seedC = abs(sin(index * 4.63f + 2.03f))
            val center = Offset(
                x = originCenter.x +
                    (((seedA + seedC) * 0.5f) - 0.5f) * cardWidthPx * 1.18f +
                    direction * cardWidthPx * 0.14f * fall,
                y = originCenter.y +
                    (seedB - 0.22f) * cardHeightPx * 0.9f +
                    cardHeightPx * 0.2f * fall
            )
            val hazeWidth = cardWidthPx * (0.18f + seedA * 0.16f)
            val hazeHeight = cardHeightPx * (0.1f + seedB * 0.08f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MeshShaderPalette.DustGlow.copy(alpha = hazeAlpha * (0.12f + seedC * 0.16f)),
                        MeshShaderPalette.DustGlowWarm.copy(alpha = hazeAlpha * (0.07f + seedA * 0.08f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxOf(hazeWidth, hazeHeight)
                ),
                topLeft = Offset(
                    x = center.x - hazeWidth * 0.5f,
                    y = center.y - hazeHeight * 0.5f
                ),
                size = Size(hazeWidth, hazeHeight)
            )
        }

        particles.forEach { particle ->
            val local = ((progress - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach

            val eased = local * local * (3f - 2f * local)
            val fade = (1f - local).coerceIn(0f, 1f)
            val fadeIn = stagedProgress(local, 0f, 0.16f)
            val start = originCenter + Offset(
                x = particle.anchor.x * cardWidthPx * 0.92f,
                y = particle.anchor.y * cardHeightPx * 0.72f
            )
            val flowOffset = Offset(
                x = particle.flow.x * size.width * 0.05f,
                y = particle.flow.y * size.height * 0.036f
            )
            val curl = Offset(
                x = sin(particle.phase + local * 4.2f) * size.width * 0.0052f,
                y = cos(particle.phase * 0.7f + local * 3.9f) * size.height * 0.0039f
            ) * (0.18f + fall * 0.82f)
            val flight = start + (flowOffset + globalDrift) * eased + curl
            val settled = Offset(
                x = start.x + direction * size.width * 0.055f + sin(particle.phase) * cardWidthPx * 0.11f,
                y = floorY - particle.size.dp.toPx() * (0.2f + abs(cos(particle.phase)) * 0.8f)
            )
            val settleMix = if (local < 0.5f) 0f else ((local - 0.5f) / 0.5f).coerceIn(0f, 1f) * settle
            val current = if (settleMix <= 0f) flight else Offset(
                x = flight.x + (settled.x - flight.x) * settleMix,
                y = flight.y + (settled.y - flight.y) * settleMix
            )
            val alpha = (
                fade *
                    (0.24f + intensity * 0.26f) *
                    (0.84f + burst * 0.46f)
                ).coerceIn(0f, 0.72f) * dissolve * fadeIn
            val radius = particle.size.dp.toPx() * (0.74f + fade * 0.5f)

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = radius,
                center = current
            )

            if (particle.size > 0.22f && local > 0.16f) {
                val haloAlpha = alpha * 0.22f
                val haloRadius = radius * (2.8f + local * 1.1f)
                drawCircle(
                    color = particle.color.copy(alpha = haloAlpha.coerceIn(0f, 0.12f)),
                    radius = haloRadius,
                    center = current
                )
            }
        }
    }
}

@Composable
private fun CardExplosionLayer(
    modifier: Modifier,
    progress: Float,
    chargeProgress: Float,
    revealProgress: Float
) {
    if (progress <= 0f && chargeProgress <= 0.01f) return

    val shards = remember {
        List(26) { index ->
            val seedA = abs(sin(index * 12.93f + 0.41f))
            val seedB = abs(cos(index * 8.17f + 1.13f))
            val seedC = abs(sin(index * 5.61f + 2.47f))
            val angle = -1.15f + seedA * 2.3f
            val rawDirection = Offset(cos(angle), sin(angle) * 0.92f - 0.08f)
            val normalizedDirection = rawDirection.magnitude().takeIf { it > 0f }?.let { magnitude ->
                Offset(rawDirection.x / magnitude, rawDirection.y / magnitude)
            } ?: Offset(1f, 0f)

            ExplosionShardSpec(
                anchor = Offset(
                    x = -0.44f + seedA * 0.88f,
                    y = -0.34f + seedB * 0.68f
                ),
                direction = normalizedDirection,
                size = if (index % 5 == 0) {
                    Size(42f + seedB * 24f, 16f + seedC * 14f)
                } else {
                    Size(16f + seedB * 18f, 4f + seedC * 10f)
                },
                spin = (-1f + seedB * 2f) * (0.8f + seedC * 1.2f),
                delay = seedC * 0.18f,
                travel = 0.22f + seedA * 0.34f,
                gravity = 0.08f + seedB * 0.12f,
                color = when (index % 4) {
                    0 -> MeshShaderPalette.MeteorAmber
                    1 -> MeshShaderPalette.MeteorIvory
                    2 -> MeshShaderPalette.MeteorOrange
                    else -> MeshShaderPalette.MeteorSoft
                }
            )
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = (1f - revealProgress * 0.92f).coerceIn(0f, 1f)
        }
    ) {
        val minDim = size.minDimension
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val charge = chargeProgress.coerceIn(0f, 1f)

        if (charge > 0.01f && progress <= 0.001f) {
            val borderAlpha = (charge * charge * 0.9f).coerceIn(0f, 0.88f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha * 0.5f),
                        MeshShaderPalette.MeteorFlare.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = borderAlpha * 0.4f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(34.dp.toPx(), 34.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx() + charge * 3.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MeshShaderPalette.MeteorFlare.copy(alpha = charge * 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = minDim * (0.28f + charge * 0.2f)
                ),
                radius = minDim * (0.28f + charge * 0.2f),
                center = center
            )
        }

        if (progress <= 0.001f) return@Canvas

        val flashAlpha = (1f - stagedProgress(progress, 0f, 0.2f)).coerceIn(0f, 1f)
        val ringProgress = stagedProgress(progress, 0.04f, 0.54f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = flashAlpha * 0.55f),
                    MeshShaderPalette.MeteorFlare.copy(alpha = flashAlpha * 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = minDim * (0.16f + progress * 0.36f)
            ),
            radius = minDim * (0.16f + progress * 0.36f),
            center = center
        )

        if (ringProgress > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = (1f - ringProgress) * 0.22f),
                radius = minDim * (0.18f + ringProgress * 0.62f),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        shards.forEach { shard ->
            val local = ((progress - shard.delay) / (1f - shard.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach

            val eased = 1f - (1f - local) * (1f - local)
            val alpha = (1f - local).coerceIn(0f, 1f)
            val origin = Offset(
                x = size.width * (0.5f + shard.anchor.x * 0.72f),
                y = size.height * (0.5f + shard.anchor.y * 0.72f)
            )
            val centerShift = shard.direction * (minDim * shard.travel * eased)
            val gravityShift = Offset(0f, minDim * shard.gravity * local * local)
            val shardCenter = origin + centerShift + gravityShift
            val trailAlpha = alpha * 0.54f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        shard.color.copy(alpha = trailAlpha * 0.42f),
                        shard.color.copy(alpha = trailAlpha),
                        Color.White.copy(alpha = trailAlpha)
                    ),
                    start = origin,
                    end = shardCenter
                ),
                start = origin,
                end = shardCenter,
                strokeWidth = 1.2.dp.toPx() + shard.size.minDimension * 0.04f,
                cap = StrokeCap.Round
            )

            rotate(
                degrees = shard.spin * 260f * eased,
                pivot = shardCenter
            ) {
                val topLeft = Offset(
                    x = shardCenter.x - shard.size.width * 0.5f,
                    y = shardCenter.y - shard.size.height * 0.5f
                )
                val corner = CornerRadius(
                    x = shard.size.minDimension * 0.28f,
                    y = shard.size.minDimension * 0.28f
                )

                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            shard.color.copy(alpha = alpha * 0.18f),
                            shard.color.copy(alpha = alpha * 0.72f),
                            Color.White.copy(alpha = alpha * 0.34f)
                        ),
                        start = Offset(
                            x = shardCenter.x - shard.size.width,
                            y = shardCenter.y - shard.size.height
                        ),
                        end = Offset(
                            x = shardCenter.x + shard.size.width,
                            y = shardCenter.y + shard.size.height
                        )
                    ),
                    topLeft = topLeft,
                    size = shard.size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = alpha * 0.18f),
                    topLeft = topLeft,
                    size = shard.size,
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun InteractiveShaderCard(
    modifier: Modifier,
    shape: RoundedCornerShape,
    cornerRadius: androidx.compose.ui.unit.Dp,
    tilt: Offset,
    inertia: Offset,
    focus: Offset,
    focusGlow: Float,
    pressProgress: Float,
    flipRotation: Float,
    transition: ShaderElementTransition,
    targetTilt: Offset,
    onTiltChange: (Offset) -> Unit,
    onFocusChange: (Offset) -> Unit,
    onGestureStart: () -> Unit,
    onGestureEnd: (DragReleaseState) -> Unit,
    onPressChange: (Boolean) -> Unit,
    onFlipPreviewChange: (Float) -> Unit,
    onFlipTrigger: (Float) -> Unit,
    onSpinTrigger: () -> Unit,
    onDustCollapseTrigger: (Float, Float) -> Unit,
    onCardTap: () -> Unit,
    interactionsEnabled: Boolean,
    holdProgress: Float,
    timeSeconds: Float,
    energy: Float
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val latestTargetTilt by rememberUpdatedState(targetTilt)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) {
        onPressChange(pressed)
    }

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MeshShaderPalette.CardSurfaceTop,
                        MeshShaderPalette.CardSurfaceBottom
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MeshShaderPalette.CardBorder.copy(alpha = 0.08f),
                shape = shape
            )
            .combinedClickable(
                enabled = interactionsEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardTap
            )
            .pointerInput(cardSize, interactionsEnabled) {
                if (!interactionsEnabled) return@pointerInput
                var dragTilt = Offset.Zero
                var releaseImpulse = Offset.Zero
                var totalDrag = Offset.Zero
                detectDragGestures(
                    onDragStart = { start ->
                        dragTilt = latestTargetTilt
                        releaseImpulse = Offset.Zero
                        totalDrag = Offset.Zero
                        onFlipPreviewChange(0f)
                        onFocusChange(start.normalizedPositionIn(cardSize))
                        onGestureStart()
                    },
                    onDragEnd = {
                        if (transition.progress < 0.08f && cardSize.width > 0) {
                            val threshold = cardSize.width * 0.16f
                            val verticalThreshold = cardSize.height * 0.18f
                            val instantSwipe = abs(releaseImpulse.x) > 0.03f &&
                                abs(releaseImpulse.x) > abs(releaseImpulse.y) * 1.35f &&
                                abs(totalDrag.x) > threshold * 0.7f
                            if (instantSwipe) {
                                val direction = if (totalDrag.x >= 0f) 1f else -1f
                                val intensity = (
                                    (abs(releaseImpulse.x) / 0.08f) * 0.7f +
                                        (abs(totalDrag.x) / cardSize.width.toFloat()) * 0.3f
                                    ).coerceIn(0.62f, 1f)
                                onDustCollapseTrigger(direction, intensity)
                            } else if (totalDrag.x > threshold && totalDrag.x > abs(totalDrag.y) * 1.2f) {
                                onFlipTrigger(1f)
                            } else if (totalDrag.x < -threshold && -totalDrag.x > abs(totalDrag.y) * 1.2f) {
                                onFlipTrigger(-1f)
                            } else if (
                                totalDrag.y > verticalThreshold &&
                                totalDrag.y > abs(totalDrag.x) * 1.25f
                            ) {
                                onSpinTrigger()
                            }
                        }
                        onFlipPreviewChange(0f)
                        onGestureEnd(
                            DragReleaseState(
                                impulse = releaseImpulse,
                                tilt = dragTilt
                            )
                        )
                    },
                    onDragCancel = {
                        onFlipPreviewChange(0f)
                        onGestureEnd(
                            DragReleaseState(
                                impulse = Offset.Zero,
                                tilt = latestTargetTilt
                            )
                        )
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                    val normalizedDelta = dragAmount.normalizedDeltaIn(cardSize) * 0.62f
                    dragTilt = (dragTilt + normalizedDelta).limit(0.92f)
                    onTiltChange(dragTilt)
                    onFocusChange(change.position.normalizedPositionIn(cardSize))
                    releaseImpulse = (releaseImpulse * 0.84f + normalizedDelta * 0.16f).limit(0.08f)
                    if (transition.progress < 0.08f && cardSize.width > 0) {
                        val preview = (totalDrag.x / cardSize.width.toFloat()).coerceIn(-1f, 1f)
                        onFlipPreviewChange(preview)
                    }
                }
            }
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShaderLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = transition.shaderAlpha },
                cornerRadius = cornerRadius,
                tilt = tilt,
                inertia = inertia,
                focus = focus,
                focusGlow = focusGlow,
                revealProgress = transition.progress,
                timeSeconds = timeSeconds,
                energy = energy
            )
        } else {
            FallbackShaderLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = transition.shaderAlpha },
                tilt = tilt,
                inertia = inertia,
                focus = focus,
                focusGlow = focusGlow,
                revealProgress = transition.progress,
                energy = energy,
                timeSeconds = timeSeconds
            )
        }

        PointerGlossLayer(
            modifier = Modifier.fillMaxSize(),
            tilt = tilt,
            focus = focus,
            glow = focusGlow,
            revealProgress = transition.progress
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = transition.contentAlpha
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f - pressProgress * 0.015f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f + pressProgress * 0.08f)
                        )
                    )
                )
        )

        if (holdProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MeshShaderPalette.MeteorFlare.copy(alpha = holdProgress * 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(
                                x = cardSize.width * 0.5f,
                                y = cardSize.height * 0.46f
                            ),
                            radius = kotlin.math.max(cardSize.width, cardSize.height).toFloat() * 0.72f
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.035f + energy * 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(
                            x = cardSize.width * 0.5f,
                            y = cardSize.height * 0.22f
                        ),
                        radius = kotlin.math.max(
                            kotlin.math.max(cardSize.width, cardSize.height).toFloat() * 0.8f,
                            1f
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = pressProgress * 0.05f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MeshShaderPalette.CardDetailScrim.copy(alpha = transition.detailAlpha * 0.26f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = transition.glossAlpha
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(800f, 520f)
                    )
                )
        )

        val displayedFlipAngle = positiveDegrees(flipRotation)
        val faceFrontness = cos(Math.toRadians(displayedFlipAngle.toDouble())).toFloat()
        val frontFaceAlpha = transition.contentAlpha * faceFrontness.coerceAtLeast(0f)
        val backFaceAlpha = (1f - transition.progress) * (-faceFrontness).coerceAtLeast(0f)

        CardFrontFace(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = frontFaceAlpha
                },
            transition = transition
        )

        CardBackFace(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = 180f
                    alpha = backFaceAlpha
                }
        )

        if (transition.progress > 0.7f) {
            MeshChip(
                text = MeshShaderCopy.DetailBadge,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        alpha = ((transition.progress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    }
                    .padding(horizontal = 18.dp, vertical = 22.dp)
            )
        }

        DetailScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = transition.detailAlpha
                    translationY = (1f - transition.detailAlpha) * 32.dp.toPx()
                },
            detailAlpha = transition.detailAlpha,
            timeSeconds = timeSeconds
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RuntimeShaderLayer(
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
    tilt: Offset,
    inertia: Offset,
    focus: Offset,
    focusGlow: Float,
    revealProgress: Float,
    timeSeconds: Float,
    energy: Float
) {
    val shader = remember { RuntimeShader(MESH_SHADER_SRC) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

    Canvas(modifier = modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", timeSeconds)
        shader.setFloatUniform("drag", tilt.x, tilt.y)
        shader.setFloatUniform("inertia", inertia.x, inertia.y)
        shader.setFloatUniform("focus", focus.x, focus.y)
        shader.setFloatUniform("focusGlow", focusGlow)
        shader.setFloatUniform("revealProgress", revealProgress)
        shader.setFloatUniform("energy", energy)

        paint.shader = shader

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerRadius.toPx(),
                cornerRadius.toPx(),
                paint
            )
        }
    }
}

@Composable
private fun FallbackShaderLayer(
    modifier: Modifier,
    tilt: Offset,
    inertia: Offset,
    focus: Offset,
    focusGlow: Float,
    revealProgress: Float,
    energy: Float,
    timeSeconds: Float
) {
    Canvas(modifier = modifier) {
        val glowCenter = Offset(
            x = size.width * focus.x,
            y = size.height * focus.y
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    MeshShaderPalette.ShaderFallbackBlue.copy(alpha = 0.16f + focusGlow * 0.08f + energy * 0.1f),
                    MeshShaderPalette.ShaderFallbackMidnight.copy(alpha = 0.84f),
                    MeshShaderPalette.ShaderFallbackWarm.copy(alpha = 0.46f + energy * 0.08f),
                    MeshShaderPalette.ShaderFallbackBase
                ),
                center = glowCenter,
                radius = size.minDimension * (0.54f + focusGlow * 0.08f + energy * 0.14f)
            )
        )

        val lineColor = Color.White.copy(alpha = 0.055f + energy * 0.025f)
        for (index in 0..14) {
            val progress = index / 14f
            val offset = (progress - 0.5f + tilt.x * 0.05f + inertia.x * 0.04f) * size.width
            val bend = kotlin.math.sin(timeSeconds * 1.3f + progress * 4f) * size.height * 0.04f
            drawLine(
                color = lineColor,
                start = Offset(offset, bend),
                end = Offset(offset + size.width * 0.3f, size.height - bend),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawCircle(
            color = MeshShaderPalette.ShaderFallbackAccent.copy(alpha = 0.12f + energy * 0.1f),
            radius = size.minDimension * (0.18f + energy * 0.02f),
            center = Offset(
                x = size.width * (0.72f - tilt.x * 0.08f - inertia.x * 0.05f),
                y = size.height * (0.24f + tilt.y * 0.04f + kotlin.math.sin(timeSeconds * 1.4f) * 0.03f)
            )
        )
    }
}

@Composable
private fun PointerGlossLayer(
    modifier: Modifier,
    tilt: Offset,
    focus: Offset,
    glow: Float,
    revealProgress: Float
) {
    Canvas(modifier = modifier) {
        val focusCenter = Offset(
            x = size.width * focus.x,
            y = size.height * focus.y
        )
        val tiltMagnitude = tilt.magnitude().coerceIn(0f, 1f)
        val diagonalVector = Offset(
            x = size.width * (0.28f + tilt.x * 0.06f),
            y = size.height * (-0.16f + tilt.y * 0.05f)
        )
        val fresnel = (0.018f + tiltMagnitude * 0.034f + glow * 0.04f) * (1f - revealProgress * 0.88f)
        val leftEdge = fresnel * (0.55f + (-tilt.x).coerceAtLeast(0f) * 1.2f)
        val rightEdge = fresnel * (0.55f + tilt.x.coerceAtLeast(0f) * 1.2f)
        val topEdge = fresnel * (0.7f + (-tilt.y).coerceAtLeast(0f) * 1.1f)
        val bottomEdge = fresnel * (0.42f + tilt.y.coerceAtLeast(0f) * 0.92f)
        val sheenStart = focusCenter - diagonalVector * 1.45f
        val sheenEnd = focusCenter + diagonalVector * 2.1f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.045f + glow * 0.09f + tiltMagnitude * 0.02f),
                    MeshShaderPalette.HighlightCool.copy(alpha = 0.022f + glow * 0.035f),
                    Color.Transparent
                ),
                center = focusCenter,
                radius = size.minDimension * (0.18f + glow * 0.06f + tiltMagnitude * 0.03f)
            ),
            alpha = 1f - revealProgress * 0.86f
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.03f + glow * 0.05f + tiltMagnitude * 0.018f),
                    MeshShaderPalette.HighlightWarm.copy(alpha = 0.05f + glow * 0.045f + tiltMagnitude * 0.03f),
                    Color.Transparent
                ),
                start = sheenStart,
                end = sheenEnd
            ),
            alpha = 1f - revealProgress * 0.88f
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f + glow * 0.03f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                start = Offset(size.width * 0.08f, size.height * 0.04f),
                end = Offset(size.width * 0.62f, size.height * 0.34f)
            ),
            alpha = 1f - revealProgress * 0.95f
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = leftEdge),
                    Color.Transparent
                ),
                startX = 0f,
                endX = size.width * 0.2f
            )
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = rightEdge)
                ),
                startX = size.width * 0.8f,
                endX = size.width
            )
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = topEdge),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.22f
            )
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = bottomEdge)
                ),
                startY = size.height * 0.76f,
                endY = size.height
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.07f + fresnel * 0.95f),
                    Color.Transparent
                ),
                center = Offset(
                    x = size.width * (0.18f - tilt.x * 0.08f),
                    y = size.height * (0.12f - tilt.y * 0.06f)
                ),
                radius = size.minDimension * 0.42f
            ),
            alpha = 1f - revealProgress * 0.92f
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MeshShaderScreenPreview() {
    MeshTheme(darkTheme = true, dynamicColor = false) {
        MeshShaderScreen()
    }
}
