package com.ardakazanci.mesh

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object MeshShaderCopy {
    const val Title = "Mesh Drift"
    const val CardBadge = "AGSL"
    const val DetailBadge = "Lorem ipsum"
    const val UpperLabel = "LOREM IPSUM"
    const val Body = "Lorem ipsum"

    fun detailMeta(detailAlpha: Float): String = "Lorem ipsum ${(36 + detailAlpha * 18).toInt()}%"
}

internal object MeshShaderPalette {
    val ScreenBackgroundStart = Color(0xFF2C3542)
    val ScreenBackgroundMid = Color(0xFF171D26)
    val ScreenBackgroundEnd = Color(0xFF0B0F14)

    val AmbientCool = Color(0xFF7DB4FF)
    val AmbientWarm = Color(0xFFFFA15C)
    val AmbientWarmShadow = Color(0xFF120D0A)

    val CardSurfaceTop = Color(0xFF111317)
    val CardSurfaceBottom = Color(0xFF090A0D)
    val CardBorder = Color.White
    val CardDetailScrim = Color(0xFF040608)

    val SoftText = Color(0xFFB7BEC9)
    val DetailText = Color(0xFFC4CBD5)
    val BackFaceText = Color(0xFFC3CBD5)

    val ShaderFallbackBlue = Color(0xFF4F95E8)
    val ShaderFallbackMidnight = Color(0xFF101722)
    val ShaderFallbackWarm = Color(0xFF2B1112)
    val ShaderFallbackBase = Color(0xFF060709)
    val ShaderFallbackAccent = Color(0xFFFF8A5B)

    val HighlightCool = Color(0xFFE9F2FF)
    val HighlightWarm = Color(0xFFEAE6E1)

    val MeteorIvory = Color(0xFFF7F6F4)
    val MeteorAmber = Color(0xFFFFB15B)
    val MeteorSoft = Color(0xFFEFEAE4)
    val MeteorOrange = Color(0xFFFF8A2E)
    val MeteorCloud = Color(0xFFF5F5F3)
    val MeteorFlare = Color(0xFFFFA24D)

    val DustLight = Color(0xFFE9E1D7)
    val DustSand = Color(0xFFC7B3A0)
    val DustIvory = Color(0xFFF6EBDC)
    val DustBrown = Color(0xFF9E856D)
    val DustStone = Color(0xFFD6C8B8)
    val DustGlow = Color(0xFFE6D6C5)
    val DustGlowWarm = Color(0xFFB69476)

    val HeroBackgroundStart = Color(0xFF1A1715)
    val HeroBackgroundMid = Color(0xFF0C0D10)
    val HeroBackgroundEnd = Color(0xFF08090B)

    val HeroMeteorPalette = listOf(
        MeteorAmber,
        Color(0xFFF7F7F7),
        MeteorOrange,
        Color(0xFFEFEFEF)
    )
}

internal object MeshShaderDefaults {
    val CompactCardMaxWidth = 360.dp
    val CompactCardCornerRadius = 34.dp
    val DetailHeroCornerRadius = 26.dp
    val ChipCornerRadius = 50

    const val TripleTapWindowMillis = 450L
    const val RevealDurationMillis = 760
    const val FlipDurationMillis = 920
    const val SpinDurationMillis = 1320

    const val HoldToExplodeSeconds = 10f
    const val ExplosionDurationSeconds = 1.35f
    const val DustCollapseDurationSeconds = 2.85f
    const val DustHideThreshold = 0.24f
}
