private boolean isShortOption(String token)
{
    // short options (-S, -SV, -S=V, -SV1=V2, -S1S2)
    if (!token.startsWith("-") || token.length() < 2) {
        return false;
    }
    
    // Check all chars after '-' are letters (valid short option chars)
    String optPart = token.substring(1);
    int pos = optPart.indexOf('=');
    String optName = pos == -1 ? optPart : optPart.substring(0, pos);
    
    for (int i = 0; i < optName.length(); i++) {
        if (!Character.isLetter(optName.charAt(i))) {
            return false;
        }
    }
    
    return options.hasShortOption(optName);
}