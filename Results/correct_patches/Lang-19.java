public int translate(CharSequence input, int index, Writer out) throws IOException {
    int seqEnd = input.length();
    // Uses -2 to ensure there is something after the &#
    if (input.charAt(index) == '&' && index < seqEnd - 1 && input.charAt(index + 1) == '#') {
        int start = index + 2;
        if (start >= seqEnd) {  // Check if we're already at end after "&#"
            return 0;
        }
        boolean isHex = false;

        char firstChar = input.charAt(start);
        if (firstChar == 'x' || firstChar == 'X') {
            start++;
            isHex = true;
            if (start >= seqEnd) {  // Check if we're at end after "&#x"
                return 0;
            }
        }

        int end = start;
        // Note that this supports character codes without a ; on the end
        while (end < seqEnd && (Character.isDigit(input.charAt(end)) 
                              || (isHex && Character.isLetterOrDigit(input.charAt(end))))) {
            end++;
        }

        // Check we found some digits
        if (end == start) {
            return 0;
        }

        // Check for semicolon only if there is one at the current position
        boolean semiColonFound = false;
        if (end < seqEnd && input.charAt(end) == ';') {
            semiColonFound = true;
            end++;
        }

        int entityValue;
        try {
            if (isHex) {
                entityValue = Integer.parseInt(input.subSequence(start, end - (semiColonFound ? 1 : 0)).toString(), 16);
            } else {
                entityValue = Integer.parseInt(input.subSequence(start, end - (semiColonFound ? 1 : 0)).toString(), 10);
            }
        } catch (NumberFormatException nfe) {
            return 0;
        }

        if (entityValue > 0xFFFF) {
            char[] chrs = Character.toChars(entityValue);
            out.write(chrs[0]);
            out.write(chrs[1]);
        } else {
            out.write(entityValue);
        }

        return end - index;
    }
    return 0;
}