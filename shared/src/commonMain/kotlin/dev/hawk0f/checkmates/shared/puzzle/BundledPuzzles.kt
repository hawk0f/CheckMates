package dev.hawk0f.checkmates.shared.puzzle

object BundledPuzzles {

    val all: List<Puzzle> = listOf(
        Puzzle(
            id = "cm-backrank-1",
            fen = "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1",
            solution = listOf("a1a8"),
            rating = 900,
            theme = PuzzleTheme.BACK_RANK
        ),
        Puzzle(
            id = "cm-backrank-2",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3Q2K1 w - - 0 1",
            solution = listOf("d1d8"),
            rating = 950,
            theme = PuzzleTheme.MATE_IN_ONE
        ),
        Puzzle(
            id = "cm-smother-1",
            fen = "6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1",
            solution = listOf("g5f7"),
            rating = 1200,
            theme = PuzzleTheme.MATE_IN_ONE
        ),
        Puzzle(
            id = "cm-fork-1",
            fen = "r3k3/5ppp/8/3N4/8/8/5PPP/6K1 w - - 0 1",
            solution = listOf("d5c7"),
            rating = 1100,
            theme = PuzzleTheme.FORK
        ),
        Puzzle(
            id = "cm-promotion-1",
            fen = "8/P6k/8/8/8/8/6p1/6K1 w - - 0 1",
            solution = listOf("a7a8q"),
            rating = 1000,
            theme = PuzzleTheme.PROMOTION
        ),
        Puzzle(
            id = "cm-hanging-1",
            fen = "6k1/5ppp/8/3q4/8/1B6/5PPP/6K1 w - - 0 1",
            solution = listOf("b3d5"),
            rating = 1000,
            theme = PuzzleTheme.HANGING_PIECE
        ),
        Puzzle(
            id = "cm-pin-1",
            fen = "7k/8/5q2/8/8/8/R7/2B3K1 w - - 0 1",
            solution = listOf("c1b2"),
            rating = 1400,
            theme = PuzzleTheme.PIN
        ),
        Puzzle(
            id = "cm-ladder-1",
            fen = "7k/R7/8/8/8/8/8/1R5K w - - 0 1",
            solution = listOf("b1b8"),
            rating = 800,
            theme = PuzzleTheme.MATE_IN_ONE
        ),
        Puzzle(
            id = "cm-queenmate-1",
            fen = "7k/8/5K2/8/8/8/6Q1/8 w - - 0 1",
            solution = listOf("g2g7"),
            rating = 850,
            theme = PuzzleTheme.MATE_IN_ONE
        ),
        Puzzle(
            id = "cm-fork-2",
            fen = "2q3k1/5ppp/8/3N4/8/8/5PPP/6K1 w - - 0 1",
            solution = listOf("d5e7"),
            rating = 1250,
            theme = PuzzleTheme.FORK
        ),
        Puzzle(
            id = "cm-skewer-1",
            fen = "8/q7/8/k7/8/8/8/6RK w - - 0 1",
            solution = listOf("g1a1"),
            rating = 1350,
            theme = PuzzleTheme.SKEWER
        ),
        Puzzle(
            id = "cm-matein2-1",
            fen = "7k/8/8/8/8/8/R7/1R5K w - - 0 1",
            solution = listOf("b1b7"),
            rating = 1150,
            theme = PuzzleTheme.MATE_IN_TWO
        ),
        Puzzle(
            id = "cm-promotion-2",
            fen = "8/6P1/8/8/8/8/1k6/7K w - - 0 1",
            solution = listOf("g7g8q"),
            rating = 950,
            theme = PuzzleTheme.PROMOTION
        ),
        Puzzle(
            id = "cm-backrank-3",
            fen = "3r2k1/5ppp/8/8/8/8/5PPP/3Q2K1 w - - 0 1",
            solution = listOf("d1d8"),
            rating = 1050,
            theme = PuzzleTheme.BACK_RANK
        )
    )

    fun byId(id: String): Puzzle? = all.find { it.id == id }
}
