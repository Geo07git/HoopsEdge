package com.example.data

import retrofit2.http.GET
import retrofit2.http.Path

interface EspnApi {
    @GET("apis/v2/sports/basketball/{league}/standings")
    suspend fun getStandings(
        @Path("league") league: String
    ): EspnStandingsResponse

    @GET("apis/site/v2/sports/basketball/{league}/injuries")
    suspend fun getInjuries(
        @Path("league") league: String
    ): EspnInjuriesResponse

    @GET("apis/site/v2/sports/basketball/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("league") league: String,
        @retrofit2.http.Query("dates") dates: String? = null
    ): EspnScoreboardResponse
}
