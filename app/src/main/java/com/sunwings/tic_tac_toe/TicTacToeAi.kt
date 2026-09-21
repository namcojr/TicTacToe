package com.sunwings.tic_tac_toe

import kotlin.random.Random

/**
 * A tic-tac-toe opponent that plays like a person rather than a machine.
 *
 * Instead of always finding the mathematically perfect move, each difficulty has
 * a "personality": how often it overlooks an obvious win/block (oversight), how
 * often it plays a careless move (blunder), and how long it appears to think.
 * This makes even the Hard AI occasionally beatable and the Easy AI feel like a
 * distracted human rather than pure noise.
 */
class TicTacToeAi(
    private val difficulty: String,
    private val gridSize: Int,
    private val random: Random = Random.Default
) {
    private val ai = 'O'
    private val human = 'X'
    private val empty = ' '

    /** Chance of failing to notice an immediate win or a move that must be blocked. */
    private val oversight: Double = when (difficulty) {
        "Hard" -> 0.04
        "Medium" -> 0.20
        else -> 0.50
    }

    /** Chance of playing a careless (random) move instead of the considered one. */
    private val blunder: Double = when (difficulty) {
        "Hard" -> 0.08
        "Medium" -> 0.25
        else -> 0.60
    }

    data class Decision(val row: Int, val col: Int, val thinkMs: Long)

    /** Picks a move for [board] (a copy is fine) and how long to "think" first. */
    fun decide(board: Array<CharArray>): Decision? {
        val empties = emptyCells(board)
        if (empties.isEmpty()) return null

        var chosen: Pair<Int, Int>? = null
        var obvious = false

        // 1) Grab an immediate win — but a human sometimes just doesn't spot it.
        val winning = immediateWinFor(board, ai)
        if (winning != null && random.nextDouble() > oversight) {
            chosen = winning
            obvious = true
        }

        // 2) Otherwise block the opponent's imminent win — again, sometimes missed.
        if (chosen == null) {
            val threat = immediateWinFor(board, human)
            if (threat != null && random.nextDouble() > oversight) {
                chosen = threat
                obvious = true
            }
        }

        // 3) No forced move: play a considered position, or occasionally a careless one.
        if (chosen == null) {
            chosen = if (random.nextDouble() < blunder) {
                empties[random.nextInt(empties.size)]
            } else {
                consideredMove(board, empties)
            }
        }

        val move = chosen!!
        return Decision(move.first, move.second, thinkTime(empties.size, obvious))
    }

    // --- Move quality --------------------------------------------------------

    private fun consideredMove(board: Array<CharArray>, empties: List<Pair<Int, Int>>): Pair<Int, Int> {
        // Hard players calculate; everyone else uses human-style positional judgement.
        if (difficulty == "Hard") {
            minimaxMove(board)?.let { return it }
        }
        // Score each empty square and keep the best (with a little taste for variety).
        var best = empties.first()
        var bestScore = Int.MIN_VALUE
        for (cell in empties) {
            val score = positionalScore(board, cell) + random.nextInt(0, 3)
            if (score > bestScore) {
                bestScore = score
                best = cell
            }
        }
        return best
    }

    private fun positionalScore(board: Array<CharArray>, cell: Pair<Int, Int>): Int {
        val (r, c) = cell
        var score = centreWeight(r, c)
        // Reward building our own lines and sitting on the opponent's lines.
        score += 3 * lineOpportunities(board, r, c, ai)
        score += 2 * lineOpportunities(board, r, c, human)
        return score
    }

    /** Higher for central squares, then corners, then edges. */
    private fun centreWeight(r: Int, c: Int): Int {
        val mid = (gridSize - 1) / 2.0
        val dist = Math.abs(r - mid) + Math.abs(c - mid)
        val central = (gridSize.toDouble() - dist).toInt()
        val corner = if ((r == 0 || r == gridSize - 1) && (c == 0 || c == gridSize - 1)) 1 else 0
        return central * 2 + corner
    }

    /** Counts lines through (r,c) that already contain [player] pieces and no opponent. */
    private fun lineOpportunities(board: Array<CharArray>, r: Int, c: Int, player: Char): Int {
        val opponent = if (player == ai) human else ai
        var count = 0
        for (line in linesThrough(r, c)) {
            var own = 0
            var blocked = false
            for ((lr, lc) in line) {
                when (board[lr][lc]) {
                    player -> own++
                    opponent -> { blocked = true }
                }
            }
            if (!blocked && own >= 1) count++
        }
        return count
    }

    // --- Perfect play (used sparingly by Hard) -------------------------------

    private fun minimaxMove(board: Array<CharArray>): Pair<Int, Int>? {
        val maxDepth = if (gridSize == 4) 5 else Int.MAX_VALUE
        var bestScore = Int.MIN_VALUE
        var move: Pair<Int, Int>? = null
        for (cell in emptyCells(board)) {
            board[cell.first][cell.second] = ai
            val score = minimax(board, 0, false, maxDepth)
            board[cell.first][cell.second] = empty
            if (score > bestScore) {
                bestScore = score
                move = cell
            }
        }
        return move
    }

    private fun minimax(board: Array<CharArray>, depth: Int, isMax: Boolean, maxDepth: Int): Int {
        if (hasWon(board, ai)) return 10 - depth
        if (hasWon(board, human)) return depth - 10
        val empties = emptyCells(board)
        if (empties.isEmpty()) return 0
        if (depth >= maxDepth) return 0
        return if (isMax) {
            var best = Int.MIN_VALUE
            for (cell in empties) {
                board[cell.first][cell.second] = ai
                best = maxOf(best, minimax(board, depth + 1, false, maxDepth))
                board[cell.first][cell.second] = empty
            }
            best
        } else {
            var best = Int.MAX_VALUE
            for (cell in empties) {
                board[cell.first][cell.second] = human
                best = minOf(best, minimax(board, depth + 1, true, maxDepth))
                board[cell.first][cell.second] = empty
            }
            best
        }
    }

    // --- Board helpers -------------------------------------------------------

    private fun emptyCells(board: Array<CharArray>): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (r in 0 until gridSize) for (c in 0 until gridSize) if (board[r][c] == empty) out.add(r to c)
        return out
    }

    private fun immediateWinFor(board: Array<CharArray>, player: Char): Pair<Int, Int>? {
        for (cell in emptyCells(board)) {
            board[cell.first][cell.second] = player
            val won = hasWon(board, player)
            board[cell.first][cell.second] = empty
            if (won) return cell
        }
        return null
    }

    private fun hasWon(board: Array<CharArray>, player: Char): Boolean {
        for (i in 0 until gridSize) {
            if ((0 until gridSize).all { board[i][it] == player }) return true
            if ((0 until gridSize).all { board[it][i] == player }) return true
        }
        if ((0 until gridSize).all { board[it][it] == player }) return true
        if ((0 until gridSize).all { board[it][gridSize - 1 - it] == player }) return true
        return false
    }

    /** All winning lines that pass through (r,c). */
    private fun linesThrough(r: Int, c: Int): List<List<Pair<Int, Int>>> {
        val lines = ArrayList<List<Pair<Int, Int>>>()
        lines.add((0 until gridSize).map { r to it })          // row
        lines.add((0 until gridSize).map { it to c })          // column
        if (r == c) lines.add((0 until gridSize).map { it to it })                       // main diagonal
        if (r + c == gridSize - 1) lines.add((0 until gridSize).map { it to gridSize - 1 - it }) // anti-diagonal
        return lines
    }

    // --- Timing --------------------------------------------------------------

    private fun thinkTime(optionCount: Int, obvious: Boolean): Long {
        val base = when (difficulty) {
            "Hard" -> 520
            "Medium" -> 440
            else -> 380
        }
        val perOption = optionCount * when (difficulty) {
            "Hard" -> 70
            "Medium" -> 55
            else -> 40
        }
        val jitter = random.nextInt(0, 320)
        var t = base + perOption + jitter
        if (obvious) t = (t * 0.55).toInt() // reacts quickly to a clear win/block
        return t.coerceIn(320, 1700).toLong()
    }
}
