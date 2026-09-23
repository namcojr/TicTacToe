package com.sunwings.tic_tac_toe

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import android.content.Context
import android.os.SystemClock
import android.graphics.Color
import android.text.InputFilter
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameActivity : AppCompatActivity() {
    private lateinit var board: Array<Array<ImageView>>
    private lateinit var tvPlayerTurn: TextView
    private lateinit var tvBanner: TextView
    private lateinit var btnReset: Button
    private lateinit var btnQuit: Button
    private var aiDifficulty: String = "Easy"
    private var gridSize: Int = 3
    private var currentPlayer = 'X'
    private var gameActive = true
    private lateinit var boardState: Array<CharArray>
    private var aiThinking = false

    private var gradientRes: Int = R.drawable.bg_gradient_default
    private var aiJob: Job? = null
    private var winningCells: List<Pair<Int, Int>> = emptyList()
    private lateinit var ai: TicTacToeAi
    private lateinit var sound: SoundManager

    // Arcade "run" state: accumulates across games until the player loses.
    private var sessionScore = 0
    private var currentStreak = 0
    private var sessionWins = 0
    private var bestStreak = 0

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
        tvBanner = findViewById(R.id.tvBanner)
        btnReset = findViewById(R.id.btnReset)
        btnQuit = findViewById(R.id.btnQuit)
        aiDifficulty = prefs.getString("ai_difficulty", "Easy") ?: "Easy"
        ai = TicTacToeAi(aiDifficulty, gridSize)
        sound = SoundManager(this)
        val theme = prefs.getString("theme_color", "default") ?: "default"
        gradientRes = gradientForTheme(theme)
        board = Array(gridSize) { row ->
            Array(gridSize) { col ->
                val cellId = resources.getIdentifier("btnCell${row}${col}", "id", packageName)
                findViewById<ImageView>(cellId)
            }
        }
        boardState = Array(gridSize) { CharArray(gridSize) { ' ' } }
        setBoardCardBackgrounds()
        setListeners()
        updateGridVisibility()
        updateTurnText()
        updateBanner()
        btnReset.setOnClickListener { resetGame() }
        btnQuit.setOnClickListener { quitRun() }
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

    private fun gradientForTheme(theme: String): Int = when (theme) {
        "blue" -> R.drawable.bg_gradient_blue
        "green" -> R.drawable.bg_gradient_green
        "red" -> R.drawable.bg_gradient_red
        "gold" -> R.drawable.bg_gradient_gold
        "silver" -> R.drawable.bg_gradient_silver
        else -> R.drawable.bg_gradient_default
    }

    private fun setBoardCardBackgrounds() {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                cellBackground(row, col)?.setBackgroundResource(gradientRes)
            }
        }
    }

    private fun cellBackground(row: Int, col: Int): android.widget.FrameLayout? {
        val bgId = resources.getIdentifier("boardBg${row}${col}", "id", packageName)
        return findViewById(bgId)
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

    private fun cloneBoard(): Array<CharArray> = Array(gridSize) { r -> boardState[r].copyOf() }

    // Centralized move application to avoid recursive checks and allow AI moves
    private fun applyMove(row: Int, col: Int, player: Char, fromAi: Boolean) {
        if (!gameActive) return
        if (boardState[row][col] != ' ') return
        boardState[row][col] = player
        val drawableRes = if (player == 'X') R.drawable.ic_ttt_x else R.drawable.ic_ttt_o
        val cell = board[row][col]
        cell.setImageResource(drawableRes)
        cell.setColorFilter(pieceColor(player))
        animatePlace(cell)
        cell.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        sound.play(if (player == 'X') SoundManager.Sound.PLACE else SoundManager.Sound.AI)

        val line = findWinningLine(player)
        if (line != null) {
            if (player == 'X') onHumanWin(line) else onComputerWin(line)
            return
        }
        if (isDraw()) {
            tvPlayerTurn.text = getString(R.string.its_a_draw)
            Toast.makeText(this, getString(R.string.its_a_draw), Toast.LENGTH_SHORT).show()
            sound.play(SoundManager.Sound.DRAW)
            gameActive = false
            btnQuit.visibility = View.VISIBLE
            return
        }

        if (fromAi) {
            // After AI move, switch back to human
            currentPlayer = 'X'
            updateTurnText()
            return
        }

        // After human move, hand over to the AI (which "thinks" for a human-like moment)
        currentPlayer = 'O'
        updateTurnText()
        aiThinking = true
        startThinkingIndicator()
        val snapshot = cloneBoard()
        // Search off the main thread so the player's move and the thinking indicator render
        // immediately; the think time counts from now, so a slow search isn't added on top.
        aiJob = lifecycleScope.launch {
            val start = SystemClock.uptimeMillis()
            val decision = withContext(Dispatchers.Default) { ai.decide(snapshot) }
            val thinkMs = decision?.thinkMs ?: 500L
            delay(thinkMs - (SystemClock.uptimeMillis() - start))
            stopThinkingIndicator()
            if (decision != null && gameActive) {
                applyMove(decision.row, decision.col, 'O', fromAi = true)
            }
            aiThinking = false
        }
    }

    private fun onHumanWin(line: List<Pair<Int, Int>>) {
        gameActive = false
        btnQuit.visibility = View.VISIBLE
        currentStreak++
        sessionWins++
        bestStreak = maxOf(bestStreak, currentStreak)
        sessionScore += ScoreStore.pointsForWin(aiDifficulty, gridSize, currentStreak)
        updateBanner()
        highlightWin(line)
        tvPlayerTurn.text = getString(R.string.you_win)
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        sound.play(SoundManager.Sound.WIN)
        Toast.makeText(this, getString(R.string.you_win), Toast.LENGTH_SHORT).show()
    }

    private fun onComputerWin(line: List<Pair<Int, Int>>) {
        gameActive = false
        btnQuit.visibility = View.VISIBLE
        highlightWin(line)
        tvPlayerTurn.text = getString(R.string.computer_wins)
        window.decorView.performHapticFeedback(HapticFeedbackConstants.REJECT)
        sound.play(SoundManager.Sound.LOSE)
        Toast.makeText(this, getString(R.string.computer_wins), Toast.LENGTH_SHORT).show()
        endRun()
    }

    /** The run is over. Offer to record it if it made the leaderboard, then reset. */
    private fun endRun() {
        val score = sessionScore
        val wins = sessionWins
        val streak = bestStreak
        val difficulty = aiDifficulty
        val qualifies = ScoreStore.qualifies(this, score)
        resetSession()
        if (qualifies) {
            promptForInitials(score, wins, streak, difficulty)
        }
    }

    /** Player chose to stop: record the run if it made the leaderboard, then leave the game. */
    private fun quitRun() {
        sound.play(SoundManager.Sound.TAP)
        val score = sessionScore
        val wins = sessionWins
        val streak = bestStreak
        val difficulty = aiDifficulty
        val qualifies = score > 0 && ScoreStore.qualifies(this, score)
        resetSession()
        if (qualifies) {
            promptForInitials(score, wins, streak, difficulty, finishAfter = true)
        } else {
            finish()
        }
    }

    private fun promptForInitials(
        score: Int,
        wins: Int,
        streak: Int,
        difficulty: String,
        finishAfter: Boolean = false
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_initials, null)
        val et = view.findViewById<EditText>(R.id.etInitials)
        val ghost = view.findViewById<TextView>(R.id.tvGhost)
        et.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(3))

        // Ghost shows the typed letters invisibly followed by the untyped "A"s, so the
        // remaining placeholders line up exactly after what the player has entered.
        fun updateGhost(typed: CharSequence) {
            ghost.text = SpannableString(typed.toString() + "AAA".drop(typed.length)).apply {
                setSpan(ForegroundColorSpan(Color.TRANSPARENT), 0, typed.length, 0)
            }
        }
        updateGhost("")
        et.doAfterTextChanged { updateGhost(it ?: "") }

        // Centre the "AAA" placeholder inside the box (both views are start-aligned)
        et.post {
            val pad = ((et.width - et.paint.measureText("AAA")) / 2).toInt().coerceAtLeast(0)
            et.setPadding(pad, et.paddingTop, 0, et.paddingBottom)
            ghost.setPadding(pad, ghost.paddingTop, 0, ghost.paddingBottom)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_high_score) + "  •  $score pts")
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(R.string.save) { _, _ ->
                val initials = ScoreStore.sanitizeInitials(et.text.toString())
                saveLastInitials(initials)
                val rank = ScoreStore.submit(
                    this,
                    ScoreEntry(initials, score, wins, streak, difficulty, System.currentTimeMillis())
                )
                startActivity(HighScoreActivity.intent(this, rank))
                if (finishAfter) finish()
            }
            .create()

        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else false
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        et.requestFocus()
    }

    private fun pieceColor(player: Char): Int =
        ContextCompat.getColor(this, if (player == 'X') R.color.piece_x else R.color.piece_o)

    /** Returns the coordinates of the winning line for [player], or null if there is none. */
    private fun findWinningLine(player: Char): List<Pair<Int, Int>>? {
        // Rows and columns
        for (i in 0 until gridSize) {
            if ((0 until gridSize).all { boardState[i][it] == player }) {
                return (0 until gridSize).map { Pair(i, it) }
            }
            if ((0 until gridSize).all { boardState[it][i] == player }) {
                return (0 until gridSize).map { Pair(it, i) }
            }
        }
        // Diagonals
        if ((0 until gridSize).all { boardState[it][it] == player }) {
            return (0 until gridSize).map { Pair(it, it) }
        }
        if ((0 until gridSize).all { boardState[it][gridSize - 1 - it] == player }) {
            return (0 until gridSize).map { Pair(it, gridSize - 1 - it) }
        }
        return null
    }

    private fun updateTurnText() {
        tvPlayerTurn.text =
            if (currentPlayer == 'X') getString(R.string.your_turn) else getString(R.string.computer_turn)
    }

    /** Gentle scale + fade pulse on the turn label while the AI "thinks". */
    private fun startThinkingIndicator() {
        val set = AnimationSet(true).apply {
            interpolator = AccelerateDecelerateInterpolator()
            addAnimation(AlphaAnimation(1f, 0.45f).apply {
                duration = 560
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            })
            addAnimation(
                ScaleAnimation(
                    1f, 1.08f, 1f, 1.08f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 560
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                }
            )
        }
        tvPlayerTurn.startAnimation(set)
    }

    private fun stopThinkingIndicator() {
        tvPlayerTurn.clearAnimation()
        tvPlayerTurn.alpha = 1f
    }

    private fun updateBanner() {
        tvBanner.text = getString(R.string.banner_score, sessionScore) +
            "   ·   " + getString(R.string.banner_streak, currentStreak) +
            "   ·   " + getString(R.string.banner_best, bestStreak)
    }

    private fun resetSession() {
        sessionScore = 0
        currentStreak = 0
        sessionWins = 0
        bestStreak = 0
        updateBanner()
    }

    private fun isDraw(): Boolean {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (boardState[row][col] == ' ') return false
            }
        }
        return true
    }

    // ---- Animations -------------------------------------------------------

    private fun animatePlace(view: View) {
        view.clearAnimation()
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.alpha = 0.3f
        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setInterpolator(OvershootInterpolator())
            .setDuration(220)
            .start()
    }

    private fun highlightWin(line: List<Pair<Int, Int>>) {
        winningCells = line
        for ((row, col) in line) {
            cellBackground(row, col)?.setBackgroundResource(R.drawable.bg_cell_win)
            val pulse = ScaleAnimation(
                1f, 1.15f, 1f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 420
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            board[row][col].startAnimation(pulse)
        }
    }

    private fun clearWinHighlight() {
        for ((row, col) in winningCells) {
            board[row][col].clearAnimation()
            cellBackground(row, col)?.setBackgroundResource(gradientRes)
        }
        winningCells = emptyList()
    }

    private fun resetGame() {
        sound.play(SoundManager.Sound.TAP)
        aiJob?.cancel()
        aiThinking = false
        stopThinkingIndicator()
        clearWinHighlight()
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                boardState[row][col] = ' '
                board[row][col].clearAnimation()
                board[row][col].setImageDrawable(null)
            }
        }
        currentPlayer = 'X'
        gameActive = true
        btnQuit.visibility = View.GONE
        updateTurnText()
    }

    override fun onStop() {
        super.onStop()
        // Safety net: record a strong ongoing run if the player is leaving for good.
        if (isFinishing && sessionScore > 0 && ScoreStore.qualifies(this, sessionScore)) {
            ScoreStore.submit(
                this,
                ScoreEntry(lastInitials(), sessionScore, sessionWins, bestStreak, aiDifficulty, System.currentTimeMillis())
            )
            resetSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sound.release()
    }

    private fun lastInitials(): String {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString("last_initials", "AAA") ?: "AAA"
    }

    private fun saveLastInitials(initials: String) {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("last_initials", initials).apply()
    }
}
