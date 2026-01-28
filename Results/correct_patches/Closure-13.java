private void traverse(Node node) {
  // The goal here is to avoid retraversing
  // the entire AST to catch newly created opportunities.
  // So we track whether a "unit of code" has changed,
  // and revisit immediately.
  if (!shouldVisit(node)) {
    return;
  }

  int visits = 0;
  do {
    // Get first child before starting
    Node c = node.getFirstChild();
    while(c != null) {
      // Get next sibling BEFORE traversing current child
      // in case traversal modifies the sibling list
      Node next = c.getNext();
      traverse(c);
      c = next;
    }

    visit(node);
    visits++;

    Preconditions.checkState(visits < 10000, "too many interations");
  } while (shouldRetraverse(node));

  exitNode(node);
}