public Element clone() {
    Element clone = (Element) super.clone();
    // Deep clone the attributes to ensure separation between original and clone
    if (clone.attributes != null) {
        clone.attributes = clone.attributes.clone();
        // Deep clone the class names set if it exists
        if (clone.attributes.hasKey("class")) {
            Set<String> classNames = new HashSet<String>(classNames());
            clone.attributes.put("class", StringUtil.join(classNames, " "));
            // Ensure the classNames() method will return a new set
            clone.classNames = null; // Reset the cached classNames Set
        }
    }
    return clone;
}