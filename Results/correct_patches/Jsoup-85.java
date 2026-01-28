public Attribute(String key, String val, Attributes parent) {
    Validate.notNull(key);
    Validate.notEmpty(key.trim()); // validate trimmed key to catch whitespace-only keys
    this.key = key.trim();
    this.val = val != null ? val : ""; // handle null values by converting to empty string
    this.parent = parent;
}