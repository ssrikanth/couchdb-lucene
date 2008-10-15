package org.apache.couchdb.lucene.db;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

public class
Database
{
    private URL url = null;
    private String dbname = null;
    
    public Database(URL url, String dbname)
    {
        this.url = url;
        this.dbname = dbname;
    }

    public JSONObject
    getDoc(String docid)
    throws JSONException, IOException, MalformedURLException
    {
        docid = URLEncoder.encode(docid, "UTF-8");
        String data = fetch(new URL(this.url, "/" + this.dbname + "/" + docid));
        return new JSONObject(data);
    }
    
    public JSONObject
    getDoc(String docid, String revision)
    throws JSONException, IOException, MalformedURLException
    {
        docid = URLEncoder.encode(docid, "UTF-8");
        String data = fetch(new URL(this.url, "/" + this.dbname + "/" + docid + "?rev=" + revision));
        if(data.replaceAll("\\s\\t\\r ", "").length() == 0)
        {
            System.err.println("Bad document: " + docid + " " + revision);
        }
        return new JSONObject(data);
    }
    
    public JSONObject
    nextBySequence(String curr_seq, int count)
    throws JSONException, IOException, MalformedURLException
    {
        URL loc = new URL(this.url, "/" + this.dbname + "/_all_docs_by_seq?startkey=" + curr_seq + "&count=" + count);
        return new JSONObject(fetch(loc));
    }

    private String
    fetch(URL url)
    throws IOException
    {
        URLConnection conn = url.openConnection();
        DataInputStream dis = new DataInputStream(conn.getInputStream());
        InputStreamReader isr = new InputStreamReader(dis);
        BufferedReader reader = new BufferedReader(isr);

        StringWriter data = new StringWriter();
        String line = null;
        do
        {
            line = reader.readLine();
            data.write(line);
        } while(line != null);

        return data.toString();
    }
}