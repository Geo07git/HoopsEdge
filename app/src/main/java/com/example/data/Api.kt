package com.example.data

import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Path
import retrofit2.http.Query

interface NbaContentApi {
    @GET("public/1/leagues/{league}/teams/{teamId}/roster/")
    suspend fun getRoster(
        @Path("league") league: String,
        @Path("teamId") teamId: Long,
        @HeaderMap headers: Map<String, String> = mapOf(
            "accept" to "application/json, text/plain, */*",
            "origin" to "https://www.nba.com",
            "referer" to "https://www.nba.com/",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        )
    ): RosterResponse
}

interface NbaStatsApi {
    @GET("stats/playergamelog")
    suspend fun getPlayerGameLog(
        @Query("PlayerID") playerId: Long,
        @Query("Season") season: String,
        @Query("SeasonType") seasonType: String = "Regular Season",
        @Query("LeagueID") leagueId: String = "00", // "00" for NBA, "10" for WNBA
        @HeaderMap headers: Map<String, String> = mapOf(
            "Host" to "stats.nba.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7",
            "Referer" to "https://www.nba.com/",
            "Origin" to "https://www.nba.com",
            "x-nba-stats-origin" to "stats",
            "x-nba-stats-token" to "true",
            "Connection" to "keep-alive"
        )
    ): StatsResponse
}

interface WnbaStatsApi {
    @GET("stats/playergamelog")
    suspend fun getPlayerGameLog(
        @Query("PlayerID") playerId: Long,
        @Query("Season") season: String,
        @Query("SeasonType") seasonType: String = "Regular Season",
        @Query("LeagueID") leagueId: String = "10",
        @HeaderMap headers: Map<String, String> = mapOf(
            "Host" to "stats.wnba.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "en-US,en;q=0.9,ro;q=0.8",
            "Referer" to "https://www.wnba.com/",
            "Origin" to "https://www.wnba.com",
            "x-nba-stats-origin" to "stats",
            "x-nba-stats-token" to "true",
            "Connection" to "keep-alive"
        )
    ): StatsResponse
}
