protected RealPointValuePair getSolution() {
    double[] coefficients = new double[getOriginalNumDecisionVariables()];
    Integer basicRow =
        getBasicRow(getNumObjectiveFunctions() + getOriginalNumDecisionVariables());
    double mostNegative = basicRow == null ? 0 : getEntry(basicRow, getRhsOffset());

    // First pass: compute all coefficients
    for (int i = 0; i < coefficients.length; i++) {
        basicRow = getBasicRow(getNumObjectiveFunctions() + i);
        coefficients[i] = (basicRow == null ? 0 : getEntry(basicRow, getRhsOffset()));
    }

    // Second pass: handle multiple variables taking same value
    for (int i = 0; i < coefficients.length; i++) {
        basicRow = getBasicRow(getNumObjectiveFunctions() + i);
        if (basicRow != null) {
            // Check if this is not the first variable in the basic row
            boolean hasPredecessor = false;
            for (int j = 0; j < i; j++) {
                Integer otherRow = getBasicRow(getNumObjectiveFunctions() + j);
                if (otherRow != null && otherRow.equals(basicRow)) {
                    hasPredecessor = true;
                    break;
                }
            }
            if (hasPredecessor) {
                coefficients[i] = 0;
            }
        }
    }

    // Adjust for non-negative restriction if needed
    if (!restrictToNonNegative) {
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] -= mostNegative;
        }
    }

    return new RealPointValuePair(coefficients, f.getValue(coefficients));
}