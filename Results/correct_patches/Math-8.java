public T[] sample(int sampleSize) throws NotStrictlyPositiveException {
    if (sampleSize <= 0) {
        throw new NotStrictlyPositiveException(LocalizedFormats.NUMBER_OF_SAMPLES,
                sampleSize);
    }

    // Use Object.class as the component type since anonymous classes can't be used for array creation
    @SuppressWarnings("unchecked") // safe as we create the array using Object.class
    final T[] out = (T[]) java.lang.reflect.Array.newInstance(Object.class, sampleSize);

    for (int i = 0; i < sampleSize; i++) {
        out[i] = sample();
    }

    return out;
}