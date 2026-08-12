package com.example.domain

import com.example.data.NetworkClient
import com.example.data.PlayerNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    val teamSide: String, // "HOME" or "AWAY"
    val teamName: String,
    val minReal: Double?,
    val ptsPred: Double?,
    val ptsReal: Double?,
    val rebPred: Double?,
    val rebReal: Double?,
    val astPred: Double?,
    val astReal: Double?,
    val fg3mPred: Double?,
    val fg3mReal: Double?,
    val ptsEdge: Double?,
    val rebEdge: Double?,
    val astEdge: Double?,
    val fg3mEdge: Double?
)

object HoopsRepository {

    private val STAT_COLS = listOf("MIN", "PTS", "REB", "AST", "FG3M")

    suspend fun getMatchupStats(
        competition: Competition,
        homeTeamName: String,
        awayTeamName: String,
        season: String, // e.g. "2025-26" for NBA or "2026" for WNBA
        onProgress: (String) -> Unit = {}
    ): Pair<List<PlayerStats>, List<PlayerStats>> = withContext(Dispatchers.IO) {
        
        onProgress("Fetching rosters...")
        val homeTeamId = if (competition == Competition.NBA) Constants.NBA_TEAMS[homeTeamName] else Constants.WNBA_TEAMS[homeTeamName]
        val awayTeamId = if (competition == Competition.NBA) Constants.NBA_TEAMS[awayTeamName] else Constants.WNBA_TEAMS[awayTeamName]
        
        if (homeTeamId == null || awayTeamId == null) {
            return@withContext Pair(emptyList(), emptyList())
        }
        
        val leagueString = if (competition == Competition.NBA) "nba" else "wnba"
        
        val homeRosterAsync = async { fetchRosterSafe(leagueString, homeTeamId) }
        val awayRosterAsync = async { fetchRosterSafe(leagueString, awayTeamId) }
        
        val homeRoster = homeRosterAsync.await()
        val awayRoster = awayRosterAsync.await()
        
        val totalPlayers = homeRoster.size + awayRoster.size
        val processed = java.util.concurrent.atomic.AtomicInteger(0)

        onProgress("Processing home team...")
        val homeStatsAsync = homeRoster.map { player ->
            async { 
                val stat = processPlayer(competition, player, "HOME", homeTeamName, season)
                onProgress("Processing players... (${processed.incrementAndGet()}/$totalPlayers)")
                stat
            }
        }
        val awayStatsAsync = awayRoster.map { player ->
            async { 
                val stat = processPlayer(competition, player, "AWAY", awayTeamName, season)
                onProgress("Processing players... (${processed.incrementAndGet()}/$totalPlayers)")
                stat
            }
        }

        val homeStats = homeStatsAsync.awaitAll().filterNotNull().sortedByDescending { it.minReal ?: 0.0 }
        val awayStats = awayStatsAsync.awaitAll().filterNotNull().sortedByDescending { it.minReal ?: 0.0 }

        Pair(homeStats, awayStats)
    }

    private val rosterCache = java.util.concurrent.ConcurrentHashMap<String, List<PlayerNode>>()
    private val logsCache = java.util.concurrent.ConcurrentHashMap<String, List<Map<String, Any>>>()

    private suspend fun fetchRosterSafe(league: String, teamId: Long): List<PlayerNode> {
        val cacheKey = "$league-$teamId"
        rosterCache[cacheKey]?.let { return it }

        return try {
            val response = NetworkClient.contentApi.getRoster(league, teamId)
            val roster = response.results?.roster ?: emptyList()
            if (roster.isNotEmpty()) {
                rosterCache[cacheKey] = roster
            }
            roster
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getPreviousSeasons(competition: Competition, currentSeason: String): List<String> {
        return try {
            if (competition == Competition.NBA) {
                // "2025-26" -> "2024-25"
                val startYear = currentSeason.substring(0, 4).toInt()
                val prevStart = startYear - 1
                val prevEnd = currentSeason.substring(5, 7).toInt() - 1
                listOf("${prevStart}-${String.format(java.util.Locale.US, "%02d", prevEnd)}")
            } else {
                // "2026" -> "2025", "2024"
                val year = currentSeason.toInt()
                listOf((year - 1).toString(), (year - 2).toString())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val apiSemaphore = Semaphore(10)

    private suspend fun processPlayer(
        competition: Competition,
        player: PlayerNode,
        side: String,
        teamName: String,
        currentSeason: String
    ): PlayerStats? = apiSemaphore.withPermit {
        val playerId = player.id ?: return null
        val playerName = player.displayName ?: player.name ?: "Unknown"

        val allLogs = mutableListOf<Map<String, Any>>()
        
        // Fetch logs for current season
        val currentLogs = fetchPlayerGameLogsSafe(competition, playerId, currentSeason)
        allLogs.addAll(currentLogs)

        // If not enough games, fetch previous season (lazy loading to save API calls)
        if (allLogs.size < 10) {
            val previousSeasons = getPreviousSeasons(competition, currentSeason)
            if (previousSeasons.isNotEmpty()) {
                val prevLogs = fetchPlayerGameLogsSafe(competition, playerId, previousSeasons.first())
                allLogs.addAll(prevLogs)
            }
        }

        if (allLogs.size < 5) return null
        
        // Sort by date
        val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        val sortedLogs = allLogs.sortedBy { log ->
            try {
                dateFormatter.parse(log["GAME_DATE"].toString())?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        // Current season logs for REAL averages
        val currentSeasonLogs = sortedLogs.filter { it["SEASON"] == currentSeason }
        
        fun getRealAvg(stat: String): Double? {
            if (currentSeasonLogs.isEmpty()) return null
            val vals = currentSeasonLogs.mapNotNull { it[stat] as? Double }
            if (vals.isEmpty()) return null
            return vals.average()
        }

        val minReal = getRealAvg("MIN")
        val ptsReal = getRealAvg("PTS")
        val rebReal = getRealAvg("REB")
        val astReal = getRealAvg("AST")
        val fg3mReal = getRealAvg("FG3M")

        // Prediction: Adjust to predicted minutes
        val minValues = sortedLogs.mapNotNull { it["MIN"] as? Double }
        val minPred = if (minValues.size >= 5) Predictor.predictNext(minValues) else minReal ?: 0.0

        fun getPredAdj(stat: String): Double? {
            val validLogs = sortedLogs.filter { (it["MIN"] as? Double ?: 0.0) > 0.0 }
            if (validLogs.size < 5) return null
            
            // Calculate stat per minute for each game
            val statPerMinValues = validLogs.mapNotNull { log ->
                val s = log[stat] as? Double ?: 0.0
                val m = log["MIN"] as? Double ?: 1.0
                s / m
            }
            
            // Predict next stat per minute, then multiply by predicted minutes
            val statPerMinPred = Predictor.predictNext(statPerMinValues)
            return statPerMinPred * minPred
        }

        fun calcEdge(pred: Double?, real: Double?): Double? {
            if (pred == null || real == null || real == 0.0) return null
            return ((pred - real) / real) * 100.0
        }

        val ptsPred = getPredAdj("PTS")
        val rebPred = getPredAdj("REB")
        val astPred = getPredAdj("AST")
        val fg3mPred = getPredAdj("FG3M")

        return PlayerStats(
            playerId = playerId,
            playerName = playerName,
            teamSide = side,
            teamName = teamName,
            minReal = minReal ?: minPred, // Fallback to pred if real is null
            ptsPred = ptsPred,
            ptsReal = ptsReal,
            rebPred = rebPred,
            rebReal = rebReal,
            astPred = astPred,
            astReal = astReal,
            fg3mPred = fg3mPred,
            fg3mReal = fg3mReal,
            ptsEdge = calcEdge(ptsPred, ptsReal),
            rebEdge = calcEdge(rebPred, rebReal),
            astEdge = calcEdge(astPred, astReal),
            fg3mEdge = calcEdge(fg3mPred, fg3mReal)
        )
    }

    private suspend fun fetchPlayerGameLogsSafe(
        competition: Competition,
        playerId: Long,
        season: String,
        retries: Int = 2
    ): List<Map<String, Any>> {
        val cacheKey = "$competition-$playerId-$season"
        logsCache[cacheKey]?.let { return it }

        var lastError: Exception? = null
        for (i in 0 until retries) {
            try {
                val response = if (competition == Competition.NBA) {
                    NetworkClient.nbaStatsApi.getPlayerGameLog(playerId = playerId, season = season)
                } else {
                    NetworkClient.wnbaStatsApi.getPlayerGameLog(playerId = playerId, season = season)
                }
                
                val resultSet = response.resultSets?.firstOrNull() ?: return emptyList()
                val headers = resultSet.headers ?: return emptyList()
                val rowSet = resultSet.rowSet ?: return emptyList()
                
                val parsedLogs = mutableListOf<Map<String, Any>>()
                for (row in rowSet) {
                    val logMap = mutableMapOf<String, Any>()
                    for (j in headers.indices) {
                        val key = headers[j]
                        val value = row.getOrNull(j)
                        if (value != null) {
                            if (STAT_COLS.contains(key)) {
                                logMap[key] = value.toString().toDoubleOrNull() ?: 0.0
                            } else if (key == "GAME_DATE") {
                                logMap[key] = value.toString()
                            }
                        }
                    }
                    if (STAT_COLS.any { logMap.containsKey(it) }) {
                        logMap["SEASON"] = season
                        parsedLogs.add(logMap)
                    }
                }
                if (parsedLogs.isNotEmpty()) {
                    logsCache[cacheKey] = parsedLogs
                }
                return parsedLogs
            } catch (e: Exception) {
                lastError = e
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
        lastError?.printStackTrace()
        return emptyList()
    }
}
