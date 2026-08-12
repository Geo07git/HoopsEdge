package com.example.domain

import com.example.data.NetworkClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UpcomingGame(
    val gameId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: String?,
    val awayScore: String?,
    val statusDetail: String
)

object EspnService {
    suspend fun getTeamInfo(teamName: String, competition: Competition): Pair<String?, List<String>> {
        val leagueString = if (competition == Competition.NBA) "nba" else "wnba"
        
        var standingsSummary: String? = null
        var injuriesList = emptyList<String>()

        try {
            val standingsRes = NetworkClient.espnApi.getStandings(leagueString)
            val children = standingsRes.children ?: emptyList()
            for (conference in children) {
                val entries = conference.standings?.entries ?: emptyList()
                val entry = entries.find { it.team?.displayName == teamName }
                if (entry != null) {
                    val w = entry.stats?.find { it.name == "wins" }?.value?.toInt()
                    val l = entry.stats?.find { it.name == "losses" }?.value?.toInt()
                    val pct = entry.stats?.find { it.name == "winPercent" }?.displayValue
                    val seed = entry.stats?.find { it.name == "playoffSeed" }?.displayValue
                    val gb = entry.stats?.find { it.name == "gamesBehind" }?.displayValue
                    val streak = entry.stats?.find { it.name == "streak" }?.displayValue
                    
                    standingsSummary = "${w}-${l} ($pct) | Seed: $seed | GB: $gb | Streak: $streak"
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val injuriesRes = NetworkClient.espnApi.getInjuries(leagueString)
            val teamInjuries = injuriesRes.injuries?.find { it.displayName == teamName }
            val list = teamInjuries?.injuries?.mapNotNull { inj ->
                val pName = inj.athlete?.displayName ?: "Unknown Player"
                val status = inj.status ?: "Out"
                "$pName ($status)"
            } ?: emptyList()
            injuriesList = list
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(standingsSummary, injuriesList)
    }

    suspend fun getUpcomingGames(competition: Competition): List<UpcomingGame> {
        val leagueString = if (competition == Competition.NBA) "nba" else "wnba"
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)
        val dateRange = "${todayStr}-${tomorrowStr}"

        return try {
            // First attempt: today and tomorrow
            var scoreboard = NetworkClient.espnApi.getScoreboard(leagueString, dates = dateRange)
            var events = scoreboard.events ?: emptyList()

            // Fallback if no games today/tomorrow (e.g. offseason/break)
            if (events.isEmpty()) {
                scoreboard = NetworkClient.espnApi.getScoreboard(leagueString)
                events = scoreboard.events ?: emptyList()
            }

            events.mapNotNull { event ->
                val comp = event.competitions?.firstOrNull() ?: return@mapNotNull null
                val homeCompetitor = comp.competitors?.find { it.homeAway == "home" }
                val awayCompetitor = comp.competitors?.find { it.homeAway == "away" }
                val homeName = homeCompetitor?.team?.displayName ?: return@mapNotNull null
                val awayName = awayCompetitor?.team?.displayName ?: return@mapNotNull null
                UpcomingGame(
                    gameId = event.id ?: java.util.UUID.randomUUID().toString(),
                    homeTeamName = homeName,
                    awayTeamName = awayName,
                    homeScore = homeCompetitor.score,
                    awayScore = awayCompetitor.score,
                    statusDetail = event.status?.type?.shortDetail ?: event.status?.type?.detail ?: "Scheduled"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
