public String generateToolTipFragment(String toolTipText) {
    String escapedText = toolTipText
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
    return " title=\"" + escapedText 
        + "\" alt=\"\"";
}