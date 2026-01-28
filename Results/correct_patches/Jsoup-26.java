public Document clean(Document dirtyDocument) {
    Validate.notNull(dirtyDocument);

    Document clean = Document.createShell(dirtyDocument.baseUri());
    Element body = dirtyDocument.body();
    if (body != null) {  // Only copy nodes if body exists
        copySafeNodes(body, clean.body()); 
    }

    return clean;
}