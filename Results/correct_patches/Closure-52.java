static boolean isSimpleNumber(String s) {
  int len = s.length();
  // Empty string isn't a number
  if (len == 0) {
    return false;
  }
  // Strings with leading zeros aren't simple numbers unless it's just "0"
  if (s.charAt(0) == '0' && len > 1) {
    return false;
  }
  for (int index = 0; index < len; index++) {
    char c = s.charAt(index);
    if (c < '0' || c > '9') {
      return false;
    }
  }
  return true;
}