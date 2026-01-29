package com.sunwings.tic_tac_toe

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {


    private var lastTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        lastTheme = prefs.getString("theme_color", "default")

        val btnStartGame = findViewById<Button>(R.id.btnStartGame)
        val btnHighScores = findViewById<Button>(R.id.btnHighScores)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnStartGame.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        btnHighScores.setOnClickListener {
            startActivity(Intent(this, HighScoreActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme_color", "default")
        if (currentTheme != lastTheme) {
            recreate()
        }
        lastTheme = currentTheme
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
}
