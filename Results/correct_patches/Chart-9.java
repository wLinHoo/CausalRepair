public TimeSeries createCopy(RegularTimePeriod start, RegularTimePeriod end)
    throws CloneNotSupportedException {

    if (start == null) {
        throw new IllegalArgumentException("Null 'start' argument.");
    }
    if (end == null) {
        throw new IllegalArgumentException("Null 'end' argument.");
    }
    if (start.compareTo(end) > 0) {
        throw new IllegalArgumentException(
                "Requires start on or before end.");
    }
    
    boolean emptyRange = false;
    int startIndex = getIndex(start);
    if (startIndex < 0) {
        startIndex = -(startIndex + 1);
        if (startIndex >= this.data.size()) {
            emptyRange = true;  // start is after last data item
        }
    }
    int endIndex = getIndex(end);
    if (endIndex < 0) {
        endIndex = -(endIndex + 1);
        endIndex = endIndex - 1;
        if (endIndex < 0 || endIndex < startIndex) {
            emptyRange = true;  // end is before first data item or before start
        }
    }
    
    if (emptyRange || startIndex > endIndex) {
        TimeSeries copy = (TimeSeries) super.clone();
        copy.data = new java.util.ArrayList();
        return copy;
    }
    else {
        return createCopy(startIndex, endIndex);
    }
}