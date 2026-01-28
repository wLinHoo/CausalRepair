public char[] expandCurrentSegment()
{
    final char[] curr = _currentSegment;
    final int len = curr.length;
    
    // Calculate new length with 50% growth
    int newLen = len + (len >> 1);
    
    // If we hit MAX_SEGMENT_LEN, grow beyond it with same strategy
    if (len >= MAX_SEGMENT_LEN) {
        newLen = len + (len >> 2); // Grow by 25% after hitting max
    }
    
    // Ensure we always grow by at least 1 and handle overflow
    if (newLen <= len) {
        newLen = len + 1;
    }
    
    return (_currentSegment = Arrays.copyOf(curr, newLen));
}