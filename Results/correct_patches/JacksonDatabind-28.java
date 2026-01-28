public ObjectNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
{
    JsonToken t = p.getCurrentToken();
    
    // Handle both cases where we might start with START_OBJECT or already inside an object
    if (t == JsonToken.START_OBJECT) {
        // Check for empty object case
        if (p.nextToken() == JsonToken.END_OBJECT) {
            return ctxt.getNodeFactory().objectNode();
        }
        return deserializeObject(p, ctxt, ctxt.getNodeFactory());
    }
    
    // Handle cases where we're already at a FIELD_NAME (non-empty object) 
    // or END_OBJECT (empty object when parser already advanced)
    if (t == JsonToken.FIELD_NAME) {
        return deserializeObject(p, ctxt, ctxt.getNodeFactory());
    }
    if (t == JsonToken.END_OBJECT) {
        return ctxt.getNodeFactory().objectNode();
    }
    
    throw ctxt.mappingException(ObjectNode.class);
}