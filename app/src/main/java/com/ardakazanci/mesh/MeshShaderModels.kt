package com.ardakazanci.mesh

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
internal data class ShaderElementTransition(
    val progress: Float,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val cornerRadius: Dp,
    val shaderAlpha: Float,
    val shadowAlpha: Float,
    val contentAlpha: Float,
    val glossAlpha: Float,
    val detailAlpha: Float
)

@Immutable
internal data class OverflowMeteorSpec(
    val anchor: Offset,
    val direction: Offset,
    val phase: Float,
    val speed: Float,
    val span: Float,
    val strokeWidth: Float,
    val headRadius: Float,
    val driftInfluence: Float,
    val color: Color
)

@Immutable
internal data class ExplosionShardSpec(
    val anchor: Offset,
    val direction: Offset,
    val size: Size,
    val spin: Float,
    val delay: Float,
    val travel: Float,
    val gravity: Float,
    val color: Color
)

@Immutable
internal data class DustParticleSpec(
    val anchor: Offset,
    val flow: Offset,
    val delay: Float,
    val size: Float,
    val phase: Float,
    val color: Color
)

@Immutable
internal data class DragReleaseState(
    val impulse: Offset,
    val tilt: Offset
)

internal val overflowMeteorSpecs = listOf(
    OverflowMeteorSpec(
        anchor = Offset(-0.44f, -0.24f),
        direction = Offset(0.94f, -0.34f),
        phase = 0.08f,
        speed = 0.11f,
        span = 0.58f,
        strokeWidth = 5.2f,
        headRadius = 5.8f,
        driftInfluence = 0.9f,
        color = MeshShaderPalette.MeteorIvory
    ),
    OverflowMeteorSpec(
        anchor = Offset(0.42f, -0.08f),
        direction = Offset(0.88f, 0.46f),
        phase = 0.24f,
        speed = 0.09f,
        span = 0.62f,
        strokeWidth = 5.8f,
        headRadius = 6.4f,
        driftInfluence = 0.72f,
        color = MeshShaderPalette.MeteorAmber
    ),
    OverflowMeteorSpec(
        anchor = Offset(-0.1f, 0.36f),
        direction = Offset(0.98f, 0.18f),
        phase = 0.38f,
        speed = 0.1f,
        span = 0.54f,
        strokeWidth = 4.6f,
        headRadius = 5.2f,
        driftInfluence = 0.65f,
        color = MeshShaderPalette.MeteorSoft
    ),
    OverflowMeteorSpec(
        anchor = Offset(0.18f, -0.38f),
        direction = Offset(0.82f, -0.58f),
        phase = 0.52f,
        speed = 0.125f,
        span = 0.52f,
        strokeWidth = 4.8f,
        headRadius = 5.4f,
        driftInfluence = 1.08f,
        color = MeshShaderPalette.MeteorOrange
    ),
    OverflowMeteorSpec(
        anchor = Offset(0.34f, 0.3f),
        direction = Offset(0.78f, 0.62f),
        phase = 0.68f,
        speed = 0.084f,
        span = 0.57f,
        strokeWidth = 5.4f,
        headRadius = 6.0f,
        driftInfluence = 0.84f,
        color = MeshShaderPalette.MeteorCloud
    ),
    OverflowMeteorSpec(
        anchor = Offset(-0.32f, 0.12f),
        direction = Offset(0.96f, 0.28f),
        phase = 0.84f,
        speed = 0.104f,
        span = 0.6f,
        strokeWidth = 4.9f,
        headRadius = 5.5f,
        driftInfluence = 0.76f,
        color = MeshShaderPalette.MeteorFlare
    )
)
