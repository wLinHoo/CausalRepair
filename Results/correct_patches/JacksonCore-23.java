public DefaultPrettyPrinter createInstance() {
    if (getClass() != DefaultPrettyPrinter.class) {
        throw new IllegalStateException("DefaultPrettyPrinter sub-class "+getClass().getName()+" does not override createInstance(): needs to return DefaultPrettyPrinter instance");
    }
    return new DefaultPrettyPrinter(this);
}