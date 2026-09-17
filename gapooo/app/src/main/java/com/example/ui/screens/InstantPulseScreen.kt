package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SafetyAgreementDialog
import com.example.ui.theme.GapooAmberTertiary
import com.example.ui.theme.GapooOrangePrimary
import com.example.ui.theme.GapooTealSecondary
import com.example.ui.viewmodel.GapooViewModel
import com.example.ui.viewmodel.PulseState

data class MoodOption(
    val code: String,
    val title: String,
    val emoji: String,
    val subtitle: String
)

@Composable
fun InstantPulseScreen(
    viewModel: GapooViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSafetyDialog by remember { mutableStateOf(false) }
    var silentModeEnabled by remember { mutableStateOf(false) }

    val moods = listOf(
        MoodOption("chat", "دلم گپ می‌خواد", "🗣️", "همصحبتی صمیمی پیرامون روزمرگی یا هنر"),
        MoodOption("tea", "دلم چای و همصحبت می‌خواد", "☕", "یک فنجان چای در کافه نزدیک و گپ کوتاه"),
        MoodOption("walk", "دلم پیاده‌روی می‌خواد", "🚶", "قدم زدن نیم‌ساعته در بوستان یا خیابان"),
        MoodOption("book", "دلم کتاب‌خوانی می‌خواد", "📚", "سکوت مشترک در کتابخانه یا گفتگو درباره کتاب"),
        MoodOption("group_activity", "دلم کار گروهی می‌خواد", "🎯", "بازی رومیزی دونفره یا فعالیت تیمی سبک")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "همین الان چی حالته؟ (نبض فوری)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "پیدا کردن جمع لحظه‌ای یا یک همراه نزدیک بر اساس حال دل شما در شعاع ۱ کیلومتر",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val pulse = uiState.pulseState) {
            is PulseState.Idle -> {
                // Mood Selection Grid
                Text(
                    text = "۱. حال لحظه‌ای خودت را انتخاب کن:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEach { mood ->
                        val isSelected = uiState.selectedMoodType == mood.code
                        MoodCardItem(
                            mood = mood,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectMood(mood.code) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Silent Mode Switch (Spec Section 9: Invisible Mode)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = GapooTealSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "حالت خاموش (شنونده بدون استرس)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "«من می‌آم، اما ممکن است کم حرف بزنم»",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = silentModeEnabled,
                            onCheckedChange = { silentModeEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GapooOrangePrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons (Spec Section 4: 2 options)
                Button(
                    onClick = {
                        if (!uiState.safetyAccepted) {
                            showSafetyDialog = true
                        } else {
                            viewModel.startInstantPulseSearch()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_pulse_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GapooOrangePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "⚡ پیدا کردن همراه / جمع فوری (شعاع ۱ کیلومتر)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (!uiState.safetyAccepted) {
                            showSafetyDialog = true
                        } else {
                            viewModel.startInstantPulseSearch()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, GapooTealSecondary)
                ) {
                    Text(
                        text = "☕ همراه شدن با یک نفر (تک‌به‌تک)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GapooTealSecondary
                    )
                }
            }

            is PulseState.Scanning -> {
                ScanningView(
                    moodTitle = moods.find { it.code == pulse.moodType }?.title ?: "همصحبتی",
                    onCancel = { viewModel.cancelPulse() }
                )
            }

            is PulseState.Matched -> {
                MatchedCompanionView(
                    companion = pulse.companion,
                    venue = pulse.proposedVenue,
                    icebreaker = pulse.icebreakerPrompt,
                    isSilentMode = silentModeEnabled,
                    onConfirm = { viewModel.confirmMeetup() },
                    onCancel = { viewModel.cancelPulse() }
                )
            }

            is PulseState.Confirmed -> {
                ConfirmedMeetupView(
                    companion = pulse.companion,
                    meetingTime = pulse.meetingTime,
                    onReset = { viewModel.cancelPulse() }
                )
            }
        }
    }

    if (showSafetyDialog) {
        SafetyAgreementDialog(
            onDismiss = { showSafetyDialog = false },
            onAccept = {
                viewModel.acceptSafetyRules()
                showSafetyDialog = false
                viewModel.startInstantPulseSearch()
            }
        )
    }
}

@Composable
private fun MoodCardItem(
    mood: MoodOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("mood_${mood.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, GapooOrangePrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = mood.emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mood.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mood.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanningView(
    moodTitle: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseScan")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(GapooOrangePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(GapooOrangePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📡", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "در حال پایش نبض‌های نزدیک (شعاع ۱ کیلومتر)...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "جستجوی کاربران هم‌حال با «$moodTitle» با امتیاز اعتماد بالا",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("لغو جستجو", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MatchedCompanionView(
    companion: com.example.data.model.UiMoodPulse,
    venue: String,
    icebreaker: String,
    isSilentMode: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(GapooOrangePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🤝", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "همراه پیشنهادی پیدا شد!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GapooOrangePrimary
                    )
                    Text(
                        text = "فاصله: ${companion.distanceKm} کیلومتر از شما",
                        fontSize = 11.5.sp,
                        color = GapooTealSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Companion Profile Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "👤", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = companion.entity.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "پیام: ${companion.entity.statusMessage}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GapooAmberTertiary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${companion.entity.trustScore}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GapooAmberTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proposed Public Venue (Spec Section 14)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = GapooTealSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "مکان عمومی پیشنهادی Gapoo (امن و تاییدشده):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GapooTealSecondary
                    )
                    Text(
                        text = venue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Icebreaker suggestion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3E0))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GapooAmberTertiary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = icebreaker,
                        fontSize = 11.5.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 16.sp
                    )
                }
            }

            if (isSilentMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ حالت خاموش فعال است (همراه شما مطلع است که شنونده هستید)",
                    fontSize = 10.5.sp,
                    color = GapooTealSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Confirm & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("انصراف", fontSize = 12.5.sp)
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GapooOrangePrimary),
                    modifier = Modifier.weight(1.5f).testTag("confirm_meetup_button")
                ) {
                    Text("تأیید قرار (۳۰ دقیقه)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConfirmedMeetupView(
    companion: com.example.data.model.UiMoodPulse,
    meetingTime: String,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "قرار ملاقات تأیید شد!",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "زمان دیدار: $meetingTime در کافه ترنجستان با ${companion.entity.userName}",
                fontSize = 12.5.sp,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "اطلاعات دیدار و موقعیت امن برای «دوست امن» شما ارسال شد.",
                fontSize = 11.sp,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بازگشت به نبض فوری", color = Color.White)
            }
        }
    }
}
