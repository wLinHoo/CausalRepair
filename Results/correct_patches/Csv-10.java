public CSVPrinter(final Appendable out, final CSVFormat format) throws IOException {
    Assertions.notNull(out, "out");
    Assertions.notNull(format, "format");

    this.out = out;
    this.format = format;
    this.format.validate();
    
    // Print header immediately if one is specified
    if (format.getHeader() != null) {
        this.printHeader();
    }
}

/**
 * Prints the header of the CSV file.
 * 
 * @throws IOException If an I/O error occurs
 */
private void printHeader() throws IOException {
    if (this.format.getHeader() != null && this.format.getHeader().length > 0) {
        this.printRecord((Object[]) this.format.getHeader());
    }
}