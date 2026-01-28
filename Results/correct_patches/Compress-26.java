public static long skip(InputStream input, long numToSkip) throws IOException {
    long remaining = numToSkip;
    while (remaining > 0) {
        long skipped = input.skip(remaining);
        if (skipped == 0) {
            // Fall back to read if skip returns 0
            if (input.read() == -1) {
                break; // EOF reached
            }
            skipped = 1;
        }
        remaining -= skipped;
    }
    return numToSkip - remaining;
}