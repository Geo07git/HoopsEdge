package com.example.domain

enum class Competition {
    NBA,
    WNBA
}

object Constants {

    val NBA_TEAMS = mapOf(
        "Atlanta Hawks" to 1610612737L,
        "Boston Celtics" to 1610612738L,
        "Brooklyn Nets" to 1610612751L,
        "Charlotte Hornets" to 1610612766L,
        "Chicago Bulls" to 1610612741L,
        "Cleveland Cavaliers" to 1610612739L,
        "Dallas Mavericks" to 1610612742L,
        "Denver Nuggets" to 1610612743L,
        "Detroit Pistons" to 1610612765L,
        "Golden State Warriors" to 1610612744L,
        "Houston Rockets" to 1610612745L,
        "Indiana Pacers" to 1610612754L,
        "Los Angeles Clippers" to 1610612746L,
        "Los Angeles Lakers" to 1610612747L,
        "Memphis Grizzlies" to 1610612763L,
        "Miami Heat" to 1610612748L,
        "Milwaukee Bucks" to 1610612749L,
        "Minnesota Timberwolves" to 1610612750L,
        "New Orleans Pelicans" to 1610612740L,
        "New York Knicks" to 1610612752L,
        "Oklahoma City Thunder" to 1610612760L,
        "Orlando Magic" to 1610612753L,
        "Philadelphia 76ers" to 1610612755L,
        "Phoenix Suns" to 1610612756L,
        "Portland Trail Blazers" to 1610612757L,
        "Sacramento Kings" to 1610612758L,
        "San Antonio Spurs" to 1610612759L,
        "Toronto Raptors" to 1610612761L,
        "Utah Jazz" to 1610612762L,
        "Washington Wizards" to 1610612764L
    )

    val WNBA_TEAMS = mapOf(
        "Atlanta Dream" to 1611661330L,
        "Chicago Sky" to 1611661329L,
        "Connecticut Sun" to 1611661323L,
        "Dallas Wings" to 1611661321L,
        "Golden State Valkyries" to 1611661331L,
        "Indiana Fever" to 1611661325L,
        "Las Vegas Aces" to 1611661319L,
        "Los Angeles Sparks" to 1611661320L,
        "Minnesota Lynx" to 1611661324L,
        "New York Liberty" to 1611661313L,
        "Phoenix Mercury" to 1611661317L,
        "Portland Fire" to 1611661327L,
        "Seattle Storm" to 1611661320L,
        "Toronto Tempo" to 1611661332L,
        "Washington Mystics" to 1611661322L
    )

    fun getTeamLogoUrl(teamName: String, competition: Competition): String? {
        val teamId = if (competition == Competition.NBA) {
            NBA_TEAMS[teamName]
        } else {
            WNBA_TEAMS[teamName]
        } ?: return null

        val compString = if (competition == Competition.NBA) "nba" else "wnba"
        return "https://cdn.$compString.com/logos/$compString/$teamId/global/L/logo.svg"
    }
}
