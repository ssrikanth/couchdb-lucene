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
            IndexExecutor exec = new IndexExecutor();
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            String data = null;

            while((data = input.readLine()) != null)
            {
                String ret = null ;
                
                try
                {
                    QueryInfo info = new QueryInfo(new JSONObject(data));
                    ret = exec.query(info);
                }
                
                catch(Exception ex)
                {
                    System.err.println("Failed to run query.");
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
            
            
        }
        
        catch(Exception exc)
        {
            System.err.println("couchdb-lucene is dying.");
            exc.printStackTrace();
        }
    }
}