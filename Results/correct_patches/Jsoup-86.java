public XmlDeclaration asXmlDeclaration() {
    String data = getData();
    Document doc = Jsoup.parse("<" + data.substring(1, data.length() -1) + ">", baseUri(), Parser.xmlParser());
    XmlDeclaration decl = null;
    if (doc.childNodeSize() > 0) {
        Node node = doc.childNode(0);
        if (node instanceof Element) {
            Element el = (Element) node;
            decl = new XmlDeclaration(NodeUtils.parser(doc).settings().normalizeTag(el.tagName()), data.startsWith("!"));
            decl.attributes().addAll(el.attributes());
        }
    }
    return decl;
}