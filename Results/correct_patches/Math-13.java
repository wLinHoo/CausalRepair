private RealMatrix squareRoot(RealMatrix m) {
    // Check if matrix is diagonal first
    boolean isDiagonal = true;
    final int n = m.getRowDimension();
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            if (i != j && Math.abs(m.getEntry(i, j)) > 1e-10) {
                isDiagonal = false;
                break;
            }
        }
        if (!isDiagonal) break;
    }

    if (isDiagonal) {
        // Simple case: diagonal matrix
        double[] sqrtDiagonal = new double[n];
        for (int i = 0; i < n; i++) {
            sqrtDiagonal[i] = Math.sqrt(m.getEntry(i, i));
        }
        return new DiagonalMatrix(sqrtDiagonal);
    } else {
        // General case: use eigen decomposition
        EigenDecomposition dec = new EigenDecomposition(m);
        RealMatrix sqrt = dec.getSquareRoot();
        // Clean up large intermediate objects explicitly
        dec = null;
        return sqrt;
    }
}