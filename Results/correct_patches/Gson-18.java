static Type getSupertype(Type context, Class<?> contextRawType, Class<?> supertype) {
    // Handle wildcards by getting their upper bounds
    if (context instanceof WildcardType) {
        context = ((WildcardType) context).getUpperBounds()[0];
    }
    
    checkArgument(supertype.isAssignableFrom(contextRawType));
    Type supertypeToken = $Gson$Types.getGenericSupertype(context, contextRawType, supertype);
    return resolve(context, contextRawType, supertypeToken);
}