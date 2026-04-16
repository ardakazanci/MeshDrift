package com.ardakazanci.mesh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun MeshChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White.copy(alpha = 0.74f),
    containerColor: Color = Color.White.copy(alpha = 0.06f),
    borderColor: Color = MeshShaderPalette.CardBorder.copy(alpha = 0.08f)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = MeshShaderDefaults.ChipCornerRadius))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(percent = MeshShaderDefaults.ChipCornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun CardFrontFace(
    modifier: Modifier,
    transition: ShaderElementTransition
) {
    Box(modifier = modifier) {
        MeshChip(
            text = MeshShaderCopy.CardBadge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { alpha = transition.contentAlpha }
                .padding(18.dp),
            textColor = Color.White.copy(alpha = 0.78f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    alpha = transition.contentAlpha
                    translationY = transition.progress * 28.dp.toPx()
                }
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Text(
                text = MeshShaderCopy.Title,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MeshShaderCopy.Body,
                color = MeshShaderPalette.SoftText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
internal fun CardBackFace(
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        MeshChip(
            text = MeshShaderCopy.UpperLabel,
            textColor = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = MeshShaderCopy.Body,
            color = Color.White.copy(alpha = 0.96f),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MeshShaderCopy.Body,
            color = MeshShaderPalette.BackFaceText,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = MeshShaderCopy.Body,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun DetailScreenContent(
    modifier: Modifier,
    detailAlpha: Float,
    timeSeconds: Float
) {
    val badgeAlpha = stagedProgress(detailAlpha, 0f, 0.28f)
    val heroAlpha = stagedProgress(detailAlpha, 0.12f, 0.48f)
    val titleAlpha = stagedProgress(detailAlpha, 0.28f, 0.62f)
    val bodyAlpha = stagedProgress(detailAlpha, 0.4f, 0.78f)
    val metaAlpha = stagedProgress(detailAlpha, 0.58f, 0.94f)

    Column(
        modifier = modifier.padding(horizontal = 22.dp, vertical = 28.dp)
    ) {
        Spacer(modifier = Modifier.height(42.dp))

        MeshChip(
            text = MeshShaderCopy.UpperLabel,
            modifier = Modifier.graphicsLayer {
                alpha = badgeAlpha
                translationY = (1f - badgeAlpha) * 16.dp.toPx()
                scaleX = 0.96f + badgeAlpha * 0.04f
                scaleY = 0.96f + badgeAlpha * 0.04f
            },
            textColor = Color.White.copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        DetailHeroImage(
            modifier = Modifier.graphicsLayer {
                alpha = heroAlpha
                translationY = (1f - heroAlpha) * 24.dp.toPx()
                scaleX = 0.95f + heroAlpha * 0.05f
                scaleY = 0.95f + heroAlpha * 0.05f
            },
            timeSeconds = timeSeconds,
            shimmer = 0.6f + 0.4f * sin(timeSeconds * 0.9f),
            emphasis = detailAlpha
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = MeshShaderCopy.Title,
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                translationY = (1f - titleAlpha) * 20.dp.toPx()
            },
            color = Color.White.copy(alpha = 0.96f),
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = MeshShaderCopy.Body,
            modifier = Modifier.graphicsLayer {
                alpha = bodyAlpha
                translationY = (1f - bodyAlpha) * 24.dp.toPx()
            },
            color = MeshShaderPalette.DetailText,
            fontSize = 15.sp,
            lineHeight = 23.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = MeshShaderCopy.detailMeta(detailAlpha),
            modifier = Modifier.graphicsLayer {
                alpha = metaAlpha
                translationY = (1f - metaAlpha) * 18.dp.toPx()
            },
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailHeroImage(
    modifier: Modifier = Modifier,
    timeSeconds: Float,
    shimmer: Float,
    emphasis: Float
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(MeshShaderDefaults.DetailHeroCornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MeshShaderPalette.HeroBackgroundStart,
                        MeshShaderPalette.HeroBackgroundMid,
                        MeshShaderPalette.HeroBackgroundEnd
                    ),
                    start = Offset.Zero,
                    end = Offset(820f, 540f)
                )
            )
            .border(
                width = 1.dp,
                color = MeshShaderPalette.CardBorder.copy(alpha = 0.08f),
                shape = RoundedCornerShape(MeshShaderDefaults.DetailHeroCornerRadius)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val localGlow = 0.1f + emphasis * 0.1f

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MeshShaderPalette.MeteorOrange.copy(alpha = 0.14f + localGlow),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.76f, size.height * 0.26f),
                    radius = size.minDimension * 0.48f
                )
            )

            repeat(18) { index ->
                val seed = abs(sin(index * 12.37f + 0.85f))
                val secondarySeed = abs(kotlin.math.cos(index * 7.91f + 1.24f))
                val fromRight = index % 4 == 0 || index % 5 == 0
                val speed = 0.08f + secondarySeed * 0.12f
                val color = MeshShaderPalette.HeroMeteorPalette[index % MeshShaderPalette.HeroMeteorPalette.size]
                val travel = ((timeSeconds * speed) + seed * 1.45f) % 1.35f
                val x = if (fromRight) {
                    size.width * (1.14f - travel)
                } else {
                    size.width * (-0.14f + travel)
                }
                val lane = size.height * (0.12f + seed * 0.7f)
                val bend = sin(timeSeconds * (1.1f + secondarySeed * 1.7f) + index * 0.55f) *
                    size.height * 0.045f
                val head = Offset(x, lane + bend)
                val tailLength = size.width * (0.08f + secondarySeed * 0.1f)
                val tailDrift = size.height * (0.028f + seed * 0.032f)
                val tail = Offset(
                    x = head.x + if (fromRight) tailLength else -tailLength,
                    y = head.y - tailDrift
                )
                val particleAlpha = (0.28f + emphasis * 0.2f + secondarySeed * 0.28f)
                    .coerceIn(0f, 0.92f)
                val strokeWidth = 1.8.dp.toPx() * (0.8f + secondarySeed * 0.95f)
                val headRadius = 2.dp.toPx() * (0.8f + seed * 0.9f)

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color.copy(alpha = particleAlpha * (0.34f + shimmer * 0.12f)),
                            color.copy(alpha = particleAlpha * (0.88f + shimmer * 0.08f)),
                            Color.White.copy(alpha = particleAlpha)
                        ),
                        start = tail,
                        end = head
                    ),
                    start = tail,
                    end = head,
                    strokeWidth = strokeWidth
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = particleAlpha),
                            color.copy(alpha = particleAlpha * 0.74f),
                            Color.Transparent
                        ),
                        center = head,
                        radius = headRadius * 2.4f
                    ),
                    radius = headRadius * 2.4f,
                    center = head
                )
            }

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f + shimmer * 0.05f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * 0.16f, size.height * 0.04f),
                    end = Offset(size.width * 0.68f, size.height * 0.62f)
                )
            )
        }

        MeshChip(
            text = MeshShaderCopy.DetailBadge,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            containerColor = Color.Black.copy(alpha = 0.28f),
            borderColor = Color.Transparent,
            textColor = Color.White.copy(alpha = 0.84f)
        )
    }
}
