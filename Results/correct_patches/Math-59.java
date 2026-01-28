public static float max(final float a, final float b) {
    if (Float.isNaN(a) || Float.isNaN(b)) {
        return Float.NaN;
    }
    return (a <= b) ? b : a;
}