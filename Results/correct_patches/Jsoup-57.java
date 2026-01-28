public void removeIgnoreCase(String key) {
    Validate.notEmpty(key);
    if (attributes == null)
        return;
    for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext(); ) {
        String attrKey = it.next();
        if (attrKey.equalsIgnoreCase(key))
            it.remove(); // Fixed: Using iterator's remove method instead of map's remove
    }
}