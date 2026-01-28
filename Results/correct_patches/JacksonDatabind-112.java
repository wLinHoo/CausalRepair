public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
        BeanProperty property) throws JsonMappingException
{
    // May need to resolve types for delegate-based creators:
    JsonDeserializer<Object> delegate = null;
    if (_valueInstantiator != null) {
        // Check both array-delegating and regular delegating creators
        JavaType delegateType = null;
        AnnotatedWithParams delegateCreator = _valueInstantiator.getDelegateCreator();
        
        if (delegateCreator != null) {
            delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
        } else {
            // Fall back to array delegate if regular delegate doesn't exist
            delegateCreator = _valueInstantiator.getArrayDelegateCreator();
            if (delegateCreator != null) {
                delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
            }
        }
        
        if (delegateType != null) {
            delegate = findDeserializer(ctxt, delegateType, property);
        }
    }

    JsonDeserializer<?> valueDeser = _valueDeserializer;
    final JavaType valueType = _containerType.getContentType();
    if (valueDeser == null) {
        // [databind#125]: May have a content converter
        valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
        if (valueDeser == null) {
            // And we may also need to get deserializer for String
            valueDeser = ctxt.findContextualValueDeserializer(valueType, property);
        }
    } else { // if directly assigned, probably not yet contextual, so:
        valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, valueType);
    }

    Boolean unwrapSingle = findFormatFeature(ctxt, property, Collection.class,
            JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    NullValueProvider nuller = findContentNullProvider(ctxt, property, valueDeser);
    if (isDefaultDeserializer(valueDeser)) {
        valueDeser = null;
    }
    return withResolved(delegate, valueDeser, nuller, unwrapSingle);
}