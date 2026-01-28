protected void addChildren(int index, Node... children) {
    Validate.noNullElements(children);
    ensureChildNodes();
    
    // First reparent all children
    for (Node child : children) {
        reparentChild(child);
    }
    
    // Add all children at once
    for (int i = 0; i < children.length; i++) {
        childNodes.add(index + i, children[i]);
    }
    
    // Reindex all children starting from the insertion point
    reindexChildren(index);
}