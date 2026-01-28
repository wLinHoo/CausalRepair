public List<String> getMatchingOptions(String opt)
{
    opt = Util.stripLeadingHyphens(opt);
    
    List<String> matchingOpts = new ArrayList<String>();

    // for a perfect match return the single option only
    for (String longOpt : longOpts.keySet())
    {
        if (longOpt.equals(opt))
        {
            matchingOpts.clear();
            matchingOpts.add(longOpt);
            return matchingOpts;
        }
    }

    for (String longOpt : longOpts.keySet())
    {
        if (longOpt.startsWith(opt))
        {
            matchingOpts.add(longOpt);
        }
    }
    
    return matchingOpts;
}