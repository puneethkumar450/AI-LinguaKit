package com.puneeth50.ailinguakit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wraps any composable and dissolves its last rendered pixels into animated particles when
 * [visible] changes from true to false.
 *
 * Example:
 * ```
 * DisintegrationBox(
 *     visible = isVisible,
 *     onAnimationFinished = { removeItem() }
 * ) {
 *     Card { Text("Delete me") }
 * }
 * ```
 */
@Composable
fun DisintegrationBox(
    visible: Boolean,
    onAnimationFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    spec: DisintegrationEffectSpec = DisintegrationEffectSpec(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    val currentOnAnimationFinished by rememberUpdatedState(onAnimationFinished)

    var renderContent by remember { mutableStateOf(visible) }
    var hasBeenVisible by remember { mutableStateOf(visible) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var particleLayerSize by remember { mutableStateOf(IntSize.Zero) }
    var particles by remember { mutableStateOf<List<DisintegrationParticle>>(emptyList()) }

    LaunchedEffect(visible) {
        if (visible) {
            hasBeenVisible = true
            renderContent = true
            particles = emptyList()
            particleLayerSize = IntSize.Zero
            progress.snapTo(0f)
            return@LaunchedEffect
        }

        if (!hasBeenVisible) {
            renderContent = false
            particles = emptyList()
            particleLayerSize = IntSize.Zero
            return@LaunchedEffect
        }

        val imageBitmap = runCatching { graphicsLayer.toImageBitmap() }.getOrNull()
        if (imageBitmap == null || imageBitmap.width <= 0 || imageBitmap.height <= 0) {
            renderContent = false
            particles = emptyList()
            particleLayerSize = IntSize.Zero
            currentOnAnimationFinished()
            return@LaunchedEffect
        }

        val buildSpec = with(density) {
            DisintegrationBuildSpec(
                particleSpacingPx = spec.particleSpacing.roundToPx().coerceAtLeast(1),
                particleRadiusPx = spec.particleSize.toPx().coerceAtLeast(0.5f),
                maxParticles = spec.maxParticles.coerceAtLeast(1),
                alphaThreshold = spec.alphaThreshold.coerceIn(0, 255),
                driftDistancePx = spec.driftDistance.toPx().coerceAtLeast(0f),
                gravityPx = spec.gravity.toPx().coerceAtLeast(0f),
                spread = spec.spread,
                seed = spec.seed
            )
        }

        val generatedParticles = withContext(Dispatchers.Default) {
            imageBitmap.extractDisintegrationParticles(buildSpec)
        }

        renderContent = false
        if (generatedParticles.isEmpty()) {
            particles = emptyList()
            particleLayerSize = IntSize.Zero
            currentOnAnimationFinished()
            return@LaunchedEffect
        }

        particleLayerSize = IntSize(imageBitmap.width, imageBitmap.height)
        particles = generatedParticles
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = spec.durationMillis.coerceAtLeast(1),
                easing = FastOutSlowInEasing
            )
        )
        particles = emptyList()
        particleLayerSize = IntSize.Zero
        currentOnAnimationFinished()
    }

    val shouldLockSize = !renderContent && particles.isNotEmpty() && layoutSize != IntSize.Zero
    val sizeLockModifier = if (shouldLockSize) {
        with(density) {
            Modifier.requiredSize(
                width = layoutSize.width.toDp(),
                height = layoutSize.height.toDp()
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(sizeLockModifier)
            .onSizeChanged { newSize ->
                if (newSize.width > 0 && newSize.height > 0) {
                    layoutSize = newSize
                }
            },
        contentAlignment = contentAlignment
    ) {
        if (renderContent) {
            Box(
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                },
                contentAlignment = contentAlignment,
                content = content
            )
        }

        if (particles.isNotEmpty() && particleLayerSize != IntSize.Zero) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = contentAlignment
            ) {
                DisintegrationParticleCanvas(
                    particles = particles,
                    progress = progress.value,
                    modifier = with(density) {
                        Modifier.requiredSize(
                            width = particleLayerSize.width.toDp(),
                            height = particleLayerSize.height.toDp()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DisintegrationEffect(
    visible: Boolean,
    onAnimationFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    spec: DisintegrationEffectSpec = DisintegrationEffectSpec(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    DisintegrationBox(
        visible = visible,
        onAnimationFinished = onAnimationFinished,
        modifier = modifier,
        spec = spec,
        contentAlignment = contentAlignment,
        content = content
    )
}

@Immutable
data class DisintegrationEffectSpec(
    val durationMillis: Int = 760,
    val particleSpacing: Dp = 2.dp,
    val particleSize: Dp = 1.4.dp,
    val maxParticles: Int = 1800,
    val alphaThreshold: Int = 24,
    val driftDistance: Dp = 54.dp,
    val gravity: Dp = 74.dp,
    val spread: DisintegrationSpreadDirection = DisintegrationSpreadDirection.Random,
    val seed: Int? = null
)

enum class DisintegrationSpreadDirection {
    Random,
    Left,
    Right,
    Up,
    Down,
    CenterOut
}

@Composable
private fun DisintegrationParticleCanvas(
    particles: List<DisintegrationParticle>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawDisintegrationParticles(particles, progress)
    }
}

private fun DrawScope.drawDisintegrationParticles(
    particles: List<DisintegrationParticle>,
    progress: Float
) {
    var index = 0
    while (index < particles.size) {
        val particle = particles[index]
        val localProgress = ((progress - particle.delay) / particle.activeDuration)
            .coerceIn(0f, 1f)
        val easedProgress = 1f - (1f - localProgress) * (1f - localProgress)
        val fade = (1f - localProgress) * (1f - localProgress)
        val scale = lerp(particle.startScale, particle.endScale, localProgress)
        val x = particle.x + particle.driftX * easedProgress
        val y = particle.y + particle.driftY * easedProgress + particle.gravity * localProgress * localProgress

        drawCircle(
            color = particle.color,
            radius = particle.radius * scale,
            center = Offset(x, y),
            alpha = fade
        )
        index += 1
    }
}

private data class DisintegrationBuildSpec(
    val particleSpacingPx: Int,
    val particleRadiusPx: Float,
    val maxParticles: Int,
    val alphaThreshold: Int,
    val driftDistancePx: Float,
    val gravityPx: Float,
    val spread: DisintegrationSpreadDirection,
    val seed: Int?
)

private data class DisintegrationParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val gravity: Float,
    val delay: Float,
    val activeDuration: Float,
    val startScale: Float,
    val endScale: Float
)

private fun ImageBitmap.extractDisintegrationParticles(
    spec: DisintegrationBuildSpec
): List<DisintegrationParticle> {
    val width = width
    val height = height
    val pixels = IntArray(width * height)
    readPixels(pixels)

    val random = Random(spec.seed ?: Random.Default.nextInt())
    val particles = ArrayList<DisintegrationParticle>(spec.maxParticles)
    val centerX = width / 2f
    val centerY = height / 2f
    var visibleCellCount = 0
    var y = 0

    while (y < height) {
        var x = 0
        val yEnd = minOf(y + spec.particleSpacingPx, height)

        while (x < width) {
            val xEnd = minOf(x + spec.particleSpacingPx, width)
            val sampledPixel = sampleVisiblePixel(
                pixels = pixels,
                imageWidth = width,
                xStart = x,
                xEnd = xEnd,
                yStart = y,
                yEnd = yEnd,
                alphaThreshold = spec.alphaThreshold
            )

            if (sampledPixel != null) {
                visibleCellCount += 1
                val particle = sampledPixel.toParticle(
                    imageWidth = width,
                    imageHeight = height,
                    centerX = centerX,
                    centerY = centerY,
                    random = random,
                    spec = spec
                )

                if (particles.size < spec.maxParticles) {
                    particles.add(particle)
                } else {
                    val replacementIndex = random.nextInt(visibleCellCount)
                    if (replacementIndex < spec.maxParticles) {
                        particles[replacementIndex] = particle
                    }
                }
            }

            x += spec.particleSpacingPx
        }

        y += spec.particleSpacingPx
    }

    return particles
}

private data class SampledPixel(
    val x: Float,
    val y: Float,
    val argb: Int
)

private fun sampleVisiblePixel(
    pixels: IntArray,
    imageWidth: Int,
    xStart: Int,
    xEnd: Int,
    yStart: Int,
    yEnd: Int,
    alphaThreshold: Int
): SampledPixel? {
    var alphaSum = 0
    var redSum = 0
    var greenSum = 0
    var blueSum = 0
    var xSum = 0
    var ySum = 0
    var count = 0
    var y = yStart

    while (y < yEnd) {
        var x = xStart
        val rowOffset = y * imageWidth

        while (x < xEnd) {
            val argb = pixels[rowOffset + x]
            val alpha = argb ushr 24 and 0xFF

            if (alpha > alphaThreshold) {
                alphaSum += alpha
                redSum += argb ushr 16 and 0xFF
                greenSum += argb ushr 8 and 0xFF
                blueSum += argb and 0xFF
                xSum += x
                ySum += y
                count += 1
            }

            x += 1
        }

        y += 1
    }

    if (count == 0) return null

    val alpha = alphaSum / count
    val red = redSum / count
    val green = greenSum / count
    val blue = blueSum / count
    val averagedColor = alpha shl 24 or (red shl 16) or (green shl 8) or blue

    return SampledPixel(
        x = xSum.toFloat() / count,
        y = ySum.toFloat() / count,
        argb = averagedColor
    )
}

private fun SampledPixel.toParticle(
    imageWidth: Int,
    imageHeight: Int,
    centerX: Float,
    centerY: Float,
    random: Random,
    spec: DisintegrationBuildSpec
): DisintegrationParticle {
    val angle = randomAngle(
        spread = spec.spread,
        x = x,
        y = y,
        centerX = centerX,
        centerY = centerY,
        random = random
    )
    val driftDistance = spec.driftDistancePx * randomFloat(random, 0.35f, 1.25f)
    val jitter = spec.driftDistancePx * 0.18f
    val driftX = cos(angle.toDouble()).toFloat() * driftDistance + randomFloat(random, -jitter, jitter)
    val driftY = sin(angle.toDouble()).toFloat() * driftDistance + randomFloat(random, -jitter * 0.6f, jitter)

    val delay = randomFloat(random, 0f, 0.22f)

    return DisintegrationParticle(
        x = x.coerceIn(0f, imageWidth.toFloat()),
        y = y.coerceIn(0f, imageHeight.toFloat()),
        color = Color(argb),
        radius = spec.particleRadiusPx * randomFloat(random, 0.7f, 1.45f),
        driftX = driftX,
        driftY = driftY,
        gravity = spec.gravityPx * randomFloat(random, 0.45f, 1.25f),
        delay = delay,
        activeDuration = randomFloat(random, 0.58f, 1f - delay),
        startScale = randomFloat(random, 0.85f, 1.2f),
        endScale = randomFloat(random, 0.16f, 0.64f)
    )
}

private fun randomAngle(
    spread: DisintegrationSpreadDirection,
    x: Float,
    y: Float,
    centerX: Float,
    centerY: Float,
    random: Random
): Float {
    val jitter = randomFloat(random, -0.7f, 0.7f)

    return when (spread) {
        DisintegrationSpreadDirection.Random -> randomFloat(random, 0f, TwoPi)
        DisintegrationSpreadDirection.Left -> PI.toFloat() + jitter
        DisintegrationSpreadDirection.Right -> jitter
        DisintegrationSpreadDirection.Up -> -HalfPi + jitter
        DisintegrationSpreadDirection.Down -> HalfPi + jitter
        DisintegrationSpreadDirection.CenterOut -> {
            val baseAngle = atan2(
                y = (y - centerY).toDouble(),
                x = (x - centerX).toDouble()
            ).toFloat()
            if (baseAngle.isNaN()) randomFloat(random, 0f, TwoPi) else baseAngle + jitter * 0.65f
        }
    }
}

private fun randomFloat(random: Random, start: Float, end: Float): Float {
    return start + (end - start) * random.nextFloat()
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private const val TwoPi = 6.2831855f
private const val HalfPi = 1.5707964f
