public Fraction parse(String source, ParsePosition pos) {
    // try to parse improper fraction
    Fraction ret = super.parse(source, pos);
    if (ret != null) {
        return ret;
    }
    
    int initialIndex = pos.getIndex();

    // parse whitespace
    parseAndIgnoreWhitespace(source, pos);

    // parse whole
    Number whole = getWholeFormat().parse(source, pos);
    if (whole == null) {
        // invalid integer number
        pos.setIndex(initialIndex);
        return null;
    }

    // parse whitespace
    parseAndIgnoreWhitespace(source, pos);
    
    // parse numerator
    int numStart = pos.getIndex();
    Number num = getNumeratorFormat().parse(source, pos);
    if (num == null) {
        pos.setIndex(initialIndex);
        return null;
    }
    
    // Check if numerator has embedded minus sign
    String numeratorPart = source.substring(numStart, pos.getIndex());
    if (numeratorPart.contains("-")) {
        pos.setIndex(initialIndex);
        return null;
    }

    // parse '/'
    int startIndex = pos.getIndex();
    char c = parseNextCharacter(source, pos);
    switch (c) {
    case 0 :
        // no '/'
        return new Fraction(num.intValue(), 1);
    case '/' :
        // found '/', continue parsing denominator
        break;
    default :
        pos.setIndex(initialIndex);
        pos.setErrorIndex(startIndex);
        return null;
    }

    // parse whitespace
    parseAndIgnoreWhitespace(source, pos);

    // parse denominator
    int denStart = pos.getIndex();
    Number den = getDenominatorFormat().parse(source, pos);
    if (den == null) {
        pos.setIndex(initialIndex);
        return null;
    }
    
    // Check if denominator has embedded minus sign
    String denominatorPart = source.substring(denStart, pos.getIndex());
    if (denominatorPart.contains("-")) {
        pos.setIndex(initialIndex);
        return null;
    }
    
    int w = whole.intValue();
    int n = num.intValue();
    int d = den.intValue();
    return new Fraction(((Math.abs(w) * d) + n) * MathUtils.sign(w), d);
}