package org.apache.couchdb.lucene.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONObject;

import org.apache.couchdb.lucene.db.Database;
import org.apache.couchdb.lucene.utils.Config;

public class
Runner
{
    static private BufferedReader input = null;
    
    public static void
    main(String[] args)
    throws Exception
    {
        input = new BufferedReader(new InputStreamReader(System.in));
        String data = null;
        
        while((data = input.readLine()) != null)
        {
            Index idx = null;
            
            try
            {
                JSONObject notice = new JSONObject(data);
                String dbname = notice.getString("db");
                String type = notice.getString("type");
                   
                if(type.equalsIgnoreCase("updated"))
                {
                    System.err.println("Updating index for: " + dbname);
                    Database db = new Database(new URL(Config.DBURI), dbname);
                    idx = new Index(new File(Config.IDXDIR + File.separator + dbname), db, Config.BULKSIZE);
                    idx.update();
                }
                else if(type.equalsIgnoreCase("deleted"))
                {
                    System.err.println("Deleting index for: " + dbname);
                    Index.delete(new File(Config.IDXDIR + File.separator + dbname));
                }
                else
                {
                    System.err.println("Unknown notification '" + type + "' for index: " + dbname);
                }
            }
        
            catch(Exception e)
            {
                e.printStackTrace(System.err);
            }
            
            finally
            {
                if(idx != null) idx.close();
            }
        }
        
        return;
    }
}
