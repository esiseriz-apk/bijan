package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Objects for Supabase REST API (PostgREST / PostgreSQL)
 */

@JsonClass(generateAdapter = true)
data class RemoteGroupDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "subCategory") val subCategory: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "locationName") val locationName: String = "",
    @Json(name = "latitude") val latitude: Double = 35.7219,
    @Json(name = "longitude") val longitude: Double = 51.3347,
    @Json(name = "memberCount") val memberCount: Int = 1,
    @Json(name = "maxCapacity") val maxCapacity: Int = 20,
    @Json(name = "scheduleType") val scheduleType: String = "هفتگی",
    @Json(name = "groupType") val groupType: String = "PERSISTENT",
    @Json(name = "isActive") val isActive: Boolean = true,
    @Json(name = "iconEmoji") val iconEmoji: String = "👥",
    @Json(name = "creatorName") val creatorName: String = "کاربر گپو",
    @Json(name = "creatorTrustScore") val creatorTrustScore: Int = 85,
    @Json(name = "rules") val rules: String = "احترام به اعضا در مکان عمومی"
)

@JsonClass(generateAdapter = true)
data class RemoteEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "eventType") val eventType: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "organizerName") val organizerName: String = "",
    @Json(name = "organizerTrustScore") val organizerTrustScore: Int = 90,
    @Json(name = "locationName") val locationName: String = "",
    @Json(name = "latitude") val latitude: Double = 35.7219,
    @Json(name = "longitude") val longitude: Double = 51.3347,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "capacity") val capacity: Int = 30,
    @Json(name = "registeredCount") val registeredCount: Int = 0,
    @Json(name = "startTime") val startTime: Long = System.currentTimeMillis(),
    @Json(name = "posterEmoji") val posterEmoji: String = "🎟️",
    @Json(name = "hasCompanionRequest") val hasCompanionRequest: Boolean = true,
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class RemoteMoodPulseDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "userName") val userName: String,
    @Json(name = "userAvatarEmoji") val userAvatarEmoji: String = "😊",
    @Json(name = "moodType") val moodType: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "statusMessage") val statusMessage: String = "",
    @Json(name = "trustScore") val trustScore: Int = 80,
    @Json(name = "expiresAt") val expiresAt: Long = System.currentTimeMillis() + 1800000L,
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "syncedItemsCount") val syncedItemsCount: Int = 0
)
