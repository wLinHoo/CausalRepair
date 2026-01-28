public void addValue(double value) {
    sumImpl.increment(value);
    sumsqImpl.increment(value);
    minImpl.increment(value);
    maxImpl.increment(value);
    sumLogImpl.increment(value);
    secondMoment.increment(value);
    
    // Check if the implementation is different from the stored statistic
    // or if it maintains its own state (not a direct reference to this class's aggregate)
    if (meanImpl != mean) {
        meanImpl.increment(value);
    }
    if (varianceImpl != variance) {
        varianceImpl.increment(value);
    }
    if (geoMeanImpl != geoMean) {
        geoMeanImpl.increment(value);
    }
    n++;
}