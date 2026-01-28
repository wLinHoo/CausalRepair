protected String[] flatten(Options options, String[] arguments, boolean stopAtNonOption)
{
    List tokens = new ArrayList();

    boolean eatTheRest = false;

    for (int i = 0; i < arguments.length; i++)
    {
        String arg = arguments[i];

        if ("--".equals(arg))
        {
            eatTheRest = true;
            tokens.add("--");
        }
        else if ("-".equals(arg))
        {
            tokens.add("-");
        }
        else if (arg.startsWith("-"))
        {
            String opt = Util.stripLeadingHyphens(arg);

            // Handle options with equals sign (=) for values
            if (opt.indexOf('=') != -1) {
                String[] parts = arg.split("=", 2);
                if (options.hasOption(Util.stripLeadingHyphens(parts[0]))) {
                    tokens.add(parts[0]);
                    tokens.add(parts[1]);
                    continue;
                }
            }

            if (options.hasOption(opt))
            {
                tokens.add(arg);
            }
            else
            {
                // Handle special case for single-character options
                if (arg.length() >= 2 && options.hasOption(arg.substring(1, 2)))
                {
                    // Handle property-style options (-Dproperty=value)
                    tokens.add(arg.substring(0, 2)); // -D
                    if (arg.length() > 2) {
                        tokens.add(arg.substring(2)); // property=value
                    }
                }
                else
                {
                    eatTheRest = stopAtNonOption;
                    tokens.add(arg);
                }
            }
        }
        else
        {
            tokens.add(arg);
        }

        if (eatTheRest)
        {
            for (i++; i < arguments.length; i++)
            {
                tokens.add(arguments[i]);
            }
        }
    }

    return (String[]) tokens.toArray(new String[tokens.size()]);
}