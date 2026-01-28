public int read() throws IOException {
    int current = super.read();
    
    if (current == '\n') {
        // Only increment if previous character wasn't CR
        if (lastChar != '\r') {
            lineCounter++;
        }
    } else if (current == '\r') {
        // Always increment for CR as we don't know if LF will follow
        lineCounter++;
    }
    
    lastChar = current;
    return lastChar;
}