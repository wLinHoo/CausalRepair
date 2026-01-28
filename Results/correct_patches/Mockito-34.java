public void captureArgumentsFrom(Invocation i) {
    int k = 0;
    Object[] arguments = i.getArguments();
    for (Matcher m : matchers) {
        if (m instanceof CapturesArguments && k < arguments.length) {  // Fixed: Add bounds check
            ((CapturesArguments) m).captureFrom(arguments[k]);
        }
        k++;
    }
}