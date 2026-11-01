package com.example.core.network

import android.content.Context
import com.example.core.constants.AppConstants
import com.example.core.security.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var apiService: ApiService? = null
    private var okHttpClient: OkHttpClient? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient(context).also { okHttpClient = it }
        }
    }

    fun getService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context).create(ApiService::class.java).also {
                apiService = it
            }
        }
    }

    private fun buildOkHttpClient(context: Context): OkHttpClient {
        val tokenStorage = TokenStorage.getInstance(context)

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()

            val token = tokenStorage.getAuthToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val deviceToken = tokenStorage.getDeviceToken()
            val deviceId = tokenStorage.getDeviceId()
            if (!deviceToken.isNullOrBlank()) {
                requestBuilder.header("X-Device-Token", deviceToken)
            }
            if (!deviceId.isNullOrBlank()) {
                requestBuilder.header("X-Device-Id", deviceId)
            }

            chain.proceed(requestBuilder.build())
        }

        // Never log SMS request/response bodies or credentials. BODY logging can expose
        // phone numbers, message text, Firebase tokens and device tokens in logcat.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("X-Device-Token")
            redactHeader("x-device-token")
            level = if (com.example.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL)
            .client(getOkHttpClient(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
