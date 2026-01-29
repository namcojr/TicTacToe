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
    private var gridSize: Int = 3
    private var currentPlayer = 'X'
    private var gameActive = true
    private lateinit var boardState: Array<CharArray>
    private var aiThinking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPrefs()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val gridSizePref = prefs.getString("grid_size", "3x3")
        gridSize = if (gridSizePref == "4x4") 4 else 3
        if (gridSize == 4) {
            setContentView(R.layout.activity_game_4x4)
        } else {
            setContentView(R.layout.activity_game)
        }

        tvPlayerTurn = findViewById(R.id.tvPlayerTurn)
        btnReset = findViewById(R.id.btnReset)
        aiDifficulty = prefs.getString("ai_difficulty", "Easy") ?: "Easy"
        val theme = prefs.getString("theme_color", "default") ?: "default"
        board = Array(gridSize) { row ->
            Array(gridSize) { col ->
                val cellId = resources.getIdentifier("btnCell${row}${col}", "id", packageName)
                findViewById<ImageView>(cellId)
            }
        }
        boardState = Array(gridSize) { CharArray(gridSize) { ' ' } }
        setBoardCardBackgrounds(theme)
        setListeners()
        updateGridVisibility()
        updateTurnText()
        btnReset.setOnClickListener { resetGame() }
    }

    /**
     * Show/hide board cells based on grid size (3x3 or 4x4)
     */
    private fun updateGridVisibility() {
        for (row in 0..3) {
            for (col in 0..3) {
                val cardId = resources.getIdentifier("card${row}${col}", "id", packageName)
                val card = findViewById<android.view.View>(cardId)
                if (row < gridSize && col < gridSize) {
                    card?.visibility = android.view.View.VISIBLE
                } else {
                    card?.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun setBoardCardBackgrounds(theme: String) {
        val gradientRes = when (theme) {
            "blue" -> R.drawable.bg_gradient_blue
            "green" -> R.drawable.bg_gradient_green
            "red" -> R.drawable.bg_gradient_red
            "gold" -> R.drawable.bg_gradient_gold
            "silver" -> R.drawable.bg_gradient_silver
            else -> R.drawable.bg_gradient_default
        }
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val bgId = resources.getIdentifier("boardBg${row}${col}", "id", packageName)
                val bg = findViewById<android.widget.FrameLayout>(bgId)
                bg?.setBackgroundResource(gradientRes)
            }
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
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
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
            "Easy" -> {
                // 5% chance to play smart on Easy
                if (gridSize == 3 && Math.random() < 0.05) getBestMove() else getRandomMove()
            }
            "Medium" -> getMediumMove()
            "Hard" -> {
                // 5% chance to make a random move on Hard (3x3 only)
                if (gridSize == 3 && Math.random() < 0.05) getRandomMove() else getBestMove()
            }
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
        for (row in 0 until gridSize) for (col in 0 until gridSize) if (boardState[row][col] == ' ') empty.add(Pair(row, col))
        return if (empty.isNotEmpty()) empty.random() else null
    }

    // Medium: Win if possible, block if needed, else random
    private fun getMediumMove(): Pair<Int, Int>? {
        // Try to win
        for (row in 0 until gridSize) for (col in 0 until gridSize) {
            if (boardState[row][col] == ' ') {
                boardState[row][col] = 'O'
                if (checkWinFor('O')) { boardState[row][col] = ' '; return Pair(row, col) }
                boardState[row][col] = ' '
            }
        }
        // Try to block X
        for (row in 0 until gridSize) for (col in 0 until gridSize) {
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
        val maxDepth = if (gridSize == 4) 5 else Int.MAX_VALUE // Limit depth for 4x4, unlimited for 3x3
        for (row in 0 until gridSize) for (col in 0 until gridSize) {
            if (boardState[row][col] == ' ') {
                boardState[row][col] = 'O'
                val score = minimax(0, false, maxDepth)
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
    private fun minimax(depth: Int, isMax: Boolean, maxDepth: Int): Int {
        if (checkWinFor('O')) return 10 - depth
        if (checkWinFor('X')) return depth - 10
        if (isDraw()) return 0
        if (depth >= maxDepth) return 0 // Depth limit reached, treat as draw/neutral
        if (isMax) {
            var best = Int.MIN_VALUE
            for (row in 0 until gridSize) for (col in 0 until gridSize) {
                if (boardState[row][col] == ' ') {
                    boardState[row][col] = 'O'
                    best = maxOf(best, minimax(depth + 1, false, maxDepth))
                    boardState[row][col] = ' '
                }
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (row in 0 until gridSize) for (col in 0 until gridSize) {
                if (boardState[row][col] == ' ') {
                    boardState[row][col] = 'X'
                    best = minOf(best, minimax(depth + 1, true, maxDepth))
                    boardState[row][col] = ' '
                }
            }
            return best
        }
    }

    // Helper to check win for a specific player
    private fun checkWinFor(player: Char): Boolean {
        // Check rows and columns
        for (i in 0 until gridSize) {
            var rowWin = true
            var colWin = true
            for (j in 0 until gridSize) {
                if (boardState[i][j] != player) rowWin = false
                if (boardState[j][i] != player) colWin = false
            }
            if (rowWin || colWin) return true
        }
        // Check diagonals
        var diag1Win = true
        var diag2Win = true
        for (i in 0 until gridSize) {
            if (boardState[i][i] != player) diag1Win = false
            if (boardState[i][gridSize - 1 - i] != player) diag2Win = false
        }
        return diag1Win || diag2Win
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
        return checkWinFor(currentPlayer)
    }

    private fun isDraw(): Boolean {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (boardState[row][col] == ' ') return false
            }
        }
        return true
    }

    private fun resetGame() {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                boardState[row][col] = ' '
                board[row][col].setImageDrawable(null)
            }
        }
        currentPlayer = 'X'
        gameActive = true
        updateTurnText()
    }
}
