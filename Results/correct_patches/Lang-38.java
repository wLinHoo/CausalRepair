public StringBuffer format(Calendar calendar, StringBuffer buf) {
    if (mTimeZoneForced) {
        calendar = (Calendar) calendar.clone();
        // Get the time in milliseconds before changing timezone
        long millis = calendar.getTimeInMillis();
        calendar.setTimeZone(mTimeZone);
        // Set the time again to adjust for new timezone
        calendar.setTimeInMillis(millis);
    }
    return applyRules(calendar, buf);
}