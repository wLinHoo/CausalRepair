static Document parseByteData(ByteBuffer byteData, String charsetName, String baseUri, Parser parser) {
    String docData;
    Document doc = null;
    
    // First check for BOM - overrides any other header or input
    if (byteData.remaining() > 0) {
        byteData.mark();
        byte bom[] = new byte[4];
        int bomLength = 0;
        if (byteData.remaining() >= bom.length) {
            byteData.get(bom);
            byteData.rewind();
            if (bom[0] == (byte)0x00 && bom[1] == (byte)0x00 && bom[2] == (byte)0xFE && bom[3] == (byte)0xFF) {
                charsetName = "UTF-32BE";
                bomLength = 4;
            } else if (bom[0] == (byte)0xFF && bom[1] == (byte)0xFE && bom[2] == (byte)0x00 && bom[3] == (byte)0x00) {
                charsetName = "UTF-32LE";
                bomLength = 4;
            } else if (bom[0] == (byte)0xFE && bom[1] == (byte)0xFF) {
                charsetName = "UTF-16BE";
                bomLength = 2;
            } else if (bom[0] == (byte)0xFF && bom[1] == (byte)0xFE) {
                charsetName = "UTF-16LE";
                bomLength = 2;
            } else if (bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF) {
                charsetName = "UTF-8";
                bomLength = 3;
            }
        }
        if (bomLength > 0) {
            byteData.position(byteData.position() + bomLength);
            docData = Charset.forName(charsetName).decode(byteData).toString();
            doc = parser.parseInput(docData, baseUri);
            doc.outputSettings().charset(charsetName);
            return doc;
        }
    }

    if (charsetName == null) { // determine from meta. safe parse as UTF-8
        // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
        docData = Charset.forName(defaultCharset).decode(byteData).toString();
        doc = parser.parseInput(docData, baseUri);
        Element meta = doc.select("meta[http-equiv=content-type], meta[charset]").first();
        if (meta != null) { // if not found, will keep utf-8 as best attempt
            String foundCharset = null;
            if (meta.hasAttr("http-equiv")) {
                foundCharset = getCharsetFromContentType(meta.attr("content"));
            }
            if (foundCharset == null && meta.hasAttr("charset")) {
                try {
                    if (Charset.isSupported(meta.attr("charset"))) {
                        foundCharset = meta.attr("charset");
                    }
                } catch (IllegalCharsetNameException e) {
                    foundCharset = null;
                }
            }

            if (foundCharset != null && foundCharset.length() != 0 && !foundCharset.equals(defaultCharset)) { // need to re-decode
                foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                charsetName = foundCharset;
                byteData.rewind();
                docData = Charset.forName(foundCharset).decode(byteData).toString();
                doc = null;
            }
        }
    } else { // specified by content type header (or by user on file load)
        Validate.notEmpty(charsetName, "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");
        docData = Charset.forName(charsetName).decode(byteData).toString();
    }

    if (doc == null) {
        doc = parser.parseInput(docData, baseUri);
        doc.outputSettings().charset(charsetName);
    }
    return doc;
}