package edu.fullsail.anchor.engagement.badges

import android.content.IntentSender
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.modifier.modifierLocalOf
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box



// Going to try and make it so you can click over and over
data class Explosion(val id: Long, val position: Offset)

// Hold individual particles
data class Particle(
    val color: Color,
    val velocityX: Float,
    val velocityY: Float,
    val radius: Float,
    val initialAngle: Float
)

@Composable
fun ConfettiOverlay(
    explosions: List<Explosion>, // changing to a list
    onBurstFinished: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        explosions.forEach { explosion ->
            key(explosion.id) {
                ConfettiBurst(
                    origin = explosion.position,
                    onFinished = { onBurstFinished(explosion.id) }
                )
            }
        }
    }
}
@Composable
private fun ConfettiBurst(
    origin: Offset,
    onFinished: () -> Unit
) {
    // animation progress (0 to 1)
    val animatedProgress = remember { Animatable(0f) }

    // generate particles when trigger becomes true
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }

    // colors for confetti
    val colors = listOf(
        Color(0xFF2F9E97), // teal
        Color(0xFF00E5FF), // cyan
        Color(0xFF76FF03), // lime green
        Color(0xFF4C6B82), // lighter blue
        Color(0xFF6200EA) // purple
    )


    LaunchedEffect(Unit) {
            // 1) make particles random
            particles = List(50){
                val angle = Random.nextFloat() * 360f
                val speed = Random.nextFloat() * 20f + 10f
                Particle(
                    color = colors.random(),
                    velocityX = (cos(Math.toRadians(angle.toDouble())) * speed).toFloat(),
                    velocityY = (sin(Math.toRadians(angle.toDouble())) * speed).toFloat(),
                    radius = Random.nextFloat() * 8f + 4f,
                    initialAngle = Random.nextFloat() * 360f
                )
            }
            // 2) run the animation
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = LinearEasing)
            )

            // 3) cleanup after
            onFinished()

    }
    if (particles.isNotEmpty()){
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = animatedProgress.value

            particles.forEach { particle ->
                // physics calculations
                val gravity = 20.0f * progress * progress

                // calculated current position
                val currentX = origin.x + (particle.velocityX * progress * 50)
                val currentY = origin.y + (particle.velocityY * progress * 50) + (gravity * 1000)

                // adding a fade out
                val alpha = (1f - progress).coerceIn(0f, 1f)

                // this is for the particle animation
                withTransform({
                    rotate(
                        degrees = particle.initialAngle + (progress * 360),
                        pivot = Offset(currentX, currentY)
                    )
                }){
                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = particle.radius,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}