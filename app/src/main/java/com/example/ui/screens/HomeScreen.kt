package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiMoodPulse
import com.example.ui.components.MapRadarView
import com.example.ui.theme.GapooAmberTertiary
import com.example.ui.theme.GapooOrangePrimary
import com.example.ui.theme.GapooTealSecondary
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.GapooViewModel

@Composable
fun HomeScreen(
    viewModel: GapooViewModel,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val events by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val pulses by viewModel.activeMoodPulses.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top App Identity & Trust Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "گَپو • Gapoo",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = GapooOrangePrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GapooTealSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "نسخه آزمایشی",
                            fontSize = 9.5.sp,
                            color = GapooTealSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "«همین الان، همین نزدیکی، یه جمع دوستانه...»",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Trust Score Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF3E0))
                    .clickable { onNavigate(AppDestination.PROFILE) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "امتیاز اعتماد",
                    tint = GapooAmberTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${userProfile?.trustScore ?: 85}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GapooAmberTertiary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "اعتماد",
                    fontSize = 10.sp,
                    color = Color(0xFF8D6E63)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Radar Map View
        MapRadarView(
            groups = groups,
            events = events,
            pulses = pulses,
            onPinClicked = { title, subtitle ->
                // Quick info notification
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Three Main User Pathways (Strict Spec Section 4)
        Text(
            text = "مسیرهای حضور در جمع",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Pathway 1: Social Groups
        PathwayHeroCard(
            title = "گروه‌های نزدیک من",
            subtitle = "کشف جمع‌های دوستانه کتاب، عکاسی، برنامه‌نویسی و ورزش بر اساس سلیقه شما",
            badgeText = "${groups.size} گروه فعال اطراف",
            icon = Icons.Default.Groups,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF00695C), Color(0xFF00897B))),
            testTag = "pathway_groups_button",
            onClick = { onNavigate(AppDestination.GROUPS) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pathway 2: Instant Pulse (Mood-based)
        PathwayHeroCard(
            title = "همین الان چی حالته؟ (نبض فوری)",
            subtitle = "حالت رو بگو، در شعاع ۱ کیلومتر همراه یا جمع فوری پیدا کن (۳۰ دقیقه، کافه امن)",
            badgeText = "🟢 تطبیق لحظه‌ای",
            icon = Icons.Default.LocalCafe,
            gradient = Brush.horizontalGradient(listOf(Color(0xFFD35400), Color(0xFFE67E22))),
            testTag = "pathway_instant_pulse_button",
            onClick = { onNavigate(AppDestination.INSTANT_PULSE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pathway 3: Nearby Events
        PathwayHeroCard(
            title = "رویدادهای نزدیک من",
            subtitle = "کنسرت، نمایش فیلم، کارگاه‌های مهارتی، تئاتر و دورهمی‌های مناسبتی",
            badgeText = "${events.size} رویداد پیش‌رو",
            icon = Icons.Default.Event,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))),
            testTag = "pathway_events_button",
            onClick = { onNavigate(AppDestination.EVENTS) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Mood Pulses Marquee
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نبض‌های فعال در نزدیکی شما",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "مشاهده همه",
                fontSize = 11.5.sp,
                color = GapooOrangePrimary,
                modifier = Modifier.clickable { onNavigate(AppDestination.INSTANT_PULSE) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pulses, key = { it.entity.id }) { pulse ->
                PulseQuickCard(pulse = pulse, onClick = { onNavigate(AppDestination.INSTANT_PULSE) })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Safety Tip Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GapooTealSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "نکته امنیتی Gapoo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اولین دیدار همیشه در مکان عمومی منتخب، حداکثر ۳۰ دقیقه و همراه با امکان خروج بی‌صداست.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PathwayHeroCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    gradient: Brush,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PulseQuickCard(
    pulse: UiMoodPulse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pulse.entity.userName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${pulse.distanceKm} km",
                    fontSize = 10.sp,
                    color = GapooTealSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pulse.entity.statusMessage,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GapooAmberTertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "اعتماد: ${pulse.entity.trustScore}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
