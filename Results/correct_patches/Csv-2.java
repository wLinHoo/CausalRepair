public String get(final String name) {
    if (mapping == null) {
        throw new IllegalStateException(
                "No header mapping was specified, the record values can't be accessed by name");
    }
    final Integer index = mapping.get(name);
    if (index != null) {
        if (index >= values.length) {
            throw new IllegalArgumentException(
                    String.format("Mapping for '%s' found but record has only %d values!", name, values.length));
        }
        return values[index.intValue()];
    }
    return null;
}