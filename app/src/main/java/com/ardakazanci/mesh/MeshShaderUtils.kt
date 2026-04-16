package com.ardakazanci.mesh

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.abs
import kotlin.math.sqrt

internal operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

internal fun positiveDegrees(value: Float): Float {
    val wrapped = value % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

internal fun premiumRestingTilt(source: Offset): Offset {
    val magnitude = source.magnitude()
    if (magnitude < 0.14f) return Offset.Zero

    val candidates = listOf(
        Offset(-0.18f, -0.12f),
        Offset(0.18f, -0.12f),
        Offset(-0.14f, 0.1f),
        Offset(0.14f, 0.1f)
    )
    val strength = ((magnitude - 0.14f) / 0.5f).coerceIn(0f, 1f)
    val nearest = candidates.maxByOrNull { candidate ->
        candidate.x * source.x + candidate.y * source.y
    } ?: Offset.Zero

    return nearest * (0.52f + strength * 0.38f)
}

internal fun stagedProgress(progress: Float, start: Float, end: Float): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f).let(FastOutSlowInEasing::transform)
}

internal fun pulseEnvelope(progress: Float): Float {
    val triangle = (1f - abs(progress * 2f - 1f)).coerceIn(0f, 1f)
    return triangle * triangle * (3f - 2f * triangle)
}

@Composable
internal fun rememberShaderElementTransition(
    compactCardWidth: Dp,
    compactCardHeight: Dp,
    expandedWidth: Dp,
    expandedHeight: Dp,
    progress: Float
): ShaderElementTransition {
    val clamped = progress.coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(clamped)
    return remember(
        compactCardWidth,
        compactCardHeight,
        expandedWidth,
        expandedHeight,
        clamped
    ) {
        ShaderElementTransition(
            progress = clamped,
            cardWidth = lerp(compactCardWidth, expandedWidth, eased),
            cardHeight = lerp(compactCardHeight, expandedHeight, eased),
            cornerRadius = lerp(MeshShaderDefaults.CompactCardCornerRadius, 0.dp, eased),
            shaderAlpha = 1f - clamped * 0.38f,
            shadowAlpha = 1f - clamped * 0.96f,
            contentAlpha = 1f - clamped * 0.84f,
            glossAlpha = 1f - clamped * 0.97f,
            detailAlpha = ((clamped - 0.08f) / 0.78f).coerceIn(0f, 1f)
        )
    }
}

internal fun Offset.magnitude(): Float = sqrt(x * x + y * y)

internal fun Offset.limit(maxMagnitude: Float): Offset {
    val magnitude = magnitude()
    if (magnitude <= maxMagnitude || magnitude == 0f) return this
    val scale = maxMagnitude / magnitude
    return this * scale
}

internal fun Offset.normalizedDeltaIn(size: IntSize): Offset {
    val base = minOf(size.width, size.height).toFloat().takeIf { it > 0f } ?: return Offset.Zero
    return Offset(x / base, y / base)
}

internal fun Offset.normalizedPositionIn(size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return Offset(0.5f, 0.5f)
    return Offset(
        x = (x / size.width.toFloat()).coerceIn(0f, 1f),
        y = (y / size.height.toFloat()).coerceIn(0f, 1f)
    )
}
