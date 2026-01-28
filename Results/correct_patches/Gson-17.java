public Date read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
    }
    
    if (in.peek() != JsonToken.STRING) {
        // This matches Gson's convention of throwing IllegalStateException for unexpected tokens
        throw new IllegalStateException("Expected a string but was " + in.peek());
    }
    
    try {
        Date date = deserializeToDate(in.nextString());
        if (dateType == Date.class) {
            return date;
        } else if (dateType == Timestamp.class) {
            return new Timestamp(date.getTime());
        } else if (dateType == java.sql.Date.class) {
            return new java.sql.Date(date.getTime());
        } else {
            // This must never happen: dateType is guarded in the primary constructor
            throw new AssertionError();
        }
    } catch (Exception e) {
        // Wrap any parsing/deserialization errors in JsonParseException
        throw new JsonParseException("Failed parsing date: " + e.getMessage(), e);
    }
}