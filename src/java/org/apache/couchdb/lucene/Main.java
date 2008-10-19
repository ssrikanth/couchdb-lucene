package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.json.JSONObject;

public class
Main
{
    static private BufferedReader input = null;

    public static void
    main(String[] args)
    throws Exception
    {
        input = new BufferedReader(new InputStreamReader(System.in));
        String data = null;
        
        System.err.println("Starting CouchDB Lucene Indexer.");

        while((data = input.readLine()) != null)
        {
            if(data.equals("[\"reset\"]"))
            {
                System.out.println("true");
                System.out.flush();
                continue;
            }
            
            try
            {
                JSONObject notice = new JSONObject(data);
                String action = notice.getString("action");
                if(action.equals("index"))
                {
                    Indexer.index(notice);
                }
                else if(action.equals("query"))
                {
                    QueryRunner.run(notice);
                }
                else
                {
                    System.out.println("Unknown action type: " + action);
                    System.out.flush();
                }
            }
            
            catch(Exception e)
            {
                e.printStackTrace(System.err);
            }
        }
        
        return;
    }
}