package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GroupEntity
import com.example.data.model.MoodPulseEntity
import com.example.data.model.UiEvent
import com.example.data.model.UiGroup
import com.example.data.model.UiMoodPulse
import com.example.data.model.UserProfileEntity
import com.example.data.repository.GapooRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    HOME,
    GROUPS,
    INSTANT_PULSE,
    EVENTS,
    PROFILE
}

sealed interface PulseState {
    data object Idle : PulseState
    data class Scanning(val moodType: String) : PulseState
    data class Matched(
        val companion: UiMoodPulse,
        val proposedVenue: String = "کافه کتاب ترنجستان (مکان عمومی تأییدشده)",
        val icebreakerPrompt: String = "از همراهت بپرس: آخرین کتاب یا فیلم جذابی که دیدی چی بود؟"
    ) : PulseState
    data class Confirmed(val companion: UiMoodPulse, val meetingTime: String = "۱۵ دقیقه دیگر") : PulseState
}

data class GapooUiState(
    val currentDestination: AppDestination = AppDestination.HOME,
    val selectedCategory: String = "همه",
    val searchQuery: String = "",
    val pulseState: PulseState = PulseState.Idle,
    val selectedMoodType: String = "chat",
    val safetyAccepted: Boolean = false,
    val joinedGroupIds: Set<String> = emptySet(),
    val registeredEventIds: Set<String> = emptySet(),
    val activeSnackbarMessage: String? = null,
    val isSyncingCloud: Boolean = false,
    val selectedGroupDetails: UiGroup? = null,
    val selectedEventDetails: UiEvent? = null
)

class GapooViewModel(
    private val repository: GapooRepository
) : ViewModel() {

    private val userLat = 35.7219
    private val userLng = 51.3347

    private val _uiState = MutableStateFlow(GapooUiState())
    val uiState: StateFlow<GapooUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfileFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allGroups: StateFlow<List<UiGroup>> = repository.getNearbyGroupsFlow(userLat, userLng, radiusKm = 10.0)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingEvents: StateFlow<List<UiEvent>> = repository.getUpcomingEventsFlow(userLat, userLng)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeMoodPulses: StateFlow<List<UiMoodPulse>> = repository.getAllActivePulsesFlow(userLat, userLng)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Highly optimized combined flow: filters groups without recomposition stutter
     */
    val filteredGroups: StateFlow<List<UiGroup>> = combine(
        allGroups,
        _uiState
    ) { groups, state ->
        val query = state.searchQuery.trim().lowercase()
        val category = state.selectedCategory

        groups.filter { group ->
            val matchesCategory = (category == "همه" || group.entity.category == category)
            val matchesQuery = query.isEmpty() ||
                    group.entity.name.lowercase().contains(query) ||
                    group.entity.description.lowercase().contains(query) ||
                    group.entity.subCategory.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectMood(moodType: String) {
        _uiState.update { it.copy(selectedMoodType = moodType) }
    }

    fun acceptSafetyRules() {
        _uiState.update { it.copy(safetyAccepted = true) }
    }

    fun startInstantPulseSearch() {
        val currentMood = _uiState.value.selectedMoodType
        _uiState.update { it.copy(pulseState = PulseState.Scanning(currentMood)) }

        viewModelScope.launch {
            // Post current user mood pulse to local db
            val pulse = MoodPulseEntity(
                id = "pulse_${System.currentTimeMillis()}",
                userId = "current_user",
                userName = "سامان نوری",
                moodType = currentMood,
                statusMessage = getMoodTitle(currentMood),
                latitude = userLat,
                longitude = userLng,
                trustScore = 85
            )
            repository.postMoodPulse(pulse)

            // Simulate geolocation pulse search in 1.5s
            delay(1500)

            // Find closest candidate
            val candidates = activeMoodPulses.value.filter {
                it.entity.userId != "current_user" &&
                (it.entity.moodType == currentMood || it.entity.trustScore >= 80)
            }
            val match = candidates.firstOrNull() ?: UiMoodPulse(
                entity = MoodPulseEntity(
                    id = "match_fallback",
                    userId = "usr_match",
                    userName = "روشنک (همراه نزدیک)",
                    moodType = currentMood,
                    statusMessage = "مشتاق گپ زدن در کافه هستم",
                    latitude = 35.7230,
                    longitude = 51.3360,
                    trustScore = 88
                ),
                distanceKm = 0.4
            )

            val icebreaker = generateHamdamIcebreaker(currentMood)
            _uiState.update {
                it.copy(
                    pulseState = PulseState.Matched(
                        companion = match,
                        proposedVenue = "کافه ترنجستان (مکان عمومی تأییدشده)",
                        icebreakerPrompt = icebreaker
                    )
                )
            }
        }
    }

    fun confirmMeetup() {
        val state = _uiState.value.pulseState
        if (state is PulseState.Matched) {
            _uiState.update {
                it.copy(
                    pulseState = PulseState.Confirmed(state.companion),
                    activeSnackbarMessage = "ملاقات ۳۰ دقیقه‌ای با ${state.companion.entity.userName} هماهنگ شد!"
                )
            }
        }
    }

    fun cancelPulse() {
        _uiState.update { it.copy(pulseState = PulseState.Idle) }
    }

    fun joinGroup(groupId: String) {
        viewModelScope.launch {
            repository.joinGroup(groupId)
            _uiState.update {
                it.copy(
                    joinedGroupIds = it.joinedGroupIds + groupId,
                    activeSnackbarMessage = "به گروه اضافه شدید! به جمع دوستان خوش آمدید."
                )
            }
        }
    }

    fun registerEvent(eventId: String) {
        viewModelScope.launch {
            repository.registerEvent(eventId)
            _uiState.update {
                it.copy(
                    registeredEventIds = it.registeredEventIds + eventId,
                    activeSnackbarMessage = "ثبت‌نام در رویداد با موفقیت انجام شد."
                )
            }
        }
    }

    fun showGroupDetails(group: UiGroup?) {
        _uiState.update { it.copy(selectedGroupDetails = group) }
    }

    fun showEventDetails(event: UiEvent?) {
        _uiState.update { it.copy(selectedEventDetails = event) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(activeSnackbarMessage = null) }
    }

    /**
     * Trigger synchronization with free cloud database (Supabase / REST)
     */
    fun syncWithCloud(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCloud = true) }
            val result = repository.syncWithFreeCloud(context)
            _uiState.update {
                it.copy(
                    isSyncingCloud = false,
                    activeSnackbarMessage = if (result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        if (count > 0) "همگام‌سازی ابری موفق: $count مورد جدید دریافت شد ✓"
                        else "دیتابیس ابری همگام است ✓"
                    } else {
                        "اتصال محلی فعال است (دیتابیس آفلاین آماده به کار)"
                    }
                )
            }
        }
    }

    private fun getMoodTitle(code: String): String = when (code) {
        "chat" -> "دلم گپ می‌خواد 🗣️"
        "walk" -> "دلم پیاده‌روی می‌خواد 🚶"
        "tea" -> "دلم چای و همصحبت می‌خواد ☕"
        "book" -> "دلم کتاب‌خوانی می‌خواد 📚"
        "group_activity" -> "دلم کار گروهی می‌خواد 🎯"
        else -> "همصحبتی صمیمانه"
    }

    private fun generateHamdamIcebreaker(mood: String): String = when (mood) {
        "tea" -> "همدم: «از همراهت بپرس: لذت‌بخش‌ترین کافه‌ای که رفتی کجا بوده؟»"
        "book" -> "همدم: «از همراهت بپرس: کدوم کتاب بیشترین تأثیر رو روی نگاهت به زندگی گذاشته؟»"
        "walk" -> "همدم: «از همراهت بپرس: مسیر پیاده‌روی مورد علاقه‌ات توی شهر کجاست؟»"
        "group_activity" -> "همدم: «از همراهت بپرس: اهل چه جور بازی‌های فکری یا گروهی هستی؟»"
        else -> "همدم: «از همراهت بپرس: اگه می‌تونستی به گذشته برگردی، چه مهارتی رو زودتر یاد می‌گرفتی؟»"
    }
}
