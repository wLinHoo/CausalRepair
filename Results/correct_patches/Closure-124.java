private boolean isSafeReplacement(Node node, Node replacement) {
  // No checks are needed for simple names.
  if (node.isName()) {
    return true;
  }
  Preconditions.checkArgument(node.isGetProp());

  // Recursively check all property access nodes in the chain
  Node current = node;
  while (current != null && current.isGetProp()) {
    Node child = current.getFirstChild();
    if (child.isName() && isNameAssignedTo(child.getString(), replacement)) {
      return false;
    }
    current = child;
  }

  return true;
}