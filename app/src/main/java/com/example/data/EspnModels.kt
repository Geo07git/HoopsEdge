package com.example.data

data class EspnStandingsResponse(
    val children: List<EspnConference>?
)

data class EspnConference(
    val name: String?,
    val standings: EspnStandings?
)

data class EspnStandings(
    val entries: List<EspnEntry>?
)

data class EspnEntry(
    val team: EspnTeam?,
    val stats: List<EspnStat>?
)

data class EspnTeam(
    val displayName: String?,
    val name: String?
)

data class EspnStat(
    val name: String?,
    val value: Double?,
    val displayValue: String?
)

data class EspnInjuriesResponse(
    val injuries: List<EspnTeamInjuries>?
)

data class EspnTeamInjuries(
    val displayName: String?,
    val injuries: List<EspnInjury>?
)

data class EspnInjury(
    val status: String?,
    val date: String?,
    val athlete: EspnAthlete?
)

data class EspnAthlete(
    val displayName: String?
)

data class EspnScoreboardResponse(
    val events: List<EspnEvent>?
)

data class EspnEvent(
    val id: String?,
    val name: String?,
    val shortName: String?,
    val date: String?,
    val competitions: List<EspnCompetition>?,
    val status: EspnEventStatus?
)

data class EspnCompetition(
    val competitors: List<EspnCompetitor>?
)

data class EspnCompetitor(
    val homeAway: String?,
    val team: EspnTeam?,
    val score: String?
)

data class EspnEventStatus(
    val type: EspnStatusType?
)

data class EspnStatusType(
    val description: String?,
    val detail: String?,
    val shortDetail: String?
)
