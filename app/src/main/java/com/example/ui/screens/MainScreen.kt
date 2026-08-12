package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.Competition
import com.example.domain.Constants
import com.example.domain.PlayerStats
import com.example.domain.WinProbabilityModel
import com.example.ui.components.AppDropdown
import com.example.data.PredictionHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatSeasonDisplay(season: String, competition: Competition): String {
    return if (competition == Competition.NBA) {
        when (season) {
            "2026-27" -> "27"
            "2025-26" -> "26"
            "2024-25" -> "25"
            "2023-24" -> "24"
            else -> if (season.length >= 2) season.takeLast(2) else season
        }
    } else {
        when (season) {
            "2026" -> "26"
            "2025" -> "25"
            "2024" -> "24"
            "2023" -> "23"
            else -> if (season.length >= 2) season.takeLast(2) else season
        }
    }
}

private fun parseSeasonFromDisplay(display: String, competition: Competition): String {
    return if (competition == Competition.NBA) {
        when (display) {
            "27" -> "2026-27"
            "26" -> "2025-26"
            "25" -> "2024-25"
            "24" -> "2023-24"
            else -> display
        }
    } else {
        when (display) {
            "26" -> "2026"
            "25" -> "2025"
            "24" -> "2024"
            "23" -> "23"
            else -> display
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val history by viewModel.predictionHistory.collectAsState(initial = emptyList())
    var currentTab by remember { mutableStateOf("Overview") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hoops Edge", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Text(if (isDarkTheme) "☀️" else "🌙", fontSize = 20.sp)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("Overview", "📊", "Overview"),
                        Triple("History", "🕒", "History")
                    )
                    tabs.forEach { (tabId, emoji, label) ->
                        val selected = currentTab == tabId
                        val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { currentTab = tabId }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 18.sp,
                                    color = contentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = fontWeight,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "Overview") {
                FloatingActionButton(
                    onClick = { viewModel.calculate() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("calculate_button")
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Calc", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                }
            } else if (currentTab == "History" && history.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.clearHistory() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Clear", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { padding ->
        
        if (state.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Error") },
                title = { Text("Error") },
                text = { Text(state.errorMessage!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
        
        if (state.isLoading) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelCalculation() },
                title = { Text("Calculating Edge") },
                text = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.loadingMessage ?: "Loading...", textAlign = TextAlign.Center)
                    }
                },
                confirmButton = { },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelCalculation() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isDarkTheme) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = com.example.R.drawable.hoops_edge_icon_1784115880915),
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.Center),
                    alpha = 0.1f
                )
            }
            if (currentTab == "Overview") {
                OverviewContent(state, viewModel, padding)
            } else {
                HistoryContent(history, padding, viewModel)
            }
        }
    }
}

@Composable
fun OverviewContent(state: MainUiState, viewModel: MainViewModel, padding: PaddingValues) {
    val teamOptions = if (state.competition == Competition.NBA) {
        Constants.NBA_TEAMS.keys.toList().sorted()
    } else {
        Constants.WNBA_TEAMS.keys.toList().sorted()
    }
    
    val seasonDisplayOptions = if (state.competition == Competition.NBA) {
        listOf("27", "26", "25")
    } else {
        listOf("26", "25", "24")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Competition Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SegmentedButton(
                    selected = state.competition == Competition.NBA,
                    onClick = { viewModel.onCompetitionChanged(Competition.NBA) },
                    text = "NBA",
                    isLeft = true
                )
                SegmentedButton(
                    selected = state.competition == Competition.WNBA,
                    onClick = { viewModel.onCompetitionChanged(Competition.WNBA) },
                    text = "WNBA",
                    isLeft = false
                )
            }
        }

        // Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppDropdown(
                        label = "Home Team",
                        options = teamOptions,
                        selectedOption = state.homeTeam,
                        onOptionSelected = { viewModel.onHomeTeamChanged(it) }
                    )
                    AppDropdown(
                        label = "Away Team",
                        options = teamOptions,
                        selectedOption = state.awayTeam,
                        onOptionSelected = { viewModel.onAwayTeamChanged(it) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppDropdown(
                            label = "Season",
                            options = seasonDisplayOptions,
                            selectedOption = formatSeasonDisplay(state.season, state.competition),
                            onOptionSelected = { display ->
                                viewModel.onSeasonChanged(parseSeasonFromDisplay(display, state.competition))
                            },
                            modifier = Modifier.weight(0.9f)
                        )
                        OutlinedTextField(
                            value = state.topN.toString(),
                            onValueChange = { viewModel.onTopNChanged(it.toIntOrNull() ?: 9) },
                            label = { Text("Top N") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f).testTag("top_n_input")
                        )
                        OutlinedTextField(
                            value = state.marketTotal.toString(),
                            onValueChange = { viewModel.onMarketTotalChanged(it.toDoubleOrNull() ?: 229.0) },
                            label = { Text("Total") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f).testTag("market_total_input")
                        )
                    }
                }
            }
        }

        // Upcoming Games Schedule (Azi & Mâine)
        item {
            UpcomingGamesSection(
                games = state.upcomingGames,
                isLoading = state.isScheduleLoading,
                competition = state.competition,
                selectedHome = state.homeTeam,
                selectedAway = state.awayTeam,
                onGameClick = { game ->
                    viewModel.selectGameFromSchedule(game)
                }
            )
        }

        // Totals
        if (state.homeStats.isNotEmpty() && state.awayStats.isNotEmpty()) {
            val homePred = kotlin.math.round(state.homeStats.take(state.topN).sumOf { it.ptsPred ?: 0.0 }).toInt()
            val awayPred = kotlin.math.round(state.awayStats.take(state.topN).sumOf { it.ptsPred ?: 0.0 }).toInt()
            val totalPred = homePred + awayPred
            val edge = totalPred - state.marketTotal
            
            item {
                TotalsCard(homePred, awayPred, totalPred, edge, state.homeTeam, state.awayTeam, state.competition)
            }

            item {
                val winProb = com.example.domain.WinProbabilityModel.calculate(
                    homePredPts = homePred.toDouble(),
                    awayPredPts = awayPred.toDouble(),
                    homeStandings = state.homeStandings,
                    awayStandings = state.awayStandings
                )
                WinProbabilityCard(
                    homeTeam = state.homeTeam,
                    awayTeam = state.awayTeam,
                    homeWinProb = state.homeWinProb ?: winProb.homeWinProb,
                    awayWinProb = state.awayWinProb ?: winProb.awayWinProb,
                    competition = state.competition
                )
            }
            
            if (state.homeStandings != null || state.homeInjuries.isNotEmpty() || state.awayStandings != null || state.awayInjuries.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    TeamInsightsCard(
                        homeTeam = state.homeTeam,
                        awayTeam = state.awayTeam,
                        homeStandings = state.homeStandings,
                        awayStandings = state.awayStandings,
                        homeInjuries = state.homeInjuries,
                        awayInjuries = state.awayInjuries
                    )
                }
            }
            
            item {
                Text("Home: ${state.homeTeam} (Top ${state.topN})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }
            
            item {
                PlayerStatsTable(state.homeStats.take(state.topN))
            }
            
            item {
                Text("Away: ${state.awayTeam} (Top ${state.topN})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }
            
            item {
                PlayerStatsTable(state.awayStats.take(state.topN))
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun HistoryContent(history: List<PredictionHistory>, padding: PaddingValues, viewModel: MainViewModel) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("No prediction history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        fun shareCsv(item: PredictionHistory) {
            val csv = java.lang.StringBuilder()
            csv.append("GAME PREDICTION EXPORT\n")
            csv.append("Competition,${item.competition}\n")
            csv.append("Matchup,${item.homeTeam} vs ${item.awayTeam}\n")
            csv.append("Season,${item.season}\n")
            csv.append("Home Projected Total,${item.homePred}\n")
            csv.append("Away Projected Total,${item.awayPred}\n")
            csv.append("Sum Projected Total,${item.homePred + item.awayPred}\n")
            csv.append("Market Total,${item.marketTotal}\n")
            csv.append("Edge,${String.format("%+.1f", item.edge)}\n\n")
            
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.PlayerStats::class.java)
            val adapter = com.example.data.NetworkClient.moshi.adapter<List<com.example.domain.PlayerStats>>(listType)
            
            val homeStats = try { adapter.fromJson(item.homeStatsJson) ?: emptyList() } catch(e:Exception){ emptyList() }
            val awayStats = try { adapter.fromJson(item.awayStatsJson) ?: emptyList() } catch(e:Exception){ emptyList() }
            
            val headers = "Player Name,MIN,PTS Pred,PTS Real,PTS Edge %,REB Pred,REB Real,REB Edge %,AST Pred,AST Real,AST Edge %,3PM Pred,3PM Real,3PM Edge %"
            
            if (homeStats.isNotEmpty()) {
                csv.append("PLAYER STATS (HOME: ${item.homeTeam})\n")
                csv.append(headers).append("\n")
                homeStats.forEach { p ->
                    csv.append(p.playerName.replace(",", " ")).append(",")
                        .append(String.format(Locale.US, "%.1f", p.minReal ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.ptsPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.ptsReal ?: 0.0)).append(",")
                        .append(if (p.ptsEdge != null) String.format(Locale.US, "%.0f%%", p.ptsEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.rebPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.rebReal ?: 0.0)).append(",")
                        .append(if (p.rebEdge != null) String.format(Locale.US, "%.0f%%", p.rebEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.astPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.astReal ?: 0.0)).append(",")
                        .append(if (p.astEdge != null) String.format(Locale.US, "%.0f%%", p.astEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.fg3mPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.fg3mReal ?: 0.0)).append(",")
                        .append(if (p.fg3mEdge != null) String.format(Locale.US, "%.0f%%", p.fg3mEdge) else "").append("\n")
                }
                csv.append("\n")
            }
            
            if (awayStats.isNotEmpty()) {
                csv.append("PLAYER STATS (AWAY: ${item.awayTeam})\n")
                csv.append(headers).append("\n")
                awayStats.forEach { p ->
                    csv.append(p.playerName.replace(",", " ")).append(",")
                        .append(String.format(Locale.US, "%.1f", p.minReal ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.ptsPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.ptsReal ?: 0.0)).append(",")
                        .append(if (p.ptsEdge != null) String.format(Locale.US, "%.0f%%", p.ptsEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.rebPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.rebReal ?: 0.0)).append(",")
                        .append(if (p.rebEdge != null) String.format(Locale.US, "%.0f%%", p.rebEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.astPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.astReal ?: 0.0)).append(",")
                        .append(if (p.astEdge != null) String.format(Locale.US, "%.0f%%", p.astEdge) else "").append(",")
                        .append(String.format(Locale.US, "%.1f", p.fg3mPred ?: 0.0)).append(",")
                        .append(String.format(Locale.US, "%.1f", p.fg3mReal ?: 0.0)).append(",")
                        .append(if (p.fg3mEdge != null) String.format(Locale.US, "%.0f%%", p.fg3mEdge) else "").append("\n")
                }
            }
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Prediction Report - ${item.homeTeam} vs ${item.awayTeam}")
                putExtra(android.content.Intent.EXTRA_TEXT, csv.toString())
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share CSV"))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(history) { item ->
                var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.competition, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateFormat.format(Date(item.dateMs)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { shareCsv(item) }, modifier = Modifier.size(24.dp).padding(start = 8.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteHistoryItem(item) }, modifier = Modifier.size(24.dp).padding(start = 8.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val historyContext = androidx.compose.ui.platform.LocalContext.current
                        val historyImageLoader = remember {
                            coil.ImageLoader.Builder(historyContext)
                                .components { add(coil.decode.SvgDecoder.Factory()) }
                                .crossfade(true)
                                .build()
                        }
                        val comp = try { com.example.domain.Competition.valueOf(item.competition) } catch (e: Exception) { com.example.domain.Competition.NBA }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            coil.compose.AsyncImage(
                                model = com.example.domain.Constants.getTeamLogoUrl(item.homeTeam, comp),
                                imageLoader = historyImageLoader,
                                contentDescription = "${item.homeTeam} Logo",
                                modifier = Modifier.size(24.dp).padding(end = 4.dp)
                            )
                            Text(item.homeTeam, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${item.homePred} - ${item.awayPred}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(item.awayTeam, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            coil.compose.AsyncImage(
                                model = com.example.domain.Constants.getTeamLogoUrl(item.awayTeam, comp),
                                imageLoader = historyImageLoader,
                                contentDescription = "${item.awayTeam} Logo",
                                modifier = Modifier.size(24.dp).padding(start = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proj: ${item.homePred + item.awayPred}", style = MaterialTheme.typography.bodyMedium)
                            Text("Market: ${item.marketTotal}", style = MaterialTheme.typography.bodyMedium)
                            val edgeColor = if (item.edge > 0) Color(0xFF388E3C) else if (item.edge < 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                            Text("Edge: ${String.format("%+.1f", item.edge)}", style = MaterialTheme.typography.bodyMedium, color = edgeColor, fontWeight = FontWeight.Bold)
                        }
                        
                        if (expanded && (item.homeStatsJson.isNotEmpty() || item.awayStatsJson.isNotEmpty())) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.domain.PlayerStats::class.java)
                            val adapter = com.example.data.NetworkClient.moshi.adapter<List<com.example.domain.PlayerStats>>(type)
                            
                            val homeStats = try { adapter.fromJson(item.homeStatsJson) ?: emptyList() } catch(e:Exception){ emptyList() }
                            val awayStats = try { adapter.fromJson(item.awayStatsJson) ?: emptyList() } catch(e:Exception){ emptyList() }
                            
                            if (homeStats.isNotEmpty()) {
                                Text("Home Top Players", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                PlayerStatsTable(homeStats)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            if (awayStats.isNotEmpty()) {
                                Text("Away Top Players", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                PlayerStatsTable(awayStats)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SegmentedButton(selected: Boolean, onClick: () -> Unit, text: String, isLeft: Boolean) {
    val shape = if (isLeft) {
        RoundedCornerShape(topStart = 50f, bottomStart = 50f)
    } else {
        RoundedCornerShape(topEnd = 50f, bottomEnd = 50f)
    }
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = shape,
        modifier = Modifier.testTag("comp_btn_${text.lowercase()}")
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TotalsCard(
    homePred: Int,
    awayPred: Int,
    totalPred: Int,
    edge: Double,
    homeTeam: String,
    awayTeam: String,
    competition: Competition
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    coil.compose.AsyncImage(
                        model = com.example.domain.Constants.getTeamLogoUrl(homeTeam, competition),
                        imageLoader = imageLoader,
                        contentDescription = "$homeTeam Logo",
                        modifier = Modifier.size(40.dp).padding(bottom = 4.dp)
                    )
                    Text("HOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(homePred.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                }

                Text("VS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    coil.compose.AsyncImage(
                        model = com.example.domain.Constants.getTeamLogoUrl(awayTeam, competition),
                        imageLoader = imageLoader,
                        contentDescription = "$awayTeam Logo",
                        modifier = Modifier.size(40.dp).padding(bottom = 4.dp)
                    )
                    Text("AWAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(awayPred.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL PROJECTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(totalPred.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EDGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    val edgeColor = if (edge > 0) Color(0xFF2E7D32) else if (edge < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onPrimaryContainer
                    Text(String.format("%+.1f", edge), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = edgeColor)
                }
            }
        }
    }
}

@Composable
fun PlayerStatsTable(players: List<PlayerStats>) {
    val scrollState = rememberScrollState()
    val tableWidth = 470.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "• Top line: Projected | Bottom line: Season Average (or Edge %)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                // Header
                Row(
                    modifier = Modifier
                        .width(tableWidth)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Player Name", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("MIN", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PTS", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("REB", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("AST", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("3PM", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Rows
                players.forEachIndexed { index, player ->
                    val bg = if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(tableWidth)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        )
                    }
                    Row(
                        modifier = Modifier
                            .width(tableWidth)
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            player.playerName,
                            modifier = Modifier.width(140.dp),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            String.format(Locale.US, "%.1f", player.minReal ?: 0.0),
                            modifier = Modifier.width(50.dp),
                            fontSize = 13.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        StatCell(player.ptsPred, player.ptsReal, player.ptsEdge, Modifier.width(70.dp))
                        StatCell(player.rebPred, player.rebReal, player.rebEdge, Modifier.width(70.dp))
                        StatCell(player.astPred, player.astReal, player.astEdge, Modifier.width(70.dp))
                        StatCell(player.fg3mPred, player.fg3mReal, player.fg3mEdge, Modifier.width(70.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCell(pred: Double?, real: Double?, edge: Double?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(String.format("%.1f", pred ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (edge != null && Math.abs(edge) > 5.0) {
            val color = if (edge > 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
            Text(String.format("%.0f%%", edge), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
        } else {
            Text(String.format("%.1f", real ?: 0.0), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun TeamInsightsCard(
    homeTeam: String,
    awayTeam: String,
    homeStandings: String?,
    awayStandings: String?,
    homeInjuries: List<String>,
    awayInjuries: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Matchup Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Home Team Insights
            Text("Home: $homeTeam", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (homeStandings != null) {
                Text("Standings: $homeStandings", style = MaterialTheme.typography.bodySmall)
            }
            if (homeInjuries.isNotEmpty()) {
                Text("Injuries: ${homeInjuries.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else {
                Text("Injuries: None reported", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Away Team Insights
            Text("Away: $awayTeam", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (awayStandings != null) {
                Text("Standings: $awayStandings", style = MaterialTheme.typography.bodySmall)
            }
            if (awayInjuries.isNotEmpty()) {
                Text("Injuries: ${awayInjuries.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else {
                Text("Injuries: None reported", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C))
            }
        }
    }
}

@Composable
fun WinProbabilityCard(
    homeTeam: String,
    awayTeam: String,
    homeWinProb: Int,
    awayWinProb: Int,
    competition: Competition
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Win Probability Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (homeWinProb >= awayWinProb) "$homeTeam $homeWinProb%" else "$awayTeam $awayWinProb%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil.compose.AsyncImage(
                        model = com.example.domain.Constants.getTeamLogoUrl(homeTeam, competition),
                        imageLoader = imageLoader,
                        contentDescription = "$homeTeam Logo",
                        modifier = Modifier.size(28.dp).padding(end = 6.dp)
                    )
                    Column {
                        Text(
                            homeTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("$homeWinProb%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    "VS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            awayTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("$awayWinProb%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                    }
                    coil.compose.AsyncImage(
                        model = com.example.domain.Constants.getTeamLogoUrl(awayTeam, competition),
                        imageLoader = imageLoader,
                        contentDescription = "$awayTeam Logo",
                        modifier = Modifier.size(28.dp).padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(homeWinProb.coerceAtLeast(1).toFloat())
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(awayWinProb.coerceAtLeast(1).toFloat())
                        .background(Color(0xFF38BDF8))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Model factors: Active player scoring projections, home court advantage (+2.5 pts) & league win %",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun UpcomingGamesSection(
    games: List<com.example.domain.UpcomingGame>,
    isLoading: Boolean,
    competition: Competition,
    selectedHome: String,
    selectedAway: String,
    onGameClick: (com.example.domain.UpcomingGame) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Games Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (games.isEmpty() && !isLoading) {
                Text(
                    text = "No scheduled games available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(games) { game ->
                        val isSelected = selectedHome.contains(game.homeTeamName, ignoreCase = true) && selectedAway.contains(game.awayTeamName, ignoreCase = true)
                        Card(
                            onClick = { onGameClick(game) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.width(170.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = game.statusDetail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    coil.compose.AsyncImage(
                                        model = com.example.domain.Constants.getTeamLogoUrl(game.homeTeamName, competition),
                                        imageLoader = imageLoader,
                                        contentDescription = game.homeTeamName,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text("VS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                    coil.compose.AsyncImage(
                                        model = com.example.domain.Constants.getTeamLogoUrl(game.awayTeamName, competition),
                                        imageLoader = imageLoader,
                                        contentDescription = game.awayTeamName,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${game.homeTeamName} vs ${game.awayTeamName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
