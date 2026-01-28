private static boolean isReduceableFunctionExpression(Node n) {
    if (!NodeUtil.isFunctionExpression(n)) {
        return false;
    }
    
    Node parent = n.getParent();
    if (parent == null) {
        return false;
    }
    
    // Don't reduce getter/setter functions
    if (parent.getType() == Token.GET || parent.getType() == Token.SET) {
        return false;
    }
    
    return true;
}