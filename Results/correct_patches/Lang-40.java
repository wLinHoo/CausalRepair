public static boolean containsIgnoreCase(String str, String searchStr) {
    if (str == null || searchStr == null) {
        return false;
    }
    final int len = searchStr.length();
    if (len == 0) {
        return true;
    }
    
    for (int i = 0; i <= str.length() - len; i++) {
        if (str.regionMatches(true, i, searchStr, 0, len)) {
            return true;
        }
    }
    return false;
}