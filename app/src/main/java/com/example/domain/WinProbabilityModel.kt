package com.example.domain

import kotlin.math.exp
import kotlin.math.roundToInt

data class WinProbabilityResult(
    val homeWinProb: Int,      // Percentage (e.g. 62)
    val awayWinProb: Int,      // Percentage (e.g. 38)
    val homeWinPctStandings: Double,
    val awayWinPctStandings: Double,
    val expectedMargin: Double // Positive = Home favored, Negative = Away favored
)

object WinProbabilityModel {

    fun calculate(
        homePredPts: Double,
        awayPredPts: Double,
        homeStandings: String?,
        awayStandings: String?
    ): WinProbabilityResult {
        val homeWinPct = parseWinPct(homeStandings)
        val awayWinPct = parseWinPct(awayStandings)

        // 1. Point differential based on active players stats
        val ptsDiff = homePredPts - awayPredPts

        // 2. Home court advantage bonus (typically ~2.5 points)
        val homeCourtBonus = 2.5

        // 3. Standings adjustment: 10% difference in win rate corresponds to approx 1.2 points
        val standingsDiff = (homeWinPct - awayWinPct) * 12.0

        // Combined Net Rating / Expected Margin
        val expectedMargin = (ptsDiff + homeCourtBonus) * 0.70 + standingsDiff * 0.30

        // Logistic function to map point margin to win probability
        val probRaw = 1.0 / (1.0 + exp(-expectedMargin / 6.5))

        // Clamp probabilities to realistic bounds [5%, 95%]
        val clampedProb = probRaw.coerceIn(0.05, 0.95)
        val homeProbPct = (clampedProb * 100.0).roundToInt()
        val awayProbPct = 100 - homeProbPct

        return WinProbabilityResult(
            homeWinProb = homeProbPct,
            awayWinProb = awayProbPct,
            homeWinPctStandings = homeWinPct,
            awayWinPctStandings = awayWinPct,
            expectedMargin = expectedMargin
        )
    }

    private fun parseWinPct(standings: String?): Double {
        if (standings == null || standings.trim().isEmpty()) return 0.500
        
        try {
            val parenMatch = Regex("""\((0?\.\d+)\)""").find(standings)
            if (parenMatch != null) {
                val pctStr = parenMatch.groupValues[1]
                val valPct = pctStr.toDoubleOrNull()
                if (valPct != null) return valPct
            }

            val recordMatch = Regex("""(\d+)-(\d+)""").find(standings)
            if (recordMatch != null) {
                val wins = recordMatch.groupValues[1].toDouble()
                val losses = recordMatch.groupValues[2].toDouble()
                if (wins + losses > 0) {
                    return wins / (wins + losses)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.500
    }
}
