public void captureArgumentsFrom(Invocation invocation) {
    if (invocation.getMethod().isVarArgs()) {
        int indexOfVararg = invocation.getRawArguments().length - 1;
        Object varargArray = invocation.getRawArguments()[indexOfVararg];
        
        // Handle vararg case first
        for (int position = 0; position < matchers.size(); position++) {
            Matcher m = matchers.get(position);
            if (m instanceof CapturesArguments) {
                if (position >= indexOfVararg) {
                    // This is a vararg position
                    int varargIndex = position - indexOfVararg;
                    if (varargArray != null && 
                        varargIndex < Array.getLength(varargArray)) {
                        ((CapturesArguments) m).captureFrom(Array.get(varargArray, varargIndex));
                    }
                } else {
                    // Regular argument position
                    if (position < invocation.getArguments().length) {
                        ((CapturesArguments) m).captureFrom(invocation.getArgumentAt(position, Object.class));
                    }
                }
            }
        }
    } else {
        // Non-vararg case
        for (int position = 0; position < matchers.size(); position++) {
            Matcher m = matchers.get(position);
            if (m instanceof CapturesArguments && 
                position < invocation.getArguments().length) {
                ((CapturesArguments) m).captureFrom(invocation.getArgumentAt(position, Object.class));
            }
        }
    }
}