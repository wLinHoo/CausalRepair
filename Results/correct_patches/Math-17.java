public Dfp multiply(final int x) {
    // Convert the integer to Dfp and perform multiplication
    Dfp result = field.newDfp(x);
    return multiply(result);
}