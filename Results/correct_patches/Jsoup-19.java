private boolean testValidProtocol(Element el, Attribute attr, Set<Protocol> protocols) {
    String value = el.absUrl(attr.getKey());
    String originalValue = attr.getValue();

    if (value == null || value.isEmpty()) {
        // If we can't resolve to absolute URL (possibly custom protocol), use original
        value = originalValue;
    }

    // Only update attribute if:
    // 1. We're not preserving relative links AND
    // 2. The resolved URL is actually different from the original (i.e., wasn't already absolute or using custom protocol)
    // 3. And the value is not empty
    if (!preserveRelativeLinks && !value.isEmpty() && !value.equals(originalValue)) {
        attr.setValue(value);
    }

    for (Protocol protocol : protocols) {
        String prot = protocol.toString() + ":";
        if (value.toLowerCase().startsWith(prot)) {
            return true;
        }
    }
    return false;
}