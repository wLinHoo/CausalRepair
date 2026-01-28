/**
 * Returns a matrix of p-values associated with (two-tailed) null hypothesis
 * that the corresponding correlation coefficient is zero.
 * 
 * @return matrix of p-values
 * @throws IllegalArgumentException if the underlying TDistribution implementation
 * returns NaN when computing the cumulative probability
 */
public RealMatrix getCorrelationPValues() {
    TDistribution tDistribution = new TDistributionImpl(nObs - 2);
    int nVars = correlationMatrix.getColumnDimension();
    double[][] out = new double[nVars][nVars];
    for (int i = 0; i < nVars; i++) {
        for (int j = 0; j < nVars; j++) {
            double r = correlationMatrix.getEntry(i, j);
            if (Math.abs(r) == 1.0) {
                out[i][j] = 0.0;
                continue;
            }
            double t = Math.abs(r * Math.sqrt((nObs - 2)/(1 - r * r)));
            try {
                double p = 2 * tDistribution.cumulativeProbability(-t);
                out[i][j] = Math.max(p, Double.MIN_VALUE);
            } catch (MathException ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
        }
    }
    return new BlockRealMatrix(out);
}