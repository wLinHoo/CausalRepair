public String caverphone(String txt) {
    // NOTE: Version 1.0 of Caverphone is easily derivable from this code 
    // by commenting out the 2.0 lines and adding in the 1.0 lines

    if( txt == null || txt.length() == 0 ) {
        return "1111111111";
    }

    // 1. Convert to lowercase
    txt = txt.toLowerCase(java.util.Locale.ENGLISH);

    // 2. Remove anything not A-Z
    txt = txt.replaceAll("[^a-z]", "");

    // 2.5. Remove final e
    txt = txt.replaceAll("e$", "");             // 2.0 only

    // 3. Handle various start options
    txt = txt.replaceAll("^cough", "cou2f");
    txt = txt.replaceAll("^rough", "rou2f");
    txt = txt.replaceAll("^tough", "tou2f");
    txt = txt.replaceAll("^enough", "enou2f");  // 2.0 only
    txt = txt.replaceAll("^trough", "trou2f");  // 2.0 only
    txt = txt.replaceAll("^gn", "2n");

    // 3.5. Handle mb at the end
    txt = txt.replaceAll("mb$", "m2");
    // Standard mb handling - convert b to p except when at end
    txt = txt.replaceAll("b", "p");

    // 4. Handle replacements
    txt = txt.replaceAll("cq", "2q");
    txt = txt.replaceAll("ci", "si");
    txt = txt.replaceAll("ce", "se");
    txt = txt.replaceAll("cy", "sy");
    txt = txt.replaceAll("tch", "2ch");
    txt = txt.replaceAll("c", "k");
    txt = txt.replaceAll("q", "k");
    txt = txt.replaceAll("x", "k");
    txt = txt.replaceAll("v", "f");
    txt = txt.replaceAll("dg", "2g");
    txt = txt.replaceAll("tio", "sio");
    txt = txt.replaceAll("tia", "sia");
    txt = txt.replaceAll("d", "t");
    txt = txt.replaceAll("ph", "fh");
    txt = txt.replaceAll("sh", "s2");
    txt = txt.replaceAll("z", "s");
    txt = txt.replaceAll("^[aeiou]", "A");
    txt = txt.replaceAll("[aeiou]", "3");
    txt = txt.replaceAll("j", "y");        // 2.0 only
    txt = txt.replaceAll("^y3", "Y3");     // 2.0 only
    txt = txt.replaceAll("^y", "A");       // 2.0 only
    txt = txt.replaceAll("y", "3");        // 2.0 only
    txt = txt.replaceAll("3gh3", "3kh3");
    txt = txt.replaceAll("gh", "22");
    txt = txt.replaceAll("g", "k");
    txt = txt.replaceAll("s+", "S");
    txt = txt.replaceAll("t+", "T");
    txt = txt.replaceAll("p+", "P");
    txt = txt.replaceAll("k+", "K");
    txt = txt.replaceAll("f+", "F");
    txt = txt.replaceAll("m+", "M");
    txt = txt.replaceAll("n+", "N");
    txt = txt.replaceAll("w3", "W3");
    txt = txt.replaceAll("wh3", "Wh3");
    txt = txt.replaceAll("w$", "3");       // 2.0 only
    txt = txt.replaceAll("w", "2");
    txt = txt.replaceAll("^h", "A");
    txt = txt.replaceAll("h", "2");
    txt = txt.replaceAll("r3", "R3");
    txt = txt.replaceAll("r$", "3");       // 2.0 only
    txt = txt.replaceAll("r", "2");
    txt = txt.replaceAll("l3", "L3");
    txt = txt.replaceAll("l$", "3");       // 2.0 only
    txt = txt.replaceAll("l", "2");

    // 5. Handle removals
    txt = txt.replaceAll("2", "");
    txt = txt.replaceAll("3$", "A");       // 2.0 only
    txt = txt.replaceAll("3", "");

    // 6. put ten 1s on the end
    txt = txt + "1111111111";

    // 7. take the first ten characters as the code
    return txt.substring(0, 10);
}