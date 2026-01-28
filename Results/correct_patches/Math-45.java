public OpenMapRealMatrix(int rowDimension, int columnDimension) {
    super(rowDimension, columnDimension);
    
    // Combined dimension validation
    if (rowDimension < 1 || columnDimension < 1) {
        throw new IllegalArgumentException(
            String.format("Matrix dimensions must be positive (got %d x %d)", 
                         rowDimension, columnDimension));
    }
    
    // Memory check using Math.multiplyExact which throws ArithmeticException on overflow
    try {
        Math.multiplyExact(rowDimension, columnDimension);
    } catch (ArithmeticException e) {
        throw new NumberIsTooLargeException(
            (long)rowDimension * columnDimension, 
            Integer.MAX_VALUE, 
            true);
    }
    
    this.rows = rowDimension;
    this.columns = columnDimension;
    this.entries = new OpenIntToDoubleHashMap(0.0);
}