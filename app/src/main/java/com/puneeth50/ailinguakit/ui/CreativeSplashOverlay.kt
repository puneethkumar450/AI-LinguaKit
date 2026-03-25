package com.puneeth50.ailinguakit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import kotlinx.coroutines.delay

@Composable
fun CreativeSplashOverlay(
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    var entered by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "illustratedSplash")

    val contentOffsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 26f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splashContentOffset"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "splashContentAlpha"
    )
    val illustrationFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "illustrationFloat"
    )
    val chipFloat by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chipFloat"
    )
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val translationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 0
                0f at 350
                1f at 1500
                1f at 2150
                0f at 2600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "translationProgress"
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(1800)
        visible = false
        delay(260)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(260))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF6FAFF),
                            Color(0xFFFDFEFF)
                        )
                    )
                )
                .padding(horizontal = 28.dp, vertical = 42.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-30).dp, y = (-24).dp)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A5D9CFF))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 36.dp, y = 30.dp)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFC857))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = contentOffsetY.dp)
                    .alpha(contentAlpha)
                    .padding(top = 22.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SplashIllustration(
                    modifier = Modifier
                        .offset(y = illustrationFloat.dp)
                        .scale(haloScale),
                    chipOffset = chipFloat,
                    translationProgress = translationProgress
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = stringResource(id = R.string.app_name),
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF1D2735),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )

                Text(
                    text = stringResource(id = R.string.splash_tagline),
                    modifier = Modifier
                        .padding(top = 14.dp, start = 20.dp, end = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF3F556D),
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Text(
                    text = stringResource(id = R.string.splash_caption),
                    modifier = Modifier
                        .padding(top = 18.dp, start = 16.dp, end = 16.dp)
                        .width(320.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF6F8093),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun SplashIllustration(
    modifier: Modifier = Modifier,
    chipOffset: Float,
    translationProgress: Float
) {
    Box(
        modifier = modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(256.dp)
                .clip(CircleShape)
                .background(Color(0x14A79AF4))
        )

        AiConversationIllustration(
            modifier = Modifier
                .size(286.dp)
                .offset(y = 10.dp)
            ,
            translationProgress = translationProgress
        )
    }
}

@Composable
private fun AiConversationIllustration(
    modifier: Modifier = Modifier,
    translationProgress: Float
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(Color(0x14A59CF4))
        )

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp)
                .width(144.dp)
                .height(218.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF7667D8),
            shadowElevation = 5.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF4F5FB)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .width(52.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF6D61C9))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 44.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x20FFC857))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 18.dp, top = 12.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FF8A7A))
                        )
                    }
                }
                RobotInsidePhone(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        PersonAtSide(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-8).dp, y = 4.dp)
        )

        SourceSpeechBubble(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 22.dp, y = 22.dp),
            progress = translationProgress
        )
        TranslationCard(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = (-14).dp),
            progress = translationProgress
        )
    }
}

@Composable
private fun RobotInsidePhone(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 14.dp)
                .size(width = 54.dp, height = 58.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF80A7FF),
                            Color(0xFF5979E3)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 18.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA9BEFF),
                            Color(0xFF6984EA)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 28.dp)
                .size(width = 36.dp, height = 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-8).dp, y = 34.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF5CE1E6))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 8.dp, y = 34.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF5CE1E6))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .size(width = 5.dp, height = 10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6D61C9))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
                .size(width = 24.dp, height = 14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFF7E7E))
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 6.dp, y = 6.dp)
                .size(width = 16.dp, height = 5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6D61C9))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp, y = 6.dp)
                .size(width = 16.dp, height = 5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6D61C9))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 34.dp, y = (-4).dp)
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6D61C9))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-34).dp, y = (-4).dp)
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6D61C9))
        )
    }
}

@Composable
private fun PersonAtSide(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(width = 112.dp, height = 184.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-8).dp, y = 12.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFC1B4))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-10).dp, y = 8.dp)
                .size(width = 40.dp, height = 22.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 8.dp))
                .background(Color(0xFFC7484D))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-8).dp, y = 46.dp)
                .size(width = 46.dp, height = 58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF0627D))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-8).dp, y = (-30).dp)
                .size(width = 42.dp, height = 68.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Color(0xFF2F2A2F))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-18).dp, y = (-6).dp)
                .size(width = 12.dp, height = 40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFC1B4))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (0).dp, y = (-6).dp)
                .size(width = 12.dp, height = 40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFC1B4))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-20).dp, y = 14.dp)
                .size(width = 20.dp, height = 12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF7164D6))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (2).dp, y = 14.dp)
                .size(width = 20.dp, height = 12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF7164D6))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-10).dp, y = (-2).dp)
                .size(width = 10.dp, height = 32.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFC1B4))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 6.dp, y = 8.dp)
                .size(width = 12.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF5067B9))
        )
    }
}

@Composable
private fun SourceSpeechBubble(modifier: Modifier = Modifier) {
    SourceSpeechBubble(modifier = modifier, progress = 0f)
}

@Composable
private fun SourceSpeechBubble(
    modifier: Modifier = Modifier,
    progress: Float
) {
    val bubbleOffsetX = 58f * progress
    val bubbleOffsetY = 62f * progress
    val bubbleAlpha = 1f - (progress * 0.45f)
    Surface(
        modifier = modifier
            .offset(bubbleOffsetX.dp, bubbleOffsetY.dp)
            .alpha(bubbleAlpha),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Hello, how are you?",
                color = Color(0xFF2C3B4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .width((8 + index * 4).dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1DB6A3))
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationCard(
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    val cardOffsetX = 34f - (16f * progress)
    val cardOffsetY = 22f - (16f * progress)
    val barColor = lerp(Color(0xFFDDE6F5), Color(0xFF5D97FF), progress)
    Surface(
        modifier = modifier
            .offset(cardOffsetX.dp, cardOffsetY.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Translation",
                color = Color(0xFF90A0B2),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Hola, como estas?",
                color = Color(0xFF1D2735),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(lerp(Color(0xFFDDE6F5), Color(0xFF1F78FF), progress))
                )
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
private fun VoiceWave(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(10, 18, 26, 16).forEach { height ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1DB6A3),
                                Color(0xFF1F78FF)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun LanguageChip(
    text: String,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = accent,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
@Preview(showBackground = true)
@Composable
fun CreativeSplashOverlayPreview() {
    TranslateAppTheme {
        CreativeSplashOverlay(onFinished = {})
    }
}
