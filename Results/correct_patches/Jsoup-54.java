private void copyAttributes(org.jsoup.nodes.Node source, Element el) {
    for (Attribute attribute : source.attributes()) {
        // valid xml attribute names are: ^[a-zA-Z_:][-a-zA-Z0-9_:.]
        String key = attribute.getKey().replaceAll("[^-a-zA-Z0-9_:.]", "");
        // Ensure key starts with valid character and is not empty
        if (!key.isEmpty() && key.matches("^[a-zA-Z_:].*")) {
            el.setAttribute(key, attribute.getValue());
        }
    }
}