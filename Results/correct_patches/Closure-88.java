private VariableLiveness isVariableReadBeforeKill(
    Node n, String variable) {
  if (NodeUtil.isName(n) && variable.equals(n.getString())) {
    if (NodeUtil.isLhs(n, n.getParent())) {
      // First check children (RHS) for possible reads before killing
      Node parent = n.getParent();
      if (parent.getType() == Token.ASSIGN && parent.getFirstChild() == n) {
        VariableLiveness rhsState = isVariableReadBeforeKill(parent.getLastChild(), variable);
        if (rhsState != VariableLiveness.MAYBE_LIVE) {
          return rhsState;
        }
      }
      return VariableLiveness.KILL;
    } else {
      return VariableLiveness.READ;
    }
  }

  // Expressions are evaluated left-right, depth first.
  for (Node child = n.getFirstChild();
      child != null; child = child.getNext()) {
    if (!ControlFlowGraph.isEnteringNewCfgNode(child)) { // Not a FUNCTION
      VariableLiveness state = isVariableReadBeforeKill(child, variable);
      if (state != VariableLiveness.MAYBE_LIVE) {
        return state;
      }
    }
  }
  return VariableLiveness.MAYBE_LIVE;
}