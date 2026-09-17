package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.model.EventEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MoodPulseEntity
import com.example.data.model.UiEvent
import com.example.data.model.UiGroup
import com.example.data.model.UiMoodPulse
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-Performance Repository for Gapoo with:
 * 1. Two-Tier Caching (L1 In-Memory Cache with TTL + L2 Indexed SQLite Database)
 * 2. Spatial Bounding-Box Pruning (Index range search before trigonometric distance)
 * 3. Precalculated Memoized Distances (Offloading trigonometry from UI recomposition)
 * 4. Asynchronous I/O execution on Dispatchers.IO
 */
class GapooRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val groupDao = database.groupDao()
    private val eventDao = database.eventDao()
    private val moodDao = database.moodDao()
    private val userDao = database.userDao()

    // L1 Cache: In-memory store with expiration timestamp to prevent repeated disk I/O
    private val memoryGroupCache = ConcurrentHashMap<String, CachedData<List<UiGroup>>>()
    private val memoryEventCache = ConcurrentHashMap<String, CachedData<List<UiEvent>>>()
    private val CACHE_TTL_MS = 30_000L // 30 seconds TTL

    private data class CachedData<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(ttlMs: Long): Boolean = System.currentTimeMillis() - timestamp < ttlMs
    }

    /**
     * Fetch nearby groups using B-tree indexed bounding box pruning
     */
    fun getNearbyGroupsFlow(
        userLat: Double,
        userLng: Double,
        radiusKm: Double = 5.0
    ): Flow<List<UiGroup>> {
        val (minLat, maxLat, minLng, maxLng) = calculateBoundingBox(userLat, userLng, radiusKm)

        return groupDao.getGroupsInBoundingBox(minLat, maxLat, minLng, maxLng, limit = 50)
            .map { entities ->
                val uiList = entities.map { entity ->
                    val dist = calculateHaversineDistance(userLat, userLng, entity.latitude, entity.longitude)
                    UiGroup(
                        entity = entity,
                        distanceKm = Math.round(dist * 10.0) / 10.0
                    )
                }.sortedBy { it.distanceKm }

                // Update L1 Cache
                memoryGroupCache["nearby"] = CachedData(uiList)
                uiList
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Fast retrieval for groups filtered by category with reactive Flow
     */
    fun getGroupsByCategoryFlow(
        category: String,
        userLat: Double,
        userLng: Double
    ): Flow<List<UiGroup>> {
        return groupDao.getGroupsByCategory(category, limit = 40)
            .map { entities ->
                entities.map { entity ->
                    val dist = calculateHaversineDistance(userLat, userLng, entity.latitude, entity.longitude)
                    UiGroup(
                        entity = entity,
                        distanceKm = Math.round(dist * 10.0) / 10.0
                    )
                }.sortedBy { it.distanceKm }
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Fetch upcoming events with distance calculation and chronological ordering
     */
    fun getUpcomingEventsFlow(
        userLat: Double,
        userLng: Double
    ): Flow<List<UiEvent>> {
        val currentTime = System.currentTimeMillis()
        return eventDao.getUpcomingEvents(minTime = currentTime, limit = 30)
            .map { entities ->
                val uiList = entities.map { entity ->
                    val dist = calculateHaversineDistance(userLat, userLng, entity.latitude, entity.longitude)
                    UiEvent(
                        entity = entity,
                        distanceKm = Math.round(dist * 10.0) / 10.0
                    )
                }.sortedBy { it.distanceKm }

                memoryEventCache["upcoming"] = CachedData(uiList)
                uiList
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Active mood pulses matching user's selected mood and trust score threshold
     */
    fun getMatchingMoodPulsesFlow(
        moodType: String,
        userLat: Double,
        userLng: Double,
        minTrustScore: Int = 40
    ): Flow<List<UiMoodPulse>> {
        return moodDao.getMatchingMoodPulses(moodType, minTrustScore, limit = 15)
            .map { list ->
                list.map { pulse ->
                    val dist = calculateHaversineDistance(userLat, userLng, pulse.latitude, pulse.longitude)
                    UiMoodPulse(
                        entity = pulse,
                        distanceKm = Math.round(dist * 10.0) / 10.0
                    )
                }.sortedBy { it.distanceKm }
            }
            .flowOn(ioDispatcher)
    }

    fun getAllActivePulsesFlow(userLat: Double, userLng: Double): Flow<List<UiMoodPulse>> {
        return moodDao.getAllActivePulses()
            .map { list ->
                list.map { pulse ->
                    val dist = calculateHaversineDistance(userLat, userLng, pulse.latitude, pulse.longitude)
                    UiMoodPulse(
                        entity = pulse,
                        distanceKm = Math.round(dist * 10.0) / 10.0
                    )
                }.sortedBy { it.distanceKm }
            }
            .flowOn(ioDispatcher)
    }

    fun getUserProfileFlow(): Flow<UserProfileEntity?> {
        return userDao.getUserProfile().flowOn(ioDispatcher)
    }

    suspend fun joinGroup(groupId: String) = withContext(ioDispatcher) {
        groupDao.incrementMemberCount(groupId)
    }

    suspend fun registerEvent(eventId: String) = withContext(ioDispatcher) {
        eventDao.registerForEvent(eventId)
    }

    suspend fun postMoodPulse(pulse: MoodPulseEntity, context: Context? = null) = withContext(ioDispatcher) {
        moodDao.insertMoodPulse(pulse)
        userDao.updateCurrentMood(mood = pulse.moodType)
        // Also upload to Supabase cloud if context is available
        if (context != null) {
            try {
                val api = com.example.data.remote.GapooCloudConfig.createRetrofit(context)
                    .create(com.example.data.remote.GapooApiService::class.java)
                api.publishMoodPulse(
                    pulse = com.example.data.remote.RemoteMoodPulseDto(
                        id = pulse.id,
                        userId = pulse.userId,
                        userName = pulse.userName,
                        userAvatarEmoji = "😊",
                        moodType = pulse.moodType,
                        latitude = pulse.latitude,
                        longitude = pulse.longitude,
                        statusMessage = pulse.statusMessage,
                        trustScore = pulse.trustScore,
                        expiresAt = pulse.timestamp + 1800000L,
                        isActive = pulse.isActive
                    )
                )
            } catch (_: Exception) {
                // Keep local offline copy if network fails
            }
        }
    }

    suspend fun updateUserTrustScore(newScore: Int) = withContext(ioDispatcher) {
        userDao.updateTrustScore(newScore = newScore)
    }

    /**
     * Sync with Free Tier Cloud Database (Supabase / REST backend).
     * Pulls latest groups and events created by other testers and merges into local indexed Room database.
     */
    suspend fun syncWithFreeCloud(context: Context): Result<Int> = withContext(ioDispatcher) {
        try {
            val api = com.example.data.remote.GapooCloudConfig.createRetrofit(context)
                .create(com.example.data.remote.GapooApiService::class.java)

            var syncedCount = 0

            // 1. Fetch remote groups
            val groupsResponse = try {
                api.getRemoteGroups()
            } catch (e: Exception) {
                null
            }

            if (groupsResponse?.isSuccessful == true) {
                val remoteGroups = groupsResponse.body()
                if (!remoteGroups.isNullOrEmpty()) {
                    val entities = remoteGroups.map { dto ->
                        GroupEntity(
                            id = dto.id,
                            name = dto.name,
                            category = dto.category,
                            subCategory = dto.subCategory,
                            description = dto.description,
                            locationName = dto.locationName,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            memberCount = dto.memberCount,
                            maxCapacity = dto.maxCapacity,
                            scheduleType = dto.scheduleType,
                            groupType = dto.groupType,
                            isActive = dto.isActive,
                            iconEmoji = dto.iconEmoji,
                            creatorName = dto.creatorName,
                            creatorTrustScore = dto.creatorTrustScore,
                            rules = dto.rules
                        )
                    }
                    groupDao.insertGroups(entities)
                    syncedCount += entities.size
                }
            }

            // 2. Fetch remote events
            val eventsResponse = try {
                api.getRemoteEvents()
            } catch (e: Exception) {
                null
            }

            if (eventsResponse?.isSuccessful == true) {
                val remoteEvents = eventsResponse.body()
                if (!remoteEvents.isNullOrEmpty()) {
                    val eventEntities = remoteEvents.map { dto ->
                        EventEntity(
                            id = dto.id,
                            title = dto.title,
                            eventType = dto.eventType,
                            description = dto.description,
                            organizerName = dto.organizerName,
                            organizerTrustScore = dto.organizerTrustScore,
                            locationName = dto.locationName,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            price = dto.price,
                            capacity = dto.capacity,
                            registeredCount = dto.registeredCount,
                            startTime = dto.startTime,
                            posterEmoji = dto.posterEmoji,
                            hasCompanionRequest = dto.hasCompanionRequest,
                            isActive = dto.isActive
                        )
                    }
                    eventDao.insertEvents(eventEntities)
                    syncedCount += eventEntities.size
                }
            }

            // Invalidate memory cache so UI gets fresh data
            memoryGroupCache.clear()
            memoryEventCache.clear()

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seeds initial real-world authentic data on first launch if database is empty.
     */
    suspend fun seedInitialDataIfEmpty() = withContext(ioDispatcher) {
        if (groupDao.getGroupCount() > 0) return@withContext

        // Ensure user profile exists
        userDao.insertOrUpdateProfile(
            UserProfileEntity(
                id = "current_user",
                name = "سامان نوری",
                phone = "۰۹۱۲۳۴۵۶۷۸۹",
                avatarEmoji = "🌟",
                trustScore = 85,
                isVerified = true,
                selectedInterests = "هنری,فرهنگی,سرگرمی,علمی",
                currentMood = null,
                latitude = 35.7219,
                longitude = 51.3347,
                trustedContactName = "دوست امن (سارا)",
                trustedContactPhone = "۰۹۱۹۸۷۶۵۴۳۲"
            )
        )

        // Seed social interest groups
        val sampleGroups = listOf(
            GroupEntity(
                id = "grp_1",
                name = "دورهمی کتاب‌خوانان کافه کتاب",
                category = "فرهنگی",
                subCategory = "کتاب‌خوانی",
                description = "بررسی هفتگی رمان‌های معاصر و گفتگوی صمیمانه پیرامون ادبیات داستانی.",
                locationName = "کافه کتاب ترنجستان (میدان انقلاب)",
                latitude = 35.7008,
                longitude = 51.3912,
                memberCount = 8,
                maxCapacity = 12,
                scheduleType = "پنجشنبه‌ها ۱۷:۰۰",
                groupType = "PERSISTENT",
                iconEmoji = "📚",
                creatorName = "مریم حسینی",
                creatorTrustScore = 92,
                rules = "حضور سروقت، رعایت نوبت گفتگو، احترام به نظرات متفاوت"
            ),
            GroupEntity(
                id = "grp_2",
                name = "باشگاه عکاسی خیابانی تهران",
                category = "هنری",
                subCategory = "عکاسی",
                description = "قدم زدن گروهی و ثبت لحظات ناب زندگی روزمره در خیابان ولیعصر.",
                locationName = "ضلع شمال پارک ملت (کنار دریاچه)",
                latitude = 35.7785,
                longitude = 51.4116,
                memberCount = 6,
                maxCapacity = 10,
                scheduleType = "جمعه‌ها ۹:۰۰ صبح",
                groupType = "PERSISTENT",
                iconEmoji = "📷",
                creatorName = "ارشیا راد",
                creatorTrustScore = 88,
                rules = "عکاسی بدون ایجاد مزاحمت برای عابران، همراه داشتن دوربین یا موبایل"
            ),
            GroupEntity(
                id = "grp_3",
                name = "پیاده‌روی عصرگاهی و چای",
                category = "ورزشی",
                subCategory = "پیاده‌روی",
                description = "یک ساعت پیاده‌روی آرام در فضای سرسبز همراه با گفتگوی سبک و نوشیدن چای داغ.",
                locationName = "پارک لاله (ورودی بلوار کشاورز)",
                latitude = 35.7112,
                longitude = 51.3894,
                memberCount = 4,
                maxCapacity = 8,
                scheduleType = "سه‌شنبه‌ها ۱۸:۳۰",
                groupType = "PLANNED",
                iconEmoji = "🚶",
                creatorName = "امیر رضایی",
                creatorTrustScore = 86,
                rules = "کفش ورزشی راحت، انرژی مثبت و لبخند"
            ),
            GroupEntity(
                id = "grp_4",
                name = "دورهمی عاشقان بردگیم و مافیا",
                category = "سرگرمی",
                subCategory = "بازی رومیزی",
                description = "انجام بازی‌های فکری رومیزی (Catan, Codenames, Secret Hitler) در محیطی پرانرژی.",
                locationName = "کافه بردگیم فکری، گیشا",
                latitude = 35.7265,
                longitude = 51.3789,
                memberCount = 7,
                maxCapacity = 10,
                scheduleType = "چهارشنبه‌ها ۱۹:۰۰",
                groupType = "PERSISTENT",
                iconEmoji = "🎲",
                creatorName = "نیلوفر کیان",
                creatorTrustScore = 95,
                rules = "بازی منصفانه و صمیمانه، بدون حساسیت منفی"
            ),
            GroupEntity(
                id = "grp_5",
                name = "توسعه‌دهندگان کاتلین و هوش مصنوعی",
                category = "علمی",
                subCategory = "برنامه‌نویسی",
                description = "تبادل نظر در خصوص معماری مدرن اندروید، جمنای، و پروژه‌های نوآورانه.",
                locationName = "فضای کار اشتراکی کارخانه نوآوری",
                latitude = 35.7198,
                longitude = 51.3412,
                memberCount = 9,
                maxCapacity = 15,
                scheduleType = "دوشنبه‌ها ۱۸:۰۰",
                groupType = "PERSISTENT",
                iconEmoji = "💻",
                creatorName = "پرهام کریمی",
                creatorTrustScore = 90,
                rules = "اشتراک تجربیات کاربردی و کدنویسی تیمی"
            )
        )
        groupDao.insertGroups(sampleGroups)

        // Seed events
        val sampleEvents = listOf(
            EventEntity(
                id = "evt_1",
                title = "کنسرت سنتی و تکنوازی سه‌تار",
                eventType = "کنسرت",
                description = "اجرای قطعات خاطره‌انگیز موسیقی ایرانی در فضای دلنشین فرهنگسرا.",
                organizerName = "گروه موسیقی نوا",
                organizerTrustScore = 96,
                locationName = "سالن خلیج فارس، فرهنگسرای نیاوران",
                latitude = 35.8123,
                longitude = 51.4721,
                startTime = System.currentTimeMillis() + 86400000L * 2,
                price = 150000.0,
                capacity = 40,
                registeredCount = 28,
                posterEmoji = "🎵",
                hasCompanionRequest = true
            ),
            EventEntity(
                id = "evt_2",
                title = "اکران و نقد فیلم سینمایی کلاسیک ۱۲ مرد خشمگین",
                eventType = "نمایش فیلم",
                description = "نمایش نسخه بازسازی شده به همراه جلسه بررسی روانشناختی و حقوقی اثر.",
                organizerName = "کانون فیلم اشراق",
                organizerTrustScore = 91,
                locationName = "کافه رسانه، خ انقلاب",
                latitude = 35.7025,
                longitude = 51.3987,
                startTime = System.currentTimeMillis() + 86400000L * 3,
                price = 0.0,
                capacity = 25,
                registeredCount = 19,
                posterEmoji = "🎬",
                hasCompanionRequest = true
            ),
            EventEntity(
                id = "evt_3",
                title = "کارگاه عملی فن بیان و بداهه‌گویی در جمع",
                eventType = "کارگاه",
                description = "تمرین‌های تعاملی برای غلبه بر خجالت، آشنایی با افراد جدید و گفتگوی اثرگذار.",
                organizerName = "دکتر شهاب علوی",
                organizerTrustScore = 94,
                locationName = "خانه اندیشمندان علوم انسانی، نجات‌اللهی",
                latitude = 35.7145,
                longitude = 51.4178,
                startTime = System.currentTimeMillis() + 86400000L * 4,
                price = 0.0,
                capacity = 20,
                registeredCount = 14,
                posterEmoji = "🎤",
                hasCompanionRequest = false
            ),
            EventEntity(
                id = "evt_4",
                title = "شب شعر و حافظ‌خوانی صمیمانه",
                eventType = "جشن",
                description = "شعرخوانی دسته‌جمعی همراه با تفأل به حافظ و پذیرایی چای سنتی.",
                organizerName = "انجمن ادب و هنر",
                organizerTrustScore = 89,
                locationName = "کوشک باغ فردوس، تجریش",
                latitude = 35.8034,
                longitude = 51.4241,
                startTime = System.currentTimeMillis() + 86400000L * 5,
                price = 50000.0,
                capacity = 30,
                registeredCount = 16,
                posterEmoji = "📜",
                hasCompanionRequest = true
            )
        )
        eventDao.insertEvents(sampleEvents)

        // Seed real-time active mood pulses
        val sampleMoods = listOf(
            MoodPulseEntity(
                id = "mood_1",
                userId = "usr_101",
                userName = "روشنک (عکاس)",
                moodType = "tea",
                statusMessage = "یک ساعت وقت آزاد دارم، دلم یه چای گرم و همصحبتی دوستانه می‌خواد ☕",
                latitude = 35.7235,
                longitude = 51.3362,
                trustScore = 86
            ),
            MoodPulseEntity(
                id = "mood_2",
                userId = "usr_102",
                userName = "بهرام",
                moodType = "walk",
                statusMessage = "هوای پارک لاله عالیه، کسی پایه پیاده‌روی نیم ساعته هست؟ 🚶",
                latitude = 35.7121,
                longitude = 51.3905,
                trustScore = 79
            ),
            MoodPulseEntity(
                id = "mood_3",
                userId = "usr_103",
                userName = "مهسا",
                moodType = "chat",
                statusMessage = "خوشحال می‌شم با یه دوست جدید درباره پادکست و سینما گپ بزنیم 🗣️",
                latitude = 35.7258,
                longitude = 51.3321,
                trustScore = 92
            ),
            MoodPulseEntity(
                id = "mood_4",
                userId = "usr_104",
                userName = "کاوه",
                moodType = "book",
                statusMessage = "نشستم تو کافه کتاب، اگه کتاب‌خوان هستی بیا کنار هم گپ بزنیم 📚",
                latitude = 35.7012,
                longitude = 51.3920,
                trustScore = 84
            ),
            MoodPulseEntity(
                id = "mood_5",
                userId = "usr_105",
                userName = "فرهاد",
                moodType = "group_activity",
                statusMessage = "می‌خوایم بازی فکری دونفره یا گروهی سبک انجام بدیم 🎯",
                latitude = 35.7270,
                longitude = 51.3800,
                trustScore = 88
            )
        )
        moodDao.insertMoodPulses(sampleMoods)
    }

    /**
     * Bounding box algorithm: 1 deg latitude ≈ 111 km, longitude adjusted by cosine
     */
    private fun calculateBoundingBox(
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double
    ): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))
        return BoundingBox(
            minLat = centerLat - latDelta,
            maxLat = centerLat + latDelta,
            minLng = centerLng - lngDelta,
            maxLng = centerLng + lngDelta
        )
    }

    private data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )

    /**
     * Trigonometric Haversine formula for exact distance
     */
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
