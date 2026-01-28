static boolean isValidDefineValue(Node val, Set<String> defines) {
  switch (val.getType()) {
    // Primitive literals are always valid
    case Token.STRING:
    case Token.NUMBER:
    case Token.TRUE:
    case Token.FALSE:
      return true;

    // Handle all binary operators
    case Token.ADD:
    case Token.BITAND:
    case Token.BITOR:
    case Token.BITXOR:
      Node left = val.getFirstChild();
      Node right = val.getLastChild();
      return isValidDefineValue(left, defines) && isValidDefineValue(right, defines);

    // Handle unary operators
    case Token.BITNOT:  // BITNOT is unary despite 'BIT' prefix
    case Token.NOT:
    case Token.NEG:
      return isValidDefineValue(val.getFirstChild(), defines);

    // Handle names and properties
    case Token.NAME:
    case Token.GETPROP:
      if (val.isQualifiedName()) {
        return defines.contains(val.getQualifiedName());
      }
      return false;

    // All other cases are invalid
    default:
      return false;
  }
}