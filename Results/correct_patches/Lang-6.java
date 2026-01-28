public final void translate(CharSequence input, Writer out) throws IOException {
    if (out == null) {
        throw new IllegalArgumentException("The Writer must not be null");
    }
    if (input == null) {
        return;
    }
    int pos = 0;
    int len = input.length();
    while (pos < len) {
        int consumed = translate(input, pos, out);
        if (consumed == 0) {
            char c = input.charAt(pos);
            if (Character.isHighSurrogate(c) && pos + 1 < len) {
                char c2 = input.charAt(pos + 1);
                if (Character.isLowSurrogate(c2)) {
                    out.write(new char[]{c, c2});
                    pos += 2;
                    continue;
                }
            }
            out.write(c);
            pos++;
            continue;
        }
        // Handle consumed characters properly considering surrogate pairs
        int endPos = pos;
        for (int pt = 0; pt < consumed && endPos < len; pt++) {
            int codePoint = Character.codePointAt(input, endPos);
            endPos += Character.charCount(codePoint);
        }
        pos = endPos;
    }
}