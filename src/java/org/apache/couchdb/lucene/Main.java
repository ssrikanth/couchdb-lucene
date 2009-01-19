package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONStringer;

public class
Main
{
    public static void
    main(String args[])
    {
        try
        {
            IndexCache cache = new IndexCache(Config.MAXDBS);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            String data = null;

            while((data = input.readLine()) != null)
            {
                String ret = null ;
                
                try
                {
                    JSONObject req = new JSONObject(data);
                    String dbname = req.getJSONObject("info").getString("db_name");
                    int update_seq = req.getJSONObject("info").getInt("update_seq");
                    String query = req.getJSONObject("query").getString("q");
                    int count = req.getJSONObject("query").optInt("limit", 25);
                    int offset = req.getJSONObject("query").optInt("skip", 0);

                    Index idx = (Index) cache.get(dbname);
                    if(idx == null)
                    {
                        idx = new Index(dbname);
                        cache.put(dbname, idx);
                    }
                    
                    ret = idx.query(update_seq, query, count, offset);
                }
                
                catch(Exception ex)
                {
                    ex.printStackTrace(System.err);
                    ret = new JSONStringer().object()
                                .key("code").value(500)
                                .key("body").value("Failed to execute query: " + ex.getMessage())
                            .endObject().toString();
                }
        
                finally
                {
                    // Send the response
                    System.out.println(ret);
                }
            }
            
            cache.closeAll();
        }
        
        catch(Exception exc)
        {
            System.err.println("couchdb-lucene is dying.");
            exc.printStackTrace();
        }
    }
}