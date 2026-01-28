protected StringBuffer renderWrappedText(StringBuffer sb, int width, 
                                         int nextLineTabStop, String text)
{
    int pos = findWrapPos(text, width, 0);

    if (pos == -1)
    {
        sb.append(rtrim(text));

        return sb;
    }
    sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);

    // Modified the condition to handle the case where nextLineTabStop is too large
    if (nextLineTabStop >= width)
    {
        // Use 1 instead of width-1 to prevent over-padding
        nextLineTabStop = 1;
    }

    // all following lines must be padded with nextLineTabStop space 
    // characters
    final String padding = createPadding(nextLineTabStop);

    while (true)
    {
        text = padding + text.substring(pos).trim();
        pos = findWrapPos(text, width, 0);

        if (pos == -1)
        {
            sb.append(text);

            return sb;
        }
        
        if ( (text.length() > width) && (pos == nextLineTabStop - 1) ) 
        {
            pos = width;
        }

        sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);
    }
}