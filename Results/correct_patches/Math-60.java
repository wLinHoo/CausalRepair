public double cumulativeProbability(double x) throws MathException {
    // Early checks for extreme values
    if (x == Double.POSITIVE_INFINITY) {
        return 1.0;
    }
    if (x == Double.NEGATIVE_INFINITY) {
        return 0.0;
    }
    
    // Check bounds for numerical stability
    final double bound = 20 * standardDeviation;
    if (x >= mean + bound) {
        return 1.0;
    }
    if (x <= mean - bound) {
        return 0.0;
    }

    final double dev = x - mean;
    try {
        return 0.5 * (1.0 + Erf.erf(dev / (standardDeviation * FastMath.sqrt(2.0))));
    } catch (MaxIterationsExceededException ex) {
        // This should only happen for values not caught by earlier bounds checks
        throw ex;
    }
}