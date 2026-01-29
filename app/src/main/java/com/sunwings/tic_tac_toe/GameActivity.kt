package com.sunwings.tic_tac_toe

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Button
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class GameActivity : AppCompatActivity() {
    private lateinit var board: Array<Array<ImageView>>
    private lateinit var tvPlayerTurn: TextView
    private lateinit var btnReset: Button
    private var aiDifficulty: String = "Easy"
    private var currentPlayer = 'X'
    private var gameActive = true
    private var boardState = Array(3) { CharArray(3) { ' ' } }
    private var aiThinking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvPlayerTurn = findViewById(R.id.tvPlayerTurn)
        btnReset = findViewById(R.id.btnReset)
        board = Array(3) { row ->
            Array(3) { col ->
                val cellId = resources.getIdentifier("btnCell${row}${col}", "id", packageName)
                findViewById<ImageView>(cellId)
            }
        }
        // Read difficulty and theme from SharedPreferences
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        aiDifficulty = prefs.getString("ai_difficulty", "Easy") ?: "Easy"
        val theme = prefs.getString("theme_color", "default") ?: "default"
        setBoardCardBackgrounds(theme)
        setListeners()
        updateTurnText()
        btnReset.setOnClickListener { resetGame() }
    }

    private fun setBoardCardBackgrounds(theme: String) {
        val bgIds = arrayOf(
            R.id.boardBg00, R.id.boardBg01, R.id.boardBg02,
            R.id.boardBg10, R.id.boardBg11, R.id.boardBg12,
            R.id.boardBg20, R.id.boardBg21, R.id.boardBg22
        )
        val gradientRes = when (theme) {
            "blue" -> R.drawable.bg_gradient_blue
            "green" -> R.drawable.bg_gradient_green
            "red" -> R.drawable.bg_gradient_red
            "gold" -> R.drawable.bg_gradient_gold
            "silver" -> R.drawable.bg_gradient_silver
            else -> R.drawable.bg_gradient_default
        }
        for (id in bgIds) {
            val bg = findViewById<android.widget.FrameLayout>(id)
            bg.setBackgroundResource(gradientRes)
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

    private fun setListeners() {
        for (row in 0..2) {
            for (col in 0..2) {
                board[row][col].setOnClickListener {
                    onCellClicked(row, col)
                }
            }
        }
    }

    private fun onCellClicked(row: Int, col: Int) {
        // Ignore input when game not active or AI is thinking
        if (!gameActive || aiThinking) return
        // If cell occupied, ignore
        if (boardState[row][col] != ' ') return
        // Apply human move
        applyMove(row, col, 'X', fromAi = false)
    }

    // AI move logic based on selected difficulty
    private fun aiMove() {
        if (!gameActive) return
        val move = when (aiDifficulty) {
            "Easy" -> getRandomMove()
            "Medium" -> getMediumMove()
            "Hard" -> getBestMove()
            else -> getRandomMove()
        }
        if (move != null) {
            applyMove(move.first, move.second, 'O', fromAi = true)
        }
    }

    // Centralized move application to avoid recursive checks and allow AI moves
    private fun applyMove(row: Int, col: Int, player: Char, fromAi: Boolean) {
        if (!gameActive) return
        if (boardState[row][col] != ' ') return
        boardState[row][col] = player
        val drawableRes = if (player == 'X') R.drawable.ic_ttt_x else R.drawable.ic_ttt_o
        board[row][col].setImageResource(drawableRes)
        board[row][col].setColorFilter(obtainContrastColor())

        if (checkWinFor(player)) {
            tvPlayerTurn.text = if (player == 'X') "Human player wins" else "Computer wins!"
            Toast.makeText(this, if (player == 'X') "Human player wins" else "Computer wins!", Toast.LENGTH_SHORT).show()
            saveWin(player)
            gameActive = false
            return
        }
        if (isDraw()) {
            tvPlayerTurn.text = "It's a draw!"
            Toast.makeText(this, "It's a draw!", Toast.LENGTH_SHORT).show()
            gameActive = false
            return
        }

        if (fromAi) {
            // After AI move, switch back to human
            currentPlayer = 'X'
            updateTurnText()
            return
        }

        // After human move, hand over to AI
        currentPlayer = 'O'
        updateTurnText()
        aiThinking = true
        Handler(Looper.getMainLooper()).postDelayed({
            aiMove()
            // keep aiThinking true until AI move completes; aiMove -> applyMove will switch player back
            aiThinking = false
        }, 1200)
    }

    private fun obtainContrastColor(): Int {
        val attrs = intArrayOf(android.R.attr.textColorPrimary)
        val ta = theme.obtainStyledAttributes(attrs)
        val color = ta.getColor(0, ContextCompat.getColor(this, android.R.color.white))
        ta.recycle()
        return color
    }

    // Easy: Random empty cell
    private fun getRandomMove(): Pair<Int, Int>? {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) for (col in 0..2) if (boardState[row][col] == ' ') empty.add(Pair(row, col))
        return if (empty.isNotEmpty()) empty.random() else null
    }

    // Medium: Win if possible, block if needed, else random
    private fun getMediumMove(): Pair<Int, Int>? {
        // Try to win
        for (row in 0..2) for (col in 0..2) {
            if (boardState[row][col] == ' ') {
                boardState[row][col] = 'O'
                if (checkWinFor('O')) { boardState[row][col] = ' '; return Pair(row, col) }
                boardState[row][col] = ' '
            }
        }
        // Try to block X
        for (row in 0..2) for (col in 0..2) {
            if (boardState[row][col] == ' ') {
                boardState[row][col] = 'X'
                if (checkWinFor('X')) { boardState[row][col] = ' '; return Pair(row, col) }
                boardState[row][col] = ' '
            }
        }
        // Else random
        return getRandomMove()
    }

    // Hard: Minimax (unforgiving)
    private fun getBestMove(): Pair<Int, Int>? {
        var bestScore = Int.MIN_VALUE
        var move: Pair<Int, Int>? = null
        for (row in 0..2) for (col in 0..2) {
            if (boardState[row][col] == ' ') {
                boardState[row][col] = 'O'
                val score = minimax(0, false)
                boardState[row][col] = ' '
                if (score > bestScore) {
                    bestScore = score
                    move = Pair(row, col)
                }
            }
        }
        return move
    }

    // Minimax algorithm for hard AI
    private fun minimax(depth: Int, isMax: Boolean): Int {
        if (checkWinFor('O')) return 10 - depth
        if (checkWinFor('X')) return depth - 10
        if (isDraw()) return 0
        if (isMax) {
            var best = Int.MIN_VALUE
            for (row in 0..2) for (col in 0..2) {
                if (boardState[row][col] == ' ') {
                    boardState[row][col] = 'O'
                    best = maxOf(best, minimax(depth + 1, false))
                    boardState[row][col] = ' '
                }
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (row in 0..2) for (col in 0..2) {
                if (boardState[row][col] == ' ') {
                    boardState[row][col] = 'X'
                    best = minOf(best, minimax(depth + 1, true))
                    boardState[row][col] = ' '
                }
            }
            return best
        }
    }

    // Helper to check win for a specific player
    private fun checkWinFor(player: Char): Boolean {
        for (i in 0..2) {
            if (boardState[i][0] == player && boardState[i][1] == player && boardState[i][2] == player) return true
            if (boardState[0][i] == player && boardState[1][i] == player && boardState[2][i] == player) return true
        }
        if (boardState[0][0] == player && boardState[1][1] == player && boardState[2][2] == player) return true
        if (boardState[0][2] == player && boardState[1][1] == player && boardState[2][0] == player) return true
        return false
    }

    // Save win to SharedPreferences for high score tracking
    private fun saveWin(player: Char) {
        val prefs = getSharedPreferences("high_scores", MODE_PRIVATE)
        val key = if (player == 'X') "player_x_scores" else "player_o_scores"
        val scores = prefs.getStringSet(key, mutableSetOf())?.map { it.toInt() }?.toMutableList() ?: mutableListOf()
        val newScore = (scores.firstOrNull() ?: 0) + 1
        scores.add(0, newScore)
        val top5 = scores.sortedDescending().take(5).map { it.toString() }.toSet()
        prefs.edit().putStringSet(key, top5).apply()
    }

    private fun updateTurnText() {
        val playerText = if (currentPlayer == 'X') "Human Player" else "Computer"
        tvPlayerTurn.text = "$playerText's Turn"
    }

    private fun checkWin(): Boolean {
        // Check rows and columns
        for (i in 0..2) {
            if (boardState[i][0] == currentPlayer && boardState[i][1] == currentPlayer && boardState[i][2] == currentPlayer) return true
            if (boardState[0][i] == currentPlayer && boardState[1][i] == currentPlayer && boardState[2][i] == currentPlayer) return true
        }
        // Check diagonals
        if (boardState[0][0] == currentPlayer && boardState[1][1] == currentPlayer && boardState[2][2] == currentPlayer) return true
        if (boardState[0][2] == currentPlayer && boardState[1][1] == currentPlayer && boardState[2][0] == currentPlayer) return true
        return false
    }

    private fun isDraw(): Boolean {
        for (row in 0..2) {
            for (col in 0..2) {
                if (boardState[row][col] == ' ') return false
            }
        }
        return true
    }

    private fun resetGame() {
        for (row in 0..2) {
            for (col in 0..2) {
                boardState[row][col] = ' '
                board[row][col].setImageDrawable(null)
            }
        }
        currentPlayer = 'X'
        gameActive = true
        updateTurnText()
    }
}
