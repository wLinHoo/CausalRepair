/**
 * <p>Checks if the locale specified is in the list of available locales.</p>
 *
 * @param locale the Locale object to check if it is available
 * @return true if the locale is a known locale
 */
public static boolean isAvailableLocale(Locale locale) {
    if (locale == null) {
        return false;
    }
    return Arrays.asList(Locale.getAvailableLocales()).contains(locale);
}