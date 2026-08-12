package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RosterResponse(
    @Json(name = "results") val results: RosterResults?
)

@JsonClass(generateAdapter = true)
data class RosterResults(
    @Json(name = "roster") val roster: List<PlayerNode>?
)

@JsonClass(generateAdapter = true)
data class PlayerNode(
    @Json(name = "id") val id: Long?,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "positionAbbreviation") val positionAbbreviation: String?,
    @Json(name = "position") val position: String?,
    @Json(name = "number") val number: String?,
    @Json(name = "age") val age: String?
)

@JsonClass(generateAdapter = true)
data class StatsResponse(
    @Json(name = "resultSets") val resultSets: List<ResultSet>?
)

@JsonClass(generateAdapter = true)
data class ResultSet(
    @Json(name = "name") val name: String?,
    @Json(name = "headers") val headers: List<String>?,
    @Json(name = "rowSet") val rowSet: List<List<Any>>?
)
