static int toJavaVersionInt(String version) { // Fixed: changed return type from float to int
    return toVersionInt(toJavaVersionIntArray(version, JAVA_VERSION_TRIM_SIZE));
}