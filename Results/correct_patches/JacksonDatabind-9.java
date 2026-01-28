public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
    String str;
    if (value instanceof Date) {
        provider.defaultSerializeDateKey((Date) value, jgen);
        return;
    } else if (value instanceof Class) { // Handle Class objects specially
        str = ((Class<?>) value).getName();
    } else {
        str = value.toString();
    }
    jgen.writeFieldName(str);
}