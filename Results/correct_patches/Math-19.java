private void checkParameters() {
    final double[] init = getStartPoint();
    final double[] lB = getLowerBound();
    final double[] uB = getUpperBound();

    // Checks whether there is at least one finite bound value.
    boolean hasFiniteBounds = false;
    for (int i = 0; i < lB.length; i++) {
        if (!Double.isInfinite(lB[i]) ||
            !Double.isInfinite(uB[i])) {
            hasFiniteBounds = true;
            break;
        }
    }

    // Initialize boundaries array
    boundaries = null;
    
    if (hasFiniteBounds) {
        // Verify no mixed finite and infinite bounds
        for (int i = 0; i < lB.length; i++) {
            if (Double.isInfinite(lB[i]) || Double.isInfinite(uB[i])) {
                throw new MathUnsupportedOperationException();
            }
        }

        // Set up boundaries and check range validity
        boundaries = new double[2][];
        boundaries[0] = lB;
        boundaries[1] = uB;

        final double maxAllowedRange = Double.MAX_VALUE / 2;
        for (int i = 0; i < lB.length; i++) {
            double range = uB[i] - lB[i];
            if (range >= maxAllowedRange) {
                throw new NumberIsTooLargeException(range, maxAllowedRange, false);
            }
        }
    }

    // Validate inputSigma if provided
    if (inputSigma != null) {
        if (inputSigma.length != init.length) {
            throw new DimensionMismatchException(inputSigma.length, init.length);
        }
        for (int i = 0; i < init.length; i++) {
            if (inputSigma[i] < 0) {
                throw new NotPositiveException(inputSigma[i]);
            }
            if (boundaries != null) {
                double range = boundaries[1][i] - boundaries[0][i];
                if (inputSigma[i] > range) {
                    throw new OutOfRangeException(inputSigma[i], 0, range);
                }
            }
        }
    }
}