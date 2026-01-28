protected double getInitialDomain(double p) {
    double ret;
    double d = getDenominatorDegreesOfFreedom();
    if (d > 2.0) {
        // use mean when defined
        ret = d / (d - 2.0);
    } else {
        // use a reasonable default when mean is undefined
        ret = 1.0;
    }
    return ret;
}