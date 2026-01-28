private void popStackToClose(Token.EndTag endTag) {
    String elName = endTag.name().toLowerCase(); // Normalize to lowercase for case-insensitive matching
    Element firstFound = null;

    for (int pos = stack.size() -1; pos >= 0; pos--) {
        Element next = stack.get(pos);
        if (next.nodeName().toLowerCase().equals(elName)) { // Compare lowercase versions
            firstFound = next;
            break;
        }
    }
    if (firstFound == null)
        return; // not found, skip

    for (int pos = stack.size() -1; pos >= 0; pos--) {
        Element next = stack.get(pos);
        stack.remove(pos);
        if (next == firstFound)
            break;
    }
}