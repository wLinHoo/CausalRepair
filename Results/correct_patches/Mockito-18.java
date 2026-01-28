Object returnValueFor(Class<?> type) {
    if (Primitives.isPrimitiveOrWrapper(type)) {
        return Primitives.defaultValueForPrimitiveOrWrapper(type);
        //new instances are used instead of Collections.emptyList(), etc.
        //to avoid UnsupportedOperationException if code under test modifies returned collection
    }
    
    // Handle List/Iterable types
    if (type == Iterable.class || type == Collection.class || 
        type == List.class || type == LinkedList.class) {
        return new LinkedList<Object>();
    } else if (type == ArrayList.class) {
        return new ArrayList<Object>();
    }
    
    // Handle Set types
    if (type == Set.class || type == HashSet.class) {
        return new HashSet<Object>();
    } else if (type == SortedSet.class || type == TreeSet.class) {
        return new TreeSet<Object>();
    } else if (type == LinkedHashSet.class) {
        return new LinkedHashSet<Object>();
    }
    
    // Handle Map types
    if (type == Map.class || type == HashMap.class) {
        return new HashMap<Object, Object>();
    } else if (type == SortedMap.class || type == TreeMap.class) {
        return new TreeMap<Object, Object>();
    } else if (type == LinkedHashMap.class) {
        return new LinkedHashMap<Object, Object>();
    }
    
    // Let's not care about the rest of collections.
    return null;
}