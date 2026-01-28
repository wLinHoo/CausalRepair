private boolean toStringEquals(Matcher m, Object arg) {
    String matcherString = StringDescription.toString(m);
    String argString = (arg == null) ? "null" : arg.toString();
    return matcherString.equals(argString);
}