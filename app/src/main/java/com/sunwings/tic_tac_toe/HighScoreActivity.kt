package com.sunwings.tic_tac_toe

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HighScoreActivity : AppCompatActivity() {
    private lateinit var layoutPlayerXScores: LinearLayout
    private lateinit var layoutPlayerOScores: LinearLayout
    private lateinit var btnBackToMenu: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        layoutPlayerXScores = findViewById(R.id.layoutPlayerXScores)
        layoutPlayerOScores = findViewById(R.id.layoutPlayerOScores)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        displayScores()

        btnBackToMenu.setOnClickListener {
            finish()
        }
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

    private fun displayScores() {
        val prefs = getSharedPreferences("high_scores", Context.MODE_PRIVATE)
        val xScores = prefs.getStringSet("player_x_scores", emptySet())!!.map { it.toInt() }.sortedDescending().take(5)
        val oScores = prefs.getStringSet("player_o_scores", emptySet())!!.map { it.toInt() }.sortedDescending().take(5)

        layoutPlayerXScores.removeAllViews()
        layoutPlayerOScores.removeAllViews()

        for ((i, score) in xScores.withIndex()) {
            val tv = TextView(this)
            tv.text = "${i + 1}. $score wins"
            tv.textSize = 18f
            layoutPlayerXScores.addView(tv)
        }
        for ((i, score) in oScores.withIndex()) {
            val tv = TextView(this)
            tv.text = "${i + 1}. $score wins"
            tv.textSize = 18f
            layoutPlayerOScores.addView(tv)
        }
    }
}
