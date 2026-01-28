private StringBuffer appendQuotedString(String pattern, ParsePosition pos,
        StringBuffer appendTo, boolean escapingOn) {
    int start = pos.getIndex();
    char[] c = pattern.toCharArray();
    
    // Handle immediate quote at start position
    if (escapingOn && c[start] == QUOTE) {
        next(pos);
        return appendTo == null ? null : appendTo.append(QUOTE);
    }
    
    int lastHold = start;
    int i = start;
    while (i < pattern.length()) {
        // Check for escaped quote
        if (escapingOn && i + 1 < pattern.length() && c[i] == QUOTE && c[i+1] == QUOTE) {
            appendTo.append(c, lastHold, i - lastHold).append(QUOTE);
            i += 2;
            pos.setIndex(i);
            lastHold = i;
            continue;
        }
        
        // Check for closing quote
        if (c[i] == QUOTE) {
            next(pos);
            return appendTo.append(c, lastHold, pos.getIndex() - lastHold);
        }
        
        // Move to next character
        next(pos);
        i = pos.getIndex();
    }
    
    throw new IllegalArgumentException(
            "Unterminated quoted string at position " + start);
}