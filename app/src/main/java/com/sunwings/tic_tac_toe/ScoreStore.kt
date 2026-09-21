package com.sunwings.tic_tac_toe

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single leaderboard entry representing a completed "run" (a streak of games
 * played back-to-back until the player loses or leaves).
 */
data class ScoreEntry(
    val initials: String,
    val score: Int,
    val wins: Int,
    val bestStreak: Int,
    val difficulty: String,
    val timestamp: Long
)

/**
 * Persists the arcade-style high score table for as long as the app is installed.
 *
 * Scores are stored as a JSON array in SharedPreferences. Entries are ranked by
 * weighted score (harder AI and the 4x4 board are worth more, and consecutive
 * wins build a streak bonus).
 */
object ScoreStore {
    private const val PREFS = "high_scores"
    private const val KEY_LEADERBOARD = "leaderboard"
    const val MAX_ENTRIES = 10

    /** Points awarded for a single win, factoring in difficulty, grid size and streak. */
    fun pointsForWin(difficulty: String, gridSize: Int, streak: Int): Int {
        val base = when (difficulty) {
            "Hard" -> 6
            "Medium" -> 3
            else -> 1
        }
        val gridMultiplier = if (gridSize == 4) 2 else 1
        val streakBonus = (streak - 1).coerceAtLeast(0)
        return base * gridMultiplier + streakBonus
    }

    fun getLeaderboard(context: Context): List<ScoreEntry> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LEADERBOARD, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<ScoreEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ScoreEntry(
                        initials = o.optString("initials", "AAA"),
                        score = o.optInt("score", 0),
                        wins = o.optInt("wins", 0),
                        bestStreak = o.optInt("bestStreak", 0),
                        difficulty = o.optString("difficulty", "Easy"),
                        timestamp = o.optLong("timestamp", 0L)
                    )
                )
            }
            list.sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.timestamp })
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** True if [score] would earn a spot on the leaderboard. */
    fun qualifies(context: Context, score: Int): Boolean {
        if (score <= 0) return false
        val board = getLeaderboard(context)
        if (board.size < MAX_ENTRIES) return true
        val lowest = board.minOf { it.score }
        return score > lowest
    }

    /** Inserts [entry], keeps only the top [MAX_ENTRIES] and returns the new rank (1-based) or -1. */
    fun submit(context: Context, entry: ScoreEntry): Int {
        val updated = (getLeaderboard(context) + entry)
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.timestamp })
            .take(MAX_ENTRIES)

        val arr = JSONArray()
        for (e in updated) {
            arr.put(
                JSONObject()
                    .put("initials", e.initials)
                    .put("score", e.score)
                    .put("wins", e.wins)
                    .put("bestStreak", e.bestStreak)
                    .put("difficulty", e.difficulty)
                    .put("timestamp", e.timestamp)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEADERBOARD, arr.toString())
            .apply()

        return updated.indexOf(entry).let { if (it >= 0) it + 1 else -1 }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LEADERBOARD)
            .apply()
    }

    /** Sanitises free text into exactly three uppercase A-Z initials. */
    fun sanitizeInitials(input: String): String {
        val cleaned = input.uppercase().filter { it in 'A'..'Z' }.take(3)
        return cleaned.padEnd(3, 'A')
    }
}
