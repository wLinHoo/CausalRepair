private char getMappingCode(final String str, final int index) {
    final char c = str.charAt(index);
    final char mappedChar = this.map(c);
    // HW rule check
    if (index > 0 && mappedChar != '0') {
        final char prevChar = str.charAt(index - 1);
        if ('H' == prevChar || 'W' == prevChar) {
            final char firstCode;
            // Find the previous non-H/W character
            int i = index - 2;
            while (i >= 0) {
                final char prevNonHW = str.charAt(i);
                if ('H' != prevNonHW && 'W' != prevNonHW) {
                    firstCode = this.map(prevNonHW);
                    if (firstCode == mappedChar) {
                        return 0;
                    }
                    break;
                }
                i--;
            }
        }
    }
    return mappedChar;
}