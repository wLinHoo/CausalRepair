public BaseSettings withDateFormat(DateFormat df) {
    if (_dateFormat == df) {
        return this;
    }
    // Always use the existing timezone, don't change it based on DateFormat's timezone
    return new BaseSettings(_classIntrospector, _annotationIntrospector, _visibilityChecker, _propertyNamingStrategy, _typeFactory,
            _typeResolverBuilder, df, _handlerInstantiator, _locale,
            _timeZone, _defaultBase64);
}