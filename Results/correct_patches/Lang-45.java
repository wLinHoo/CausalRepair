public static String abbreviate(String str, int lower, int upper, String appendToEnd) {
    // initial parameter checks
    if (str == null) {
        return null;
    }
    if (str.length() == 0) {
        return StringUtils.EMPTY;
    }

    // Adjust lower and upper bounds to be within string length
    lower = Math.min(lower, str.length());
    upper = upper == -1 ? str.length() : Math.min(upper, str.length());
    
    // Ensure upper is not less than lower
    upper = Math.max(upper, lower);

    StringBuffer result = new StringBuffer();
    int index = StringUtils.indexOf(str, " ", lower);
    if (index == -1) {
        result.append(str.substring(0, upper));
        // only if abbreviation has occurred do we append the appendToEnd value
        if (upper != str.length()) {
            result.append(StringUtils.defaultString(appendToEnd));
        }
    } else if (index > upper) {
        result.append(str.substring(0, upper));
        result.append(StringUtils.defaultString(appendToEnd));
    } else {
        result.append(str.substring(0, index));
        result.append(StringUtils.defaultString(appendToEnd));
    }
    return result.toString();
}