public CholeskyDecompositionImpl(final RealMatrix matrix,
                                final double relativeSymmetryThreshold,
                                final double absolutePositivityThreshold)
    throws NonSquareMatrixException,
           NotSymmetricMatrixException, NotPositiveDefiniteMatrixException {

    if (!matrix.isSquare()) {
        throw new NonSquareMatrixException(matrix.getRowDimension(),
                                           matrix.getColumnDimension());
    }

    final int order = matrix.getRowDimension();
    // Create defensive copy of matrix data
    lTData = new double[order][order];
    for (int i = 0; i < order; ++i) {
        System.arraycopy(matrix.getRow(i), 0, lTData[i], 0, order);
    }
    cachedL = null;
    cachedLT = null;

    // check the matrix before transformation
    for (int i = 0; i < order; ++i) {
        if (lTData[i][i] <= absolutePositivityThreshold) {
            throw new NotPositiveDefiniteMatrixException();
        }

        final double[] lI = lTData[i];
        // check off-diagonal elements (and reset them to 0)
        for (int j = i + 1; j < order; ++j) {
            final double[] lJ = lTData[j];
            final double lIJ = lI[j];
            final double lJI = lJ[i];
            
            // Verify symmetry
            if (Math.abs(lIJ - lJI) > 
                relativeSymmetryThreshold * Math.max(Math.abs(lIJ), Math.abs(lJI))) {
                throw new NotSymmetricMatrixException();
            }
            
            // Clear lower triangular part
            lJ[i] = 0;
        }
    }

    // Perform Cholesky decomposition
    for (int i = 0; i < order; ++i) {
        final double[] ltI = lTData[i];
        
        // Recheck diagonal element to ensure positive definiteness
        if (ltI[i] <= 0) {
            throw new NotPositiveDefiniteMatrixException();
        }
        
        // Compute diagonal element
        ltI[i] = Math.sqrt(ltI[i]);
        final double invLii = 1.0 / ltI[i];

        // Update remaining elements in the row
        for (int q = i + 1; q < order; ++q) {
            ltI[q] *= invLii;
        }

        // Update lower right submatrix
        for (int q = i + 1; q < order; ++q) {
            final double[] ltQ = lTData[q];
            final double lIq = ltI[q];
            for (int p = q; p < order; ++p) {
                ltQ[p] -= lIq * ltI[p];
            }
        }
    }
}