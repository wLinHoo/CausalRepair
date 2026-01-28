protected final boolean _add(Annotation ann) {
    if (_annotations == null) {
        _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
    }
    Class<? extends Annotation> type = ann.annotationType();
    Annotation previous = _annotations.get(type);
    if (previous == null) {
        _annotations.put(type, ann);
        return true;
    }
    // Don't replace existing annotation unless new one is different
    if (!previous.equals(ann)) {
        _annotations.put(type, ann);
    }
    return false;
}