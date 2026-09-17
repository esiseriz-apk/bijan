package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Supabase / PostgREST compatible REST interface.
 * Can connect to any free Supabase project or any standard PostgREST / REST server.
 */
interface GapooApiService {

    @GET("rest/v1/groups?select=*&order=memberCount.desc")
    suspend fun getRemoteGroups(
        @Header("apikey") apiKey: String = GapooCloudConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authHeader: String = "Bearer ${GapooCloudConfig.SUPABASE_ANON_KEY}",
        @Query("isActive") isActive: String = "eq.true"
    ): Response<List<RemoteGroupDto>>

    @POST("rest/v1/groups")
    @Headers("Prefer: return=representation")
    suspend fun createRemoteGroup(
        @Header("apikey") apiKey: String = GapooCloudConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authHeader: String = "Bearer ${GapooCloudConfig.SUPABASE_ANON_KEY}",
        @Body group: RemoteGroupDto
    ): Response<List<RemoteGroupDto>>

    @GET("rest/v1/events?select=*&order=startTime.asc")
    suspend fun getRemoteEvents(
        @Header("apikey") apiKey: String = GapooCloudConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authHeader: String = "Bearer ${GapooCloudConfig.SUPABASE_ANON_KEY}",
        @Query("isActive") isActive: String = "eq.true"
    ): Response<List<RemoteEventDto>>

    @GET("rest/v1/mood_pulses?select=*&order=timestamp.desc")
    suspend fun getRemoteMoodPulses(
        @Header("apikey") apiKey: String = GapooCloudConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authHeader: String = "Bearer ${GapooCloudConfig.SUPABASE_ANON_KEY}",
        @Query("isActive") isActive: String = "eq.true"
    ): Response<List<RemoteMoodPulseDto>>

    @POST("rest/v1/mood_pulses")
    @Headers("Prefer: return=representation")
    suspend fun publishMoodPulse(
        @Header("apikey") apiKey: String = GapooCloudConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authHeader: String = "Bearer ${GapooCloudConfig.SUPABASE_ANON_KEY}",
        @Body pulse: RemoteMoodPulseDto
    ): Response<List<RemoteMoodPulseDto>>
}
