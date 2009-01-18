package org.apache.couchdb.lucene;

public class
Main
{
    public static void
    main(String args[])
    {
        if(args.length != 1)
        {
            System.err.println("No runner specified.");
            System.exit(-1);
        }

        if(args[0].equalsIgnoreCase("index"))
        {
            IndexRunner.main();
        }
        else if(args[0].equalsIgnoreCase("query"))
        {
            QueryRunner.main();
        }
        else
        {
            System.err.println("Unknown runner: " + args[0]);
            System.exit(-1);
        }
    }
}