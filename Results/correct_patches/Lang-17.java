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
            int cp = Character.codePointAt(input, pos);
            char[] c = Character.toChars(cp);
            out.write(c);
            pos += Character.charCount(cp);
        } else {
            for (int pt = 0; pt < consumed; pt++) {
                int cp = Character.codePointAt(input, pos);
                pos += Character.charCount(cp);
            }
        }
    }
}