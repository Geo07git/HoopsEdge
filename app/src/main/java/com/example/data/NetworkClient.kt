package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 50 })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val contentApi: NbaContentApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://content-api-prod.nba.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NbaContentApi::class.java)
    }

    val nbaStatsApi: NbaStatsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://stats.nba.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NbaStatsApi::class.java)
    }
    
    val wnbaStatsApi: WnbaStatsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://stats.wnba.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WnbaStatsApi::class.java)
    }

    val espnApi: EspnApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://site.api.espn.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EspnApi::class.java)
    }
}
