package dev.hawk0f.checkmates.server

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class Glicko2Rating(
    val rating: Double = Glicko2.DEFAULT_RATING,
    val deviation: Double = Glicko2.DEFAULT_DEVIATION,
    val volatility: Double = Glicko2.DEFAULT_VOLATILITY
)

object Glicko2 {

    const val DEFAULT_RATING = 1500.0
    const val DEFAULT_DEVIATION = 350.0
    const val DEFAULT_VOLATILITY = 0.09
    const val MAX_DEVIATION = 350.0
    const val MIN_DEVIATION = 45.0
    const val PROVISIONAL_DEVIATION = 110.0

    private const val SCALE = 173.7178
    private const val TAU = 0.75
    private const val CONVERGENCE = 0.000001
    private const val MAX_ITERATIONS = 100
    private const val RATING_PERIOD_MILLIS = 24L * 60 * 60 * 1000

    fun update(player: Glicko2Rating, opponent: Glicko2Rating, score: Double): Glicko2Rating {
        val mu = (player.rating - DEFAULT_RATING) / SCALE
        val phi = player.deviation / SCALE
        val opponentMu = (opponent.rating - DEFAULT_RATING) / SCALE
        val opponentPhi = opponent.deviation / SCALE

        val g = g(opponentPhi)
        val expected = expected(mu, opponentMu, opponentPhi)
        val variance = 1.0 / (g * g * expected * (1 - expected))
        val delta = variance * g * (score - expected)

        val volatility = newVolatility(phi, variance, delta, player.volatility)
        val preDeviation = sqrt(phi * phi + volatility * volatility)
        val newPhi = 1.0 / sqrt(1.0 / (preDeviation * preDeviation) + 1.0 / variance)
        val newMu = mu + newPhi * newPhi * g * (score - expected)

        return Glicko2Rating(
            rating = newMu * SCALE + DEFAULT_RATING,
            deviation = (newPhi * SCALE).coerceIn(MIN_DEVIATION, MAX_DEVIATION),
            volatility = volatility
        )
    }

    fun decayed(rating: Glicko2Rating, idleMillis: Long): Glicko2Rating {
        if (idleMillis <= RATING_PERIOD_MILLIS) {
            return rating
        }
        val periods = (idleMillis / RATING_PERIOD_MILLIS).toDouble()
        val phi = rating.deviation / SCALE
        val inflated = sqrt(phi * phi + rating.volatility * rating.volatility * periods) * SCALE
        return rating.copy(deviation = inflated.coerceIn(MIN_DEVIATION, MAX_DEVIATION))
    }

    fun isProvisional(deviation: Double): Boolean = deviation > PROVISIONAL_DEVIATION

    private fun g(phi: Double): Double = 1.0 / sqrt(1.0 + 3.0 * phi * phi / (PI * PI))

    private fun expected(mu: Double, opponentMu: Double, opponentPhi: Double): Double =
        1.0 / (1.0 + exp(-g(opponentPhi) * (mu - opponentMu)))

    private fun newVolatility(phi: Double, variance: Double, delta: Double, volatility: Double): Double {
        val a = ln(volatility * volatility)
        val deltaSquared = delta * delta
        val phiSquared = phi * phi

        val f = { x: Double ->
            val expX = exp(x)
            val denominator = phiSquared + variance + expX
            expX * (deltaSquared - phiSquared - variance - expX) / (2.0 * denominator * denominator) -
                (x - a) / (TAU * TAU)
        }

        var lower = a
        var upper: Double
        if (deltaSquared > phiSquared + variance) {
            upper = ln(deltaSquared - phiSquared - variance)
        } else {
            var k = 1
            while (f(a - k * TAU) < 0 && k < MAX_ITERATIONS) {
                k++
            }
            upper = lower
            lower = a - k * TAU
        }
        if (upper < lower) {
            val swap = upper
            upper = lower
            lower = swap
        }

        var fLower = f(lower)
        var fUpper = f(upper)
        var iterations = 0
        while (abs(upper - lower) > CONVERGENCE && iterations < MAX_ITERATIONS) {
            val mid = lower + (lower - upper) * fLower / (fUpper - fLower)
            val fMid = f(mid)
            if (fMid * fUpper <= 0) {
                lower = upper
                fLower = fUpper
            } else {
                fLower /= 2.0
            }
            upper = mid
            fUpper = fMid
            iterations++
        }
        return exp(upper / 2.0)
    }
}
