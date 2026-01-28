private void checkRequiredOptions()
    throws MissingOptionException
{
    // if there are required options that have not been
    // processsed
    if (requiredOptions.size() > 0)
    {
        Iterator iter = requiredOptions.iterator();
        StringBuffer buff = new StringBuffer();

        // Add the appropriate prefix based on number of missing options
        if (requiredOptions.size() == 1) {
            buff.append("Missing required option: ");
        } else {
            buff.append("Missing required options: ");
        }

        // loop through the required options
        while (iter.hasNext())
        {
            buff.append(iter.next());
        }

        throw new MissingOptionException(buff.toString());
    }
}