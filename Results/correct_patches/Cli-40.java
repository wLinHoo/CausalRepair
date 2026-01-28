public static <T> T createValue(final String str, final Class<T> clazz) throws ParseException
{
    if (clazz == null)
    {
        throw new ParseException("Class argument cannot be null");
    }
    else if (clazz.equals(PatternOptionBuilder.STRING_VALUE))
    {
        return clazz.cast(str);
    }
    else if (clazz.equals(PatternOptionBuilder.OBJECT_VALUE))
    {
        return clazz.cast(createObject(str));
    }
    else if (clazz.equals(PatternOptionBuilder.NUMBER_VALUE) || Number.class.isAssignableFrom(clazz))
    {
        return clazz.cast(createNumber(str));
    }
    else if (clazz.equals(PatternOptionBuilder.DATE_VALUE))
    {
        return clazz.cast(createDate(str));
    }
    else if (clazz.equals(PatternOptionBuilder.CLASS_VALUE))
    {
        return clazz.cast(createClass(str));
    }
    else if (clazz.equals(PatternOptionBuilder.FILE_VALUE))
    {
        return clazz.cast(createFile(str));
    }
    else if (clazz.equals(PatternOptionBuilder.EXISTING_FILE_VALUE))
    {
        return clazz.cast(openFile(str));
    }
    else if (clazz.equals(PatternOptionBuilder.FILES_VALUE))
    {
        return clazz.cast(createFiles(str));
    }
    else if (clazz.equals(PatternOptionBuilder.URL_VALUE))
    {
        return clazz.cast(createURL(str));
    }
    else
    {
        throw new ParseException("Unsupported type: " + clazz.getName());
    }
}