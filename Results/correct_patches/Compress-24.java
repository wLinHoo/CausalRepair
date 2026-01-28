public static long parseOctal(final byte[] buffer, final int offset, final int length) {
    long    result = 0;
    int     end = offset + length;
    int     start = offset;

    if (length < 2){
        throw new IllegalArgumentException("Length "+length+" must be at least 2");
    }

    if (buffer[start] == 0) {
        return 0L;
    }

    // Skip leading spaces
    while (start < end){
        if (buffer[start] == ' '){
            start++;
        } else {
            break;
        }
    }

    // Trim all trailing NULs and spaces if they exist
    // But don't throw exception if there's no trailing space/NUL
    while (end > start) {
        byte trailer = buffer[end - 1];
        if (trailer == 0 || trailer == ' ') {
            end--;
        } else {
            break;
        }
    }

    // Must have at least one digit after trimming
    if (start >= end) {
        throw new IllegalArgumentException(
                "Empty number after trimming spaces and NULs");
    }

    for ( ;start < end; start++) {
        final byte currentByte = buffer[start];
        // CheckStyle:MagicNumber OFF
        if (currentByte < '0' || currentByte > '7'){
            throw new IllegalArgumentException(
                    exceptionMessage(buffer, offset, length, start, currentByte));
        }
        result = (result << 3) + (currentByte - '0'); // convert from ASCII
        // CheckStyle:MagicNumber ON
    }

    return result;
}