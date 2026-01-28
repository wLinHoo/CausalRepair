public static boolean isNumber(String str) {
    if (StringUtils.isEmpty(str)) {
        return false;
    }
    char[] chars = str.toCharArray();
    int sz = chars.length;
    boolean hasExp = false;
    boolean hasDecPoint = false;
    boolean allowSigns = false;
    boolean foundDigit = false;
    
    // deal with any possible sign up front
    int start = (chars[0] == '-') ? 1 : 0;
    
    // handle hex numbers
    if (sz > start + 1 && chars[start] == '0' && chars[start + 1] == 'x') {
        int i = start + 2;
        if (i == sz) {
            return false; // str == "0x"
        }
        // checking hex (it can't be anything else)
        for (; i < chars.length; i++) {
            if ((chars[i] < '0' || chars[i] > '9')
                && (chars[i] < 'a' || chars[i] > 'f')
                && (chars[i] < 'A' || chars[i] > 'F')) {
                return false;
            }
        }
        return true;
    }
    
    sz--; // don't want to loop to the last char, check it afterwards
    int i = start;
    
    // loop to the next to last char or to the last char if we need another digit
    while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
        if (chars[i] >= '0' && chars[i] <= '9') {
            foundDigit = true;
            allowSigns = false;
        } else if (chars[i] == '.') {
            if (hasDecPoint || hasExp) {
                return false;
            }
            hasDecPoint = true;
        } else if (chars[i] == 'e' || chars[i] == 'E') {
            if (hasExp || !foundDigit) {
                return false;
            }
            hasExp = true;
            allowSigns = true;
            foundDigit = false; // we need a digit after E
        } else if (chars[i] == '+' || chars[i] == '-') {
            if (!allowSigns) {
                return false;
            }
            allowSigns = false;
        } else {
            return false;
        }
        i++;
    }
    
    if (i < chars.length) {
        if (chars[i] >= '0' && chars[i] <= '9') {
            return true;
        }
        if (chars[i] == 'e' || chars[i] == 'E') {
            return false;
        }
        if (chars[i] == '.') {
            if (hasDecPoint || hasExp || !foundDigit) {
                return false;
            }
            return true;
        }
        if (!allowSigns && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
            return foundDigit;
        }
        if (chars[i] == 'l' || chars[i] == 'L') {
            return foundDigit && !hasExp && !hasDecPoint;
        }
        return false;
    }
    
    return foundDigit && !allowSigns;
}