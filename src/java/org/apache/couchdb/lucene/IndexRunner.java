package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class
IndexRunner
{
    static private Map indexers = null;
    static private BufferedReader input = null;
    
    public static void
    main()
    {
        try
        {
            indexers = new HashMap<String, Indexer>();
            input = new BufferedReader(new InputStreamReader(System.in));
            String data = null;
        
            while((data = input.readLine()) != null)
            {
                JSONObject notice = new JSONObject(data);
                String dbname = notice.getString("db");

                Indexer idx = (Indexer) indexers.get(dbname);
                if(idx == null || !idx.isAlive())
                {
                    indexers.put(dbname, new Indexer(dbname));
                }

                idx = (Indexer) indexers.get(dbname);
                idx.notify(notice);

                if(!idx.isAlive())
                {
                    idx.start();
                }
            }
        }
        
        catch(Exception e)
        {
            System.err.println("IndexRunner is dying.");
            e.printStackTrace(System.err);
        }
        
        return;
    }
}
