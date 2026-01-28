public void close() throws IOException {
    if (!this.closed) {
        try {
            finish();
            super.close();
        } finally {
            this.closed = true;
        }
    }
}