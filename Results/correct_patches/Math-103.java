public double cumulativeProbability(double x) throws MathException {
    final double z = (x - mean) / (standardDeviation * Math.sqrt(2.0));
    // Handle extreme values directly to avoid convergence issues
    if (Double.isNaN(z)) {
        return Double.NaN;
    }
    if (z < -20.0) {
        return 0.0;
    }
    if (z > 20.0) {
        return 1.0;
    }
    return 0.5 * (1.0 + Erf.erf(z));
}