public double chiSquare(double[] expected, long[] observed)
    throws IllegalArgumentException {
    // Input validation
    if (expected == null || observed == null || expected.length < 2 || observed.length != expected.length) {
        throw new IllegalArgumentException(
                "observed and expected array lengths must be equal and at least 2");
    }
    
    // Check for non-negative observed counts and positive expected counts
    for (double e : expected) {
        if (e <= 0.0) {
            throw new IllegalArgumentException("expected counts must be positive");
        }
    }
    for (long o : observed) {
        if (o < 0L) {
            throw new IllegalArgumentException("observed counts must be non-negative");
        }
    }
    
    // Calculate sums
    double sumExpected = 0.0;
    long sumObserved = 0L;
    for (int i = 0; i < observed.length; i++) {
        sumExpected += expected[i];
        sumObserved += observed[i];
    }
    
    // Compute chi-square statistic
    double sumSq = 0.0;
    for (int i = 0; i < observed.length; i++) {
        double e = expected[i];
        if (sumExpected != sumObserved) {
            e = expected[i] * sumObserved / sumExpected;
        }
        double diff = observed[i] - e;
        sumSq += diff * diff / e;
    }
    return sumSq;
}