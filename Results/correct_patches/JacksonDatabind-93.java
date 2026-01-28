public void validateSubType(DeserializationContext ctxt, JavaType type) throws JsonMappingException
{
    // There are certain nasty classes that could cause problems, mostly
    // via default typing -- catch them here.
    final Class<?> raw = type.getRawClass();
    if (raw == null) {
        return; // can't check without class
    }
    String full = raw.getName();

    main_check:
    do {
        if (_cfgIllegalClassNames.contains(full)) {
            break;
        }

        // 18-Dec-2017, tatu: As per [databind#1855], need bit more sophisticated handling
        //    for some Spring framework types
        // 05-Jan-2017, tatu: ... also, only applies to classes, not interfaces
        if (full.startsWith(PREFIX_STRING)) {
            // Skip interfaces and check superclasses only for concrete classes
            if (!raw.isInterface()) {
                Class<?> cls = raw;
                while (cls != null && cls != Object.class) {
                    String name = cls.getSimpleName();
                    // looking for "AbstractBeanFactoryPointcutAdvisor" but no point to allow any is there?
                    if ("AbstractPointcutAdvisor".equals(name)
                            // ditto for "FileSystemXmlApplicationContext": block all ApplicationContexts
                            || "AbstractApplicationContext".equals(name)) {
                        break main_check;
                    }
                    cls = cls.getSuperclass();
                }
            }
        }
        return;
    } while (false);

    throw JsonMappingException.from(ctxt,
            String.format("Illegal type (%s) to deserialize: prevented for security reasons", full));
}