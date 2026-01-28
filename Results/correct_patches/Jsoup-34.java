int nextIndexOf(CharSequence seq) {
    // doesn't handle scanning for surrogates
    char startChar = seq.charAt(0);
    int seqLength = seq.length();
    
    for (int offset = pos; offset <= length - seqLength; offset++) {
        // scan to first instance of startchar:
        if (startChar != input[offset])
            while(++offset < length && startChar != input[offset]);
        
        // we might have advanced past possible matches
        if (offset > length - seqLength)
            break;

        boolean matched = true;
        for (int j = 1; j < seqLength; j++) {
            if (input[offset + j] != seq.charAt(j)) {
                matched = false;
                break;
            }
        }
        if (matched) // found full sequence
            return offset - pos;
    }
    return -1;
}