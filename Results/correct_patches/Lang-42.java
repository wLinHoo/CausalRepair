public void escape(Writer writer, String str) throws IOException {
    int len = str.length();
    for (int i = 0; i < len; i++) {
        char c = str.charAt(i);
        String entityName = this.entityName(c);
        if (entityName == null) {
            if (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE) {
                // Handle surrogate pairs
                if (Character.isHighSurrogate(c) && i + 1 < len) {
                    char low = str.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        int codePoint = Character.toCodePoint(c, low);
                        writer.write("&#");
                        writer.write(Integer.toString(codePoint, 10));
                        writer.write(';');
                        i++; // Skip the low surrogate
                        continue;
                    }
                }
                // If we get here, it's an invalid surrogate pair
                writer.write('?');
            } else if (c > 0x7F) {
                writer.write("&#");
                writer.write(Integer.toString(c, 10));
                writer.write(';');
            } else {
                writer.write(c);
            }
        } else {
            writer.write('&');
            writer.write(entityName);
            writer.write(';');
        }
    }
}