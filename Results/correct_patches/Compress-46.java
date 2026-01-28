private static ZipLong unixTimeToZipLong(long l) {
    final long MAX_UNIX_TIME = (1L << 31) - 1; // Maximum signed 32-bit value
    if (l < 0 || l > MAX_UNIX_TIME) {
        throw new IllegalArgumentException("X5455 timestamps must fit in a signed 32 bit integer: " + l);
    }
    return new ZipLong(l);
}