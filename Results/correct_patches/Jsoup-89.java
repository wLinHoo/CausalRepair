public String setValue(String val) {
    String oldVal = this.val; // Get the current value before changing it
    if (parent != null) {
        oldVal = parent.get(this.key); // Only try to get from parent if it exists
        int i = parent.indexOfKey(this.key);
        if (i != Attributes.NotFound)
            parent.vals[i] = val;
    }
    this.val = val;
    return Attributes.checkNotNull(oldVal);
}