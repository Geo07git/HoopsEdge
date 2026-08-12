package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PredictionHistory
import com.example.domain.Competition
import com.example.domain.Constants
import com.example.domain.HoopsRepository
import com.example.domain.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val competition: Competition = Competition.NBA,
    val homeTeam: String = "",
    val awayTeam: String = "",
    val season: String = "2026-27",
    val topN: Int = 9,
    val marketTotal: Double = 229.0,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val homeStats: List<PlayerStats> = emptyList(),
    val awayStats: List<PlayerStats> = emptyList(),
    val homeStandings: String? = null,
    val awayStandings: String? = null,
    val homeInjuries: List<String> = emptyList(),
    val awayInjuries: List<String> = emptyList(),
    val errorMessage: String? = null,
    val upcomingGames: List<com.example.domain.UpcomingGame> = emptyList(),
    val isScheduleLoading: Boolean = false,
    val homeWinProb: Int? = null,
    val awayWinProb: Int? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).predictionHistoryDao()
    
    val predictionHistory: StateFlow<List<PredictionHistory>> = dao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        setDefaultTeams(Competition.NBA)
        loadSchedule(Competition.NBA)
    }

    fun onCompetitionChanged(competition: Competition) {
        _uiState.update { 
            it.copy(
                competition = competition,
                season = if (competition == Competition.NBA) "2026-27" else "2026",
                marketTotal = if (competition == Competition.NBA) 229.0 else 165.0,
                topN = if (competition == Competition.NBA) 9 else 8
            ) 
        }
        setDefaultTeams(competition)
        loadSchedule(competition)
    }

    fun loadSchedule(competition: Competition) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScheduleLoading = true) }
            val games = com.example.domain.EspnService.getUpcomingGames(competition)
            _uiState.update { it.copy(upcomingGames = games, isScheduleLoading = false) }
        }
    }

    fun selectGameFromSchedule(game: com.example.domain.UpcomingGame) {
        val competition = _uiState.value.competition
        val teams = if (competition == Competition.NBA) Constants.NBA_TEAMS.keys.toList() else Constants.WNBA_TEAMS.keys.toList()
        val matchedHome = teams.find { it.equals(game.homeTeamName, ignoreCase = true) || it.contains(game.homeTeamName, ignoreCase = true) || game.homeTeamName.contains(it, ignoreCase = true) } ?: game.homeTeamName
        val matchedAway = teams.find { it.equals(game.awayTeamName, ignoreCase = true) || it.contains(game.awayTeamName, ignoreCase = true) || game.awayTeamName.contains(it, ignoreCase = true) } ?: game.awayTeamName
        
        _uiState.update { 
            it.copy(
                homeTeam = matchedHome,
                awayTeam = matchedAway
            ) 
        }
    }

    private fun setDefaultTeams(competition: Competition) {
        val teams = if (competition == Competition.NBA) Constants.NBA_TEAMS.keys.toList() else Constants.WNBA_TEAMS.keys.toList()
        if (teams.size >= 2) {
            _uiState.update { it.copy(homeTeam = teams[0], awayTeam = teams[1], homeStats = emptyList(), awayStats = emptyList()) }
        }
    }

    fun onHomeTeamChanged(team: String) {
        _uiState.update { it.copy(homeTeam = team) }
    }

    fun onAwayTeamChanged(team: String) {
        _uiState.update { it.copy(awayTeam = team) }
    }

    fun onSeasonChanged(season: String) {
        _uiState.update { it.copy(season = season) }
    }
    
    fun onTopNChanged(topN: Int) {
        _uiState.update { it.copy(topN = topN) }
    }
    
    fun onMarketTotalChanged(marketTotal: Double) {
        _uiState.update { it.copy(marketTotal = marketTotal) }
    }
    
    private var calcJob: kotlinx.coroutines.Job? = null

    fun calculate() {
        val state = _uiState.value
        if (state.homeTeam == state.awayTeam) {
            _uiState.update { it.copy(errorMessage = "Home and Away teams cannot be the same") }
            return
        }
        
        calcJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadingMessage = "Starting...", errorMessage = null) }
        
        calcJob = viewModelScope.launch {
            try {
                val (homeStats, awayStats) = HoopsRepository.getMatchupStats(
                    competition = state.competition,
                    homeTeamName = state.homeTeam,
                    awayTeamName = state.awayTeam,
                    season = state.season,
                    onProgress = { msg ->
                        _uiState.update { it.copy(loadingMessage = msg) }
                    }
                )
                
                _uiState.update { it.copy(loadingMessage = "Fetching Team Standings & Injuries...") }
                val (homeStandings, homeInjuries) = com.example.domain.EspnService.getTeamInfo(state.homeTeam, state.competition)
                val (awayStandings, awayInjuries) = com.example.domain.EspnService.getTeamInfo(state.awayTeam, state.competition)

                fun isInjured(playerName: String, injuries: List<String>): Boolean {
                    val cleanName = playerName.replace(".", "").replace("-", "").lowercase()
                    return injuries.any { inj ->
                        val injName = inj.substringBeforeLast(" (").trim().replace(".", "").replace("-", "").lowercase()
                        cleanName.contains(injName) || injName.contains(cleanName)
                    }
                }

                val activeHomeStats = homeStats.filter { !isInjured(it.playerName, homeInjuries) }
                val activeAwayStats = awayStats.filter { !isInjured(it.playerName, awayInjuries) }

                if (activeHomeStats.isEmpty() && activeAwayStats.isEmpty()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "No data available. The season might not have started yet, or the API request was blocked."
                        ) 
                    }
                    return@launch
                }
                
                val homePred = kotlin.math.round(activeHomeStats.take(state.topN).sumOf { it.ptsPred ?: 0.0 }).toInt()
                val awayPred = kotlin.math.round(activeAwayStats.take(state.topN).sumOf { it.ptsPred ?: 0.0 }).toInt()
                val totalPred = homePred + awayPred
                val edge = totalPred - state.marketTotal

                // Save prediction history automatically after calculation
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.PlayerStats::class.java)
                val adapter = com.example.data.NetworkClient.moshi.adapter<List<com.example.domain.PlayerStats>>(type)
                dao.insertHistory(
                    PredictionHistory(
                        competition = state.competition.name,
                        homeTeam = state.homeTeam,
                        awayTeam = state.awayTeam,
                        season = state.season,
                        homePred = homePred,
                        awayPred = awayPred,
                        marketTotal = state.marketTotal,
                        edge = edge,
                        homeStatsJson = adapter.toJson(activeHomeStats),
                        awayStatsJson = adapter.toJson(activeAwayStats)
                    )
                )

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        homeStats = activeHomeStats,
                        awayStats = activeAwayStats,
                        homeStandings = homeStandings,
                        awayStandings = awayStandings,
                        homeInjuries = homeInjuries,
                        awayInjuries = awayInjuries
                    ) 
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore, as it was cancelled intentionally
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error calculating stats: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun cancelCalculation() {
        calcJob?.cancel()
        _uiState.update { it.copy(isLoading = false, errorMessage = "Calculation cancelled.") }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }
    
    fun deleteHistoryItem(item: PredictionHistory) {
        viewModelScope.launch {
            dao.deleteItem(item.id)
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

