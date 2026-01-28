public double solve(final UnivariateRealFunction f, double min, double max, double initial)
    throws MaxIterationsExceededException, FunctionEvaluationException {
    return solve(f, min, max); // Fixed: Pass the function reference to the next call
}