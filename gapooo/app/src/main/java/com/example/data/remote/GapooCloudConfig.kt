package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cloud Configuration for Gapoo Free Tier Database (Supabase / REST).
 * Allows users or testers to dynamically update or use the default staging/free server.
 */
object GapooCloudConfig {

    private const val PREFS_NAME = "gapoo_cloud_prefs"
    private const val KEY_BASE_URL = "cloud_base_url"
    private const val KEY_ANON_KEY = "cloud_anon_key"

    // Default Supabase project endpoints configured for live free database
    var defaultBaseUrl = "https://ffilxtapxhlgwyyfgbeh.supabase.co/"
    var SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmaWx4dGFweGhsZ3d5eWZnYmVoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MjYyODEsImV4cCI6MjEwNTIwMjI4MX0.sWC6-MAPZrTM48a2e3j0hrlQeGCXOIz1FmrnvqqGJrg"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        return if (!saved.isNullOrBlank()) {
            if (saved.endsWith("/")) saved else "$saved/"
        } else {
            defaultBaseUrl
        }
    }

    fun saveConfig(context: Context, url: String, anonKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BASE_URL, url)
            .putString(KEY_ANON_KEY, anonKey)
            .apply()
        SUPABASE_ANON_KEY = anonKey
    }

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ANON_KEY, null) ?: SUPABASE_ANON_KEY
    }

    fun createRetrofit(context: Context): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val baseUrl = getBaseUrl(context)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
