public double solve(double min, double max)
    throws MaxIterationsExceededException, FunctionEvaluationException {

    clearResult();
    verifyInterval(min, max);

    double ret = Double.NaN;

    double yMin = f.value(min);
    double yMax = f.value(max);

    // Verify bracketing
    double sign = yMin * yMax;

    if (sign > 0.0) {
        // same sign, check near-zero
        if (Math.abs(yMin) <= functionValueAccuracy) {
            setResult(min, 0);
            ret = min;
        } else if (Math.abs(yMax) <= functionValueAccuracy) {
            setResult(max, 0);
            ret = max;
        } else {
            throw new IllegalArgumentException(
                "Function values at endpoints do not have different signs." +
                "  Endpoints: [" + min + "," + max + "]" +
                "  Values: [" + yMin + "," + yMax + "]"
            );
        }
    } else if (sign < 0.0) {
        // proper bracketing
        ret = solve(min, yMin, max, yMax, min, yMin);
    } else {
        // sign == 0 : one endpoint is exactly a root
        if (yMin == 0.0) {
            setResult(min, 0);
            ret = min;
        } else {
            setResult(max, 0);
            ret = max;
        }
    }

    return ret;
}
