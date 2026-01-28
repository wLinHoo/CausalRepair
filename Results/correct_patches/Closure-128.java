static boolean isSimpleNumber(String s) {
    int len = s.length();
    if (len == 0) {
        return false;
    }
    for (int index = 0; index < len; index++) {
        char c = s.charAt(index);
        if (c < '0' || c > '9') {
            return false;
        }
    }
    // Allow numbers starting with '0' as long as they're not just "0"
    return len == 1 || s.charAt(0) != '0';
}