package com.ssmath.app

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Retired games are removed before decoding their no-longer-supported enum values. */
internal object ReadingMigration {
    val retiredGames = setOf("CHAPTER", "PARTNERS", "CAPTION")

    private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject.rows(key: String) = get(key)?.let { it as JsonArray }.orEmpty().map { it.jsonObject }

    fun migrate(root: JsonObject): JsonObject {
        val attempts = root.rows("attempts")
        val sessions = root.rows("sessions")
        val removed = attempts.filter { attempt ->
            val game = (attempt["activityRef"] as? JsonObject)?.text("game")
            game in retiredGames || (game == null && attempt.text("contentId")?.let {
                it.startsWith("chapter-") || it.startsWith("partners-") || it.startsWith("caption-")
            } == true)
        }.map { requireNotNull(it.text("id")) }.toMutableSet()
        val removedSessions = sessions.filter { session ->
            session.text("game") in retiredGames ||
                (session["attemptIds"] as? JsonArray).orEmpty().any { it.jsonPrimitive.content in removed }
        }
        removedSessions.forEach { session ->
            removed += (session["attemptIds"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
        }
        val profiles = root.rows("profiles")
        val activeRemoved = profiles.any {
            it.text("id") == root.text("selectedProfileId") && it.text("activeAttemptId") in removed
        }
        return JsonObject(root.toMutableMap().apply {
            put("attempts", JsonArray(attempts.filterNot { it.text("id") in removed }.map { attempt ->
                JsonObject(attempt.toMutableMap().apply {
                    listOf("branch", "partnerId", "turns", "reread").forEach(::remove)
                    (attempt["checkpoints"] as? JsonObject)?.let { checks ->
                        put("checkpoints", JsonObject(checks.mapValues { (_, value) ->
                            JsonObject(value.jsonObject - "shared")
                        }))
                    }
                })
            }))
            put("stories", JsonArray(root.rows("stories").filterNot { it.text("attemptId") in removed }
                .map { JsonObject(it - "turns") }))
            put("profiles", JsonArray(profiles.map {
                if (it.text("activeAttemptId") in removed) JsonObject(it - "activeAttemptId") else it
            }))
            if ("sessions" in root) put("sessions", JsonArray(sessions - removedSessions.toSet()))
            if (removedSessions.isNotEmpty()) {
                val deleted = (root["deletedSessionIds"] as? JsonArray).orEmpty() +
                    removedSessions.map { requireNotNull(it["id"]) }
                put("deletedSessionIds", JsonArray(deleted.distinct()))
            }
            if (activeRemoved && root.text("page") == "ACTIVITY") put("page", JsonPrimitive("MENU"))
        })
    }
}
