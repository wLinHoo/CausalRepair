JSType meet(JSType that) {
    UnionTypeBuilder builder = new UnionTypeBuilder(registry);
    
    // Add our alternates that are subtypes of 'that'
    for (JSType alternate : alternates) {
        if (alternate.isSubtype(that)) {
            builder.addAlternate(alternate);
        }
    }

    // Add their alternates that are subtypes of us (if 'that' is a union)
    if (that instanceof UnionType) {
        for (JSType otherAlternate : ((UnionType) that).alternates) {
            if (otherAlternate.isSubtype(this)) {
                builder.addAlternate(otherAlternate);
            }
        }
    }
    // Or add 'that' directly if it's a subtype of us (and not a union)
    else if (that.isSubtype(this)) {
        builder.addAlternate(that);
    }

    JSType result = builder.build();
    
    // Handle empty result cases
    if (result == null || result.isNoType()) {
        if (this.isObject() && that.isObject()) {
            return getNativeType(JSTypeNative.NO_OBJECT_TYPE);
        }
        return getNativeType(JSTypeNative.NO_TYPE);
    }
    
    return result;
}