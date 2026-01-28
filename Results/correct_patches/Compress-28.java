public int read(byte[] buf, int offset, int numToRead) throws IOException {
    int totalRead = 0;

    if (hasHitEOF || entryOffset >= entrySize) {
        return -1;
    }

    if (currEntry == null) {
        throw new IllegalStateException("No current tar entry");
    }

    numToRead = Math.min(numToRead, available());
    
    // Ensure we don't read beyond the entry size
    if (entryOffset + numToRead > entrySize) {
        numToRead = (int)(entrySize - entryOffset);
    }
    
    totalRead = is.read(buf, offset, numToRead);
    
    if (totalRead == -1) {
        if (numToRead > 0) {
            // We expected to read data but got EOF - this is a truncated entry
            throw new IOException("Truncated TAR archive");
        }
        hasHitEOF = true;
        entryOffset = entrySize; // Ensure we don't try to read more
    } else {
        count(totalRead);
        entryOffset += totalRead;
        
        // Check if we got less than requested which might indicate truncation
        if (totalRead < numToRead && entryOffset < entrySize) {
            throw new IOException("Truncated TAR archive");
        }
    }

    return totalRead;
}