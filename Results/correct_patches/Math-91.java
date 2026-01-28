public int compareTo(Fraction object) {
    if (this == object) {
        return 0;
    }
    if (object == null) {
        throw new NullPointerException();
    }
    
    // Compare by cross-multiplying to avoid floating-point precision issues
    long nOd = ((long) this.numerator) * ((long) object.denominator);
    long dOn = ((long) object.numerator) * ((long) this.denominator);
    return (nOd < dOn) ? -1 : ((nOd > dOn) ? 1 : 0);
}