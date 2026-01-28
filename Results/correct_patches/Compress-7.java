public static String parseName(byte[] buffer, final int offset, final int length) {
    StringBuffer result = new StringBuffer(length);
    int          end = offset + length;

    for (int i = offset; i < end; ++i) {
        if (buffer[i] == 0) {
            break;
        }
        result.append((char) (buffer[i] & 0xFF)); // Mask with 0xFF to handle signed bytes
    }

    return result.toString();
}