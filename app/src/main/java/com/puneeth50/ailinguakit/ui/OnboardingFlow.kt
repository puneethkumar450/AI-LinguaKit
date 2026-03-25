package com.puneeth50.ailinguakit.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puneeth50.ailinguakit.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

@Composable
fun OnboardingFlow(
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
        OnboardingPage(R.string.onboarding_languages_title, R.string.onboarding_languages_body),
        OnboardingPage(R.string.onboarding_features_title, R.string.onboarding_features_body)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> WelcomeIllustration()
                        1 -> LanguageChoiceIllustration()
                        else -> FeaturesIllustration()
                    }

                    Text(
                        text = stringResource(item.titleRes),
                        modifier = Modifier.padding(top = 28.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF202733),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(item.bodyRes),
                        modifier = Modifier.padding(top = 16.dp, start = 10.dp, end = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF667384),
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index == pagerState.currentPage) 28.dp else 10.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index == pagerState.currentPage) Color(0xFF1F78FF)
                            else Color(0xFFD7E2EE)
                        )
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F78FF)
            )
        ) {
            Text(
                text = stringResource(
                    if (pagerState.currentPage == pages.lastIndex) {
                        R.string.onboarding_get_started
                    } else {
                        R.string.onboarding_next
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WelcomeIllustration() {
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-72).dp, y = 48.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB6A3)),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color.White, fontSize = 18.sp)
        }

        Surface(
            shape = RoundedCornerShape(34.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(150.dp)
                .height(236.dp)
                .border(2.dp, Color(0xFF242B36), RoundedCornerShape(34.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF222831))
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD9E3F3))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFEFF4FA))
                        )
                    }
                }

                GlobeMini(
                    modifier = Modifier
                        .size(102.dp)
                        .padding(top = 16.dp)
                )

                Row(
                    modifier = Modifier.offset(y = (-18).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniChip("HELLO", Color(0xFFFFC533), Color(0xFF916100))
                    MiniChip("起", Color(0xFFFF6B6B), Color.White)
                }

                Surface(
                    modifier = Modifier.padding(top = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6DEE8))
                ) {
                    Text(
                        text = "TRANSLATE",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = Color(0xFFE06A6A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageChoiceIllustration() {
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        GlobeStand(modifier = Modifier.size(180.dp))
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 72.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search",
                    color = Color(0xFFB6C0CB),
                    fontSize = 15.sp,
                    modifier = Modifier.width(94.dp)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F78FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        MiniChip("SALUT", Color.White, Color(0xFF3C91E6), Modifier.align(Alignment.TopStart).offset(x = 38.dp, y = 34.dp))
        MiniChip("CIAO", Color.White, Color(0xFFE06A6A), Modifier.align(Alignment.TopEnd).offset(x = (-42).dp, y = 24.dp))
        MiniChip("BONJOUR", Color.White, Color(0xFFD0A73B), Modifier.align(Alignment.BottomEnd).offset(x = (-26).dp, y = (-38).dp))
        MiniChip("न", Color.White, Color(0xFFE06A6A), Modifier.align(Alignment.BottomStart).offset(x = 54.dp, y = (-30).dp))
    }
}

@Composable
private fun FeaturesIllustration() {
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(206.dp)
                .height(188.dp)
                .border(2.dp, Color(0xFF242B36), RoundedCornerShape(30.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(Color(0xFFFF6B6B), Color(0xFFFFC533), Color(0xFF39C46A)).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                SpeakingPersonIllustration(modifier = Modifier.align(Alignment.Center))
            }
        }

        MiniChip("CIAO", Color.White, Color(0xFF2F5D95), Modifier.align(Alignment.TopStart).offset(x = 36.dp, y = 32.dp))
        MiniChip("AI", Color.White, Color(0xFF9E59D1), Modifier.align(Alignment.CenterEnd).offset(x = (-6).dp, y = 0.dp))
        MiniChip("अ", Color.White, Color(0xFFE15B5B), Modifier.align(Alignment.BottomEnd).offset(x = (-22).dp, y = (-10).dp))
    }
}

@Composable
private fun GlobeMini(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(color = Color.White)
        drawCircle(color = Color(0xFF1F78FF), radius = size.minDimension * 0.42f)
        drawArc(color = Color.White, startAngle = 18f, sweepAngle = 100f, useCenter = true)
        drawArc(color = Color.White, startAngle = 182f, sweepAngle = 78f, useCenter = true)
    }
}

@Composable
private fun GlobeStand(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawCircle(color = Color(0xFFD9E0F1), radius = size.minDimension * 0.42f)
            drawCircle(color = Color(0xFF30363F), radius = size.minDimension * 0.42f, style = Stroke(width = stroke))
            drawLine(
                color = Color(0xFF30363F),
                start = center.copy(y = size.height * 0.82f),
                end = center.copy(y = size.height * 1.02f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF30363F),
                start = androidx.compose.ui.geometry.Offset(center.x - 26.dp.toPx(), size.height * 1.02f),
                end = androidx.compose.ui.geometry.Offset(center.x + 26.dp.toPx(), size.height * 1.02f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawArc(color = Color.White, startAngle = 35f, sweepAngle = 90f, useCenter = true)
            drawArc(color = Color.White, startAngle = 185f, sweepAngle = 74f, useCenter = true)
        }
    }
}

@Composable
private fun SpeakingPersonIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 18.dp)
                .size(width = 74.dp, height = 62.dp)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(Color(0xFF1F78FF))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 28.dp)
                .size(width = 90.dp, height = 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFD9E3F3))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-8).dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFDFC4))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-2).dp, y = (-26).dp)
                .size(width = 66.dp, height = 40.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 20.dp, bottomEnd = 18.dp))
                .background(Color(0xFF2C3440))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-9).dp)
                .size(width = 8.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE18A78))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-14).dp, y = (-12).dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D3440))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 14.dp, y = (-12).dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D3440))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp, y = (-6).dp)
                .size(width = 8.dp, height = 22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFC533))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp, y = (-6).dp)
                .size(width = 8.dp, height = 22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFC533))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 18.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB6A3)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-26).dp, y = 42.dp)
                .size(42.dp)
        ) {
            drawArc(
                color = Color(0xFF1DB6A3),
                startAngle = -28f,
                sweepAngle = 56f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0x801DB6A3),
                startAngle = -34f,
                sweepAngle = 68f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun MiniChip(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = background,
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
