package com.sakthi.joksy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakthi.joksy.data.Joke
import com.sakthi.joksy.ui.theme.JoksyTheme
import org.koin.compose.koinInject
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }

        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: JoksyViewModel = koinInject()
    val joke by viewModel.jokeState.collectAsStateWithLifecycle()
    var cardStack by remember { mutableStateOf(listOf<JokeCard>()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.getRandomJoke()
    }

    // Generate card stack when joke changes
    LaunchedEffect(joke) {
        if (joke is JokeState.Success) {
            val currentJoke = (joke as JokeState.Success).joke
            val newCard = JokeCard(
                joke = currentJoke,
                color = getRandomCardColor(),
                id = System.currentTimeMillis()
            )
            cardStack = cardStack + newCard
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (joke) {
            is JokeState.Loading -> {
                CRTShimmerCard()
            }

            is JokeState.Success -> {
                CardStack(
                    cards = cardStack,
                    currentIndex = currentCardIndex,
                    onSwipe = { direction ->
                        currentCardIndex++
                        // Always fetch new joke when swiping
                        viewModel.getRandomJoke()
                    }
                )
            }

            is JokeState.Error -> {
                ErrorCard(message = (joke as JokeState.Error).message)
            }
        }

        Row(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstructionTag("👈 Swipe Left to Nope", Color.Red)

            InstructionTag("Swipe Right to Like 👉", Color(0xFF208620))
        }
    }
}

@Composable
fun InstructionTag(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}


@Composable
fun CardStack(
    cards: List<JokeCard>,
    currentIndex: Int,
    onSwipe: (SwipeDirection) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background cards (stack effect)
        for (i in (currentIndex + 2).coerceAtMost(cards.size - 1) downTo currentIndex) {
            if (i < cards.size) {
                val scale = 1f - (i - currentIndex) * 0.05f
                val offsetY = (i - currentIndex) * 8.dp
                val alpha = 1f - (i - currentIndex) * 0.2f

                SwipeableJokeCard(
                    card = cards[i],
                    scale = scale,
                    offsetY = offsetY,
                    alpha = alpha,
                    isInteractive = i == currentIndex,
                    onSwipe = onSwipe
                )
            }
        }
    }
}

@Composable
fun SwipeableJokeCard(
    card: JokeCard,
    scale: Float = 1f,
    offsetY: Dp = 0.dp,
    alpha: Float = 1f,
    isInteractive: Boolean = true,
    onSwipe: (SwipeDirection) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetYState by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(true) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 300),
        label = "offsetX"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )

    if (isVisible) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp + (3 - (scale * 3)).dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = card.color
            ),
            modifier = Modifier
                .size(320.dp, 450.dp)
                .offset(y = offsetY)
                .scale(scale)
                .alpha(alpha)
                .offset { IntOffset(animatedOffsetX.roundToInt(), offsetYState.roundToInt()) }
                .rotate(animatedRotation)
                .pointerInput(Unit) {
                    if (isInteractive) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > 300 -> {
                                        offsetX = 1200f
                                        rotation = 30f
                                        onSwipe(SwipeDirection.RIGHT)
                                        isVisible = false
                                    }
                                    offsetX < -300 -> {
                                        offsetX = -1200f
                                        rotation = -30f
                                        onSwipe(SwipeDirection.LEFT)
                                        isVisible = false
                                    }
                                    else -> {
                                        offsetX = 0f
                                        offsetYState = 0f
                                        rotation = 0f
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                offsetX += dragAmount.x
                                offsetYState += dragAmount.y * 0.5f
                                rotation = (offsetX / 20f).coerceIn(-15f, 15f)
                                change.consume()
                            }
                        )
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                card.color.copy(alpha = 0.9f),
                                card.color.copy(alpha = 0.7f)
                            ),
                            radius = 600f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Joke icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "😂",
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = card.joke.setup,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = card.joke.punchline,
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                // Swipe indicators
                if (isInteractive) {
                    SwipeIndicators(offsetX = offsetX)
                }
            }
        }
    }
}

@Composable
fun SwipeIndicators(offsetX: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Left indicator (Nope)
        if (offsetX < -50) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(32.dp)
                    .alpha((-offsetX / 300f).coerceIn(0f, 1f))
            ) {
                Text(
                    text = "NOPE",
                    color = Color.Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }

        // Right indicator (Like)
        if (offsetX > 50) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(32.dp)
                    .alpha((offsetX / 300f).coerceIn(0f, 1f))
            ) {
                Text(
                    text = "LIKE",
                    color = Color.Green,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun CRTShimmerCard() {
    val cardColor = getRandomCardColor()

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        modifier = Modifier.size(320.dp, 450.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cardColor.copy(alpha = 0.9f),
                            cardColor.copy(alpha = 0.7f)
                        ),
                        radius = 600f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Joke icon placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Setup text shimmer
                Column {
                    repeat(3) { index ->
                        ShimmerText(
                            width = when (index) {
                                0 -> 0.9f
                                1 -> 0.7f
                                else -> 0.8f
                            },
                            height = 20.dp
                        )
                        if (index < 2) Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Punchline text shimmer
                Column {
                    repeat(2) { index ->
                        ShimmerText(
                            width = when (index) {
                                0 -> 0.8f
                                else -> 0.6f
                            },
                            height = 18.dp
                        )
                        if (index < 1) Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerText(
    width: Float,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "translateAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim.value - 200f, 0f),
                    end = Offset(translateAnim.value, 0f)
                )
            )
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "translateAnim"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim.value - 100f, translateAnim.value - 100f),
                    end = Offset(translateAnim.value, translateAnim.value)
                )
            )
    )
}

@Composable
fun ErrorCard(message: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E1A1A)
        ),
        modifier = Modifier.size(320.dp, 450.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.Red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getRandomCardColor(): Color {
    val colors = listOf(
        Color(0xFF667EEA), // Blue
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFFFFE66D), // Yellow
        Color(0xFF95E1D3), // Mint
        Color(0xFFFF8B94), // Pink
        Color(0xFFB8860B), // Orange
        Color(0xFF9B59B6), // Purple
        Color(0xFF16A085), // Green
        Color(0xFFE67E22)  // Dark Orange
    )
    return colors.random()
}

enum class SwipeDirection {
    LEFT, RIGHT
}

data class JokeCard(
    val joke: Joke, // Your existing Joke data class
    val color: Color,
    val id: Long
)