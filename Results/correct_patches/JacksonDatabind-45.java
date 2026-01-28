public JsonSerializer<?> createContextual(SerializerProvider serializers,
        BeanProperty property) throws JsonMappingException
{
    if (property != null) {
        JsonFormat.Value format = serializers.getAnnotationIntrospector().findFormat((Annotated)property.getMember());
        if (format != null) {
            // Check for explicit numeric shape first
            if (format.getShape().isNumeric()) {
                return withFormat(Boolean.TRUE, null);
            }
            
            // Handle string shape or any shape with pattern/timezone/locale
            if (format.getShape() == JsonFormat.Shape.STRING || format.hasPattern()
                    || format.hasLocale() || format.hasTimeZone()) {
                TimeZone tz = format.getTimeZone();
                final String pattern = format.hasPattern()
                        ? format.getPattern()
                        : StdDateFormat.DATE_FORMAT_STR_ISO8601;
                final Locale loc = format.hasLocale()
                        ? format.getLocale()
                        : serializers.getLocale();
                SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
                if (tz == null) {
                    tz = serializers.getTimeZone();
                }
                df.setTimeZone(tz);
                return withFormat(Boolean.FALSE, df);
            }
            
            // For ANY shape without pattern/locale/timezone, use defaults
            return this;
        }
    }
    // If no format specified at all, use defaults
    return this;
}