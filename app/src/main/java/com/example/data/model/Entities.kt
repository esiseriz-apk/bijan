package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Optimized Room entity for Social Interest Groups with B-Tree indices
 * for fast filtering by category, groupType, and 2D spatial bounding box.
 */
@Entity(
    tableName = "groups",
    indices = [
        Index(value = ["category", "isActive"]),
        Index(value = ["groupType", "isActive"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["name"])
    ]
)
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,          // علمی، هنری، فرهنگی، ورزشی، سرگرمی، سلامت
    val subCategory: String,       // عکاسی، کتاب‌خوانی، پیاده‌روی، و غیره
    val description: String,
    val locationName: String,      // کافه، پارک، کتابخانه
    val latitude: Double,
    val longitude: Double,
    val memberCount: Int,
    val maxCapacity: Int,
    val scheduleType: String,      // هفتگی، دوهفتگی، ماهانه، فوری
    val groupType: String,         // INSTANT, PLANNED, PERSISTENT
    val isActive: Boolean = true,
    val iconEmoji: String = "👥",
    val creatorName: String = "کاربر گپو",
    val creatorTrustScore: Int = 85,
    val rules: String = "احترام به اعضا، گفتگوی دوستانه در مکان عمومی",
    val lastActivityTimestamp: Long = System.currentTimeMillis()
)

/**
 * Optimized Room entity for Events with indices on eventType, startTime, and location.
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["eventType", "isActive"]),
        Index(value = ["startTime", "isActive"]),
        Index(value = ["latitude", "longitude"])
    ]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val eventType: String,         // کنسرت، پاتی، نمایش فیلم، جشن، تئاتر، استندآپ، کارگاه
    val description: String,
    val organizerName: String,
    val organizerTrustScore: Int = 90,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startTime: Long,
    val price: Double = 0.0,       // 0 for free
    val capacity: Int = 30,
    val registeredCount: Int = 12,
    val isActive: Boolean = true,
    val posterEmoji: String = "🎭",
    val hasCompanionRequest: Boolean = false
)

/**
 * Optimized Room entity for real-time mood pulses with index on active mood type.
 */
@Entity(
    tableName = "mood_pulses",
    indices = [
        Index(value = ["moodType", "isActive"]),
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class MoodPulseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val moodType: String,          // chat, walk, tea, book, group_activity
    val statusMessage: String,
    val latitude: Double,
    val longitude: Double,
    val trustScore: Int = 75,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User profile cached locally with trust score and verified state.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_user",
    val name: String = "سامان نوری",
    val phone: String = "۰۹۱۲۳۴۵۶۷۸۹",
    val avatarEmoji: String = "🌟",
    val trustScore: Int = 85,
    val isVerified: Boolean = true,
    val selectedInterests: String = "هنری,فرهنگی,سرگرمی",
    val currentMood: String? = null,
    val latitude: Double = 35.7219,
    val longitude: Double = 51.3347,
    val trustedContactName: String = "دوست امن (سارا)",
    val trustedContactPhone: String = "۰۹۱۹۸۷۶۵۴۳۲"
)

/**
 * Immutable UI Domain Models with precalculated distance to eliminate runtime compute overhead
 */
@Immutable
data class UiGroup(
    val entity: GroupEntity,
    val distanceKm: Double,
    val isJoined: Boolean = false
)

@Immutable
data class UiEvent(
    val entity: EventEntity,
    val distanceKm: Double,
    val isRegistered: Boolean = false
)

@Immutable
data class UiMoodPulse(
    val entity: MoodPulseEntity,
    val distanceKm: Double
)
