package com.example.domain

object Predictor {
    
    /**
     * Approximates the XGBoost prediction logic from the original script.
     * Since XGBRegressor with X = [1, 2, ..., N] extrapolating to N+1 just returns 
     * the average of the last few samples (due to tree splits ending at max X), 
     * we can simulate this by taking a weighted average favoring recent games.
     * We'll use an Exponential Moving Average (EMA) with a smoothing factor 
     * or a Simple Moving Average (SMA) of the last 10 games to closely emulate the trend.
     */
    fun predictNext(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        if (values.size < 5) {
            // Not enough data for a trend, just average
            return values.average()
        }
        
        // Take the last 15 games and apply EMA to favor recent form
        val recentValues = values.takeLast(12)
        val alpha = 0.33 // Smoothing factor
        var ema = recentValues.first()
        for (i in 1 until recentValues.size) {
            ema = alpha * recentValues[i] + (1 - alpha) * ema
        }
        return ema
    }
}
