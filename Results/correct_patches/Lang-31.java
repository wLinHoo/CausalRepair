public static boolean containsAny(CharSequence cs, char[] searchChars) {
    if (isEmpty(cs) || ArrayUtils.isEmpty(searchChars)) {
        return false;
    }
    int csLength = cs.length();
    int searchLength = searchChars.length;
    for (int i = 0; i < csLength; i++) {
        char ch = cs.charAt(i);
        for (int j = 0; j < searchLength; j++) {
            if (searchChars[j] == ch) {
                // Check if we're dealing with supplementary characters
                if (Character.isHighSurrogate(ch)) {
                    if (j < searchLength - 1) {
                        if (i < csLength - 1 && searchChars[j + 1] == cs.charAt(i + 1)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
        }
    }
    return false;
}