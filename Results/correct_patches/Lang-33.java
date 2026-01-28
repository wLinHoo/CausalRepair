public static Class<?>[] toClass(Object[] array) {
    if (array == null) {
        return null;
    } else if (array.length == 0) {
        return ArrayUtils.EMPTY_CLASS_ARRAY;
    }
    Class<?>[] classes = new Class[array.length];
    for (int i = 0; i < array.length; i++) {
        classes[i] = (array[i] != null) ? array[i].getClass() : null; // Fixed: Check for null before calling getClass()
    }
    return classes;
}