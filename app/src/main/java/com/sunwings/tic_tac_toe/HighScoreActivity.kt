package com.sunwings.tic_tac_toe

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class HighScoreActivity : AppCompatActivity() {
    private lateinit var layoutScores: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        layoutScores = findViewById(R.id.layoutScores)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<Button>(R.id.btnBackToMenu).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnClearScores).setOnClickListener { confirmClear() }

        displayScores()
    }

    private fun setThemeFromPrefs() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        when (prefs.getString("theme_color", "default")) {
            "blue" -> setTheme(R.style.Theme_TicTacToe_Blue)
            "green" -> setTheme(R.style.Theme_TicTacToe_Green)
            "red" -> setTheme(R.style.Theme_TicTacToe_Red)
            "gold" -> setTheme(R.style.Theme_TicTacToe_Gold)
            "silver" -> setTheme(R.style.Theme_TicTacToe_Silver)
            else -> setTheme(R.style.Theme_TicTacToe)
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_scores)
            .setMessage("Remove all high scores?")
            .setPositiveButton(R.string.clear_scores) { _, _ ->
                ScoreStore.clear(this)
                displayScores()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun displayScores() {
        val entries = ScoreStore.getLeaderboard(this)
        layoutScores.removeAllViews()

        val highlightRank = intent.getIntExtra(EXTRA_HIGHLIGHT_RANK, -1)

        if (entries.isEmpty()) {
            layoutScores.visibility = LinearLayout.GONE
            tvEmpty.visibility = TextView.VISIBLE
            return
        }

        layoutScores.visibility = LinearLayout.VISIBLE
        tvEmpty.visibility = TextView.GONE

        val inflater = LayoutInflater.from(this)
        entries.forEachIndexed { index, entry ->
            val rank = index + 1
            val row = inflater.inflate(R.layout.item_score, layoutScores, false)

            val tvRank = row.findViewById<TextView>(R.id.tvRank)
            tvRank.text = rank.toString()
            tintRankBadge(tvRank, rank)

            row.findViewById<TextView>(R.id.tvInitials).text = entry.initials
            row.findViewById<TextView>(R.id.tvMeta).text = buildString {
                append(entry.wins)
                append(if (entry.wins == 1) " win" else " wins")
                append(" · streak ")
                append(entry.bestStreak)
                append(" · ")
                append(entry.difficulty)
            }
            row.findViewById<TextView>(R.id.tvScore).text = entry.score.toString()

            if (rank == highlightRank) {
                row.alpha = 0f
                row.postDelayed({ row.animate().alpha(1f).setDuration(400).start() }, 100L)
            }

            layoutScores.addView(row)
        }
    }

    /** Colours the rank badge gold/silver/bronze for the podium. */
    private fun tintRankBadge(view: TextView, rank: Int) {
        val bgColor = when (rank) {
            1 -> ContextCompat.getColor(this, R.color.medal_gold)
            2 -> ContextCompat.getColor(this, R.color.medal_silver)
            3 -> ContextCompat.getColor(this, R.color.medal_bronze)
            else -> ContextCompat.getColor(this, R.color.rank_plain)
        }
        (view.background as? GradientDrawable)?.let {
            it.mutate()
            it.setColor(bgColor)
        }
        view.setTextColor(if (rank <= 3) Color.WHITE else Color.parseColor("#FF1A1A1A"))
    }

    companion object {
        const val EXTRA_HIGHLIGHT_RANK = "highlight_rank"

        fun intent(context: Context, highlightRank: Int = -1): Intent =
            Intent(context, HighScoreActivity::class.java)
                .putExtra(EXTRA_HIGHLIGHT_RANK, highlightRank)
    }
}
