public double[] repairAndDecode(final double[] x) {
    if (boundaries != null && isRepairMode) {
        return decode(repair(x));
    }
    return decode(x);
}