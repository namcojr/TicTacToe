package com.sunwings.tic_tac_toe

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Button
import android.content.SharedPreferences
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedDifficulty = prefs.getString("ai_difficulty", "Easy")

        val cardDefault = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardDefault)
        val cardBlue = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardBlue)
        val cardGreen = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardGreen)
        val cardRed = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardRed)
        val cardGold = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardGold)
        val cardSilver = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSilver)

        val themeCards = mapOf(
            "default" to cardDefault,
            "blue" to cardBlue,
            "green" to cardGreen,
            "red" to cardRed,
            "gold" to cardGold,
            "silver" to cardSilver
        )

        themeCards.forEach { (theme, card) ->
            card.setOnClickListener {
                prefs.edit().putString("theme_color", theme).apply()
                recreate()
            }
        }

        val radioGroupDifficulty = findViewById<RadioGroup>(R.id.radioGroupDifficulty)
        val difficultyMap = mapOf(
            "Easy" to R.id.radioEasy,
            "Medium" to R.id.radioMedium,
            "Hard" to R.id.radioHard
        )
        difficultyMap[savedDifficulty]?.let { radioGroupDifficulty.check(it) }

        radioGroupDifficulty.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = when (checkedId) {
                R.id.radioEasy -> "Easy"
                R.id.radioMedium -> "Medium"
                R.id.radioHard -> "Hard"
                else -> "Easy"
            }
            prefs.edit().putString("ai_difficulty", difficulty).apply()
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
}
