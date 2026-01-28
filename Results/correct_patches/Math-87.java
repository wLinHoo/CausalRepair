private Integer getBasicRow(final int col) {
    Integer row = null;
    for (int i = getNumObjectiveFunctions(); i < getHeight(); i++) {
        final double entry = getEntry(i, col);
        if (MathUtils.equals(entry, 1.0, epsilon)) {
            if (row == null) {
                row = i;
            } else {
                // more than one row has a 1.0 in this column => not basic
                return null;
            }
        } else if (!MathUtils.equals(entry, 0.0, epsilon)) {
            // found non-zero entry => not basic
            return null;
        }
    }
    return row;
}