package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EventEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MoodPulseEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    /**
     * Optimized B-tree indexed spatial query with bounding box and limit.
     * Avoids full-table sequential scans.
     */
    @Query("""
        SELECT * FROM groups 
        WHERE isActive = 1 
          AND latitude BETWEEN :minLat AND :maxLat 
          AND longitude BETWEEN :minLng AND :maxLng 
        ORDER BY lastActivityTimestamp DESC 
        LIMIT :limit
    """)
    fun getGroupsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int = 50
    ): Flow<List<GroupEntity>>

    @Query("""
        SELECT * FROM groups 
        WHERE isActive = 1 
          AND category = :category 
        ORDER BY memberCount DESC 
        LIMIT :limit
    """)
    fun getGroupsByCategory(category: String, limit: Int = 40): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY lastActivityTimestamp DESC LIMIT :limit")
    fun getAllActiveGroups(limit: Int = 60): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun getGroupCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("UPDATE groups SET memberCount = memberCount + 1 WHERE id = :groupId")
    suspend fun incrementMemberCount(groupId: String)
}

@Dao
interface EventDao {
    /**
     * Optimized index query on startTime and isActive
     */
    @Query("""
        SELECT * FROM events 
        WHERE isActive = 1 
          AND startTime >= :minTime 
        ORDER BY startTime ASC 
        LIMIT :limit
    """)
    fun getUpcomingEvents(minTime: Long, limit: Int = 40): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isActive = 1 
          AND eventType = :eventType 
        ORDER BY startTime ASC 
        LIMIT :limit
    """)
    fun getEventsByType(eventType: String, limit: Int = 30): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("UPDATE events SET registeredCount = registeredCount + 1 WHERE id = :eventId")
    suspend fun registerForEvent(eventId: String)
}

@Dao
interface MoodDao {
    /**
     * Optimized query filtering for active pulse matches with trust threshold
     */
    @Query("""
        SELECT * FROM mood_pulses 
        WHERE isActive = 1 
          AND moodType = :moodType 
          AND trustScore >= :minTrustScore 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getMatchingMoodPulses(moodType: String, minTrustScore: Int = 40, limit: Int = 20): Flow<List<MoodPulseEntity>>

    @Query("SELECT * FROM mood_pulses WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 30")
    fun getAllActivePulses(): Flow<List<MoodPulseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodPulse(pulse: MoodPulseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodPulses(pulses: List<MoodPulseEntity>)

    @Query("UPDATE mood_pulses SET isActive = 0 WHERE timestamp < :cutoffTime")
    suspend fun purgeExpiredPulses(cutoffTime: Long)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun getUserProfile(userId: String = "current_user"): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET trustScore = :newScore WHERE id = :userId")
    suspend fun updateTrustScore(userId: String = "current_user", newScore: Int)

    @Query("UPDATE user_profile SET currentMood = :mood WHERE id = :userId")
    suspend fun updateCurrentMood(userId: String = "current_user", mood: String?)
}
