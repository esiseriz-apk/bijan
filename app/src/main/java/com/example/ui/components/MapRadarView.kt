package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UiEvent
import com.example.data.model.UiGroup
import com.example.data.model.UiMoodPulse
import com.example.ui.theme.PinCompanionPurple
import com.example.ui.theme.PinEventOrange
import com.example.ui.theme.PinInstantGreen
import com.example.ui.theme.PinPersistentBlue
import com.example.ui.theme.PinPlannedYellow

/**
 * High performance hardware-accelerated Canvas Radar & Interactive Gathering Map.
 * Renders nearby active Gapoo pins using spatial colors specified in Section 13.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapRadarView(
    groups: List<UiGroup>,
    events: List<UiEvent>,
    pulses: List<UiMoodPulse>,
    onPinClicked: (title: String, subtitle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("radar_map_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2421) // Deep slate navy for high contrast radar
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رادار زنده گردهم‌آیی‌ها (شعاع ۳ کیلومتر)",
                        color = Color(0xFFE8F5E9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${groups.size + events.size + pulses.size} جمع فعال",
                    color = Color(0xFFA5D6A7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF141A17)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.height * 0.46f

                    // Range Rings
                    drawCircle(
                        color = Color(0xFF263238),
                        radius = maxRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0xFF263238),
                        radius = maxRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0xFF2E4038),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Animated Radar Pulse
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = (1f - pulseProgress) * 0.4f),
                        radius = maxRadius * pulseProgress,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // Center User Location Dot
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = 3.5f,
                        center = center
                    )

                    // Draw pins at distributed offsets
                    // Instant Pulse (Green)
                    pulses.take(4).forEachIndexed { i, p ->
                        val angle = (i * 75f) * (Math.PI / 180f)
                        val dist = (maxRadius * 0.4f) + (i * 12f)
                        val offset = Offset(
                            x = center.x + (dist * Math.cos(angle)).toFloat(),
                            y = center.y + (dist * Math.sin(angle)).toFloat()
                        )
                        drawCircle(color = PinInstantGreen, radius = 6.5f, center = offset)
                        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2.5f, center = offset)
                    }

                    // Persistent Groups (Blue)
                    groups.filter { it.entity.groupType == "PERSISTENT" }.take(3).forEachIndexed { i, g ->
                        val angle = (140f + i * 50f) * (Math.PI / 180f)
                        val dist = (maxRadius * 0.65f)
                        val offset = Offset(
                            x = center.x + (dist * Math.cos(angle)).toFloat(),
                            y = center.y + (dist * Math.sin(angle)).toFloat()
                        )
                        drawCircle(color = PinPersistentBlue, radius = 7f, center = offset)
                    }

                    // Planned Groups (Yellow)
                    groups.filter { it.entity.groupType == "PLANNED" }.take(2).forEachIndexed { i, g ->
                        val angle = (230f + i * 60f) * (Math.PI / 180f)
                        val dist = (maxRadius * 0.5f)
                        val offset = Offset(
                            x = center.x + (dist * Math.cos(angle)).toFloat(),
                            y = center.y + (dist * Math.sin(angle)).toFloat()
                        )
                        drawCircle(color = PinPlannedYellow, radius = 7f, center = offset)
                    }

                    // Events (Orange)
                    events.take(3).forEachIndexed { i, e ->
                        val angle = (300f + i * 40f) * (Math.PI / 180f)
                        val dist = (maxRadius * 0.85f)
                        val offset = Offset(
                            x = center.x + (dist * Math.cos(angle)).toFloat(),
                            y = center.y + (dist * Math.sin(angle)).toFloat()
                        )
                        drawCircle(color = PinEventOrange, radius = 7.5f, center = offset)
                    }
                }

                // Overlay interactive button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            onPinClicked("موقعیت شما", "خیابان آزادی، نزدیک به ۷ گردهم‌آیی و ۳ رویداد")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📍 موقعیت من: آزادی",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pin Legend (Spec Section 13)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendChip(color = PinInstantGreen, label = "نبض فوری")
                LegendChip(color = PinPlannedYellow, label = "برنامه‌ریزی‌شده")
                LegendChip(color = PinPersistentBlue, label = "گروه علاقه‌مندی")
                LegendChip(color = PinEventOrange, label = "رویداد / کنسرت")
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF26302B))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color(0xFFCFD8DC),
            fontSize = 9.5.sp
        )
    }
}
