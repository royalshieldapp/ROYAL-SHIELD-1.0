package com.royalshield.app.features.riskprediction.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

import com.royalshield.app.BuildConfig

/**
 * API Client Configuration
 * Supports localhost (development) and Render (production)
 */
object RiskApiClient {
    
    // Environment Configuration
    private const val LOCALHOST_URL = "https://server-beckend.onrender.com/"  // Android emulator localhost
    
    // Use Render URL from BuildConfig if available, otherwise local
    val BASE_URL = if (BuildConfig.DEBUG) {
        LOCALHOST_URL 
    } else {
        // In production, this should be set in local.properties -> build.gradle
        // We fallback to a placeholder if not defined
        "https://server-beckend.onrender.com/"
    }
    
    /**
     * Moshi JSON converter
     */
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * OkHttp client with logging
     */
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()
    
    /**
     * Retrofit instance
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    
    /**
     * API service instance
     */
    val api: RiskPredictionApi = retrofit.create(RiskPredictionApi::class.java)
}
