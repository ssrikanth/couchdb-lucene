package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

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

    public String
    getName()
    {
        return this.dbname;
    }

    public JSONObject
    getDesignDocs()
    throws IOException, JSONException, MalformedURLException
    {
        String path = "/" + this.dbname
                    + "/_all_docs?startkey=\"_design/\"&endkey=\"_design/\\u9999\"&include_docs=true";
        String data = fetch(new URL(this.url, path));
        return new JSONObject(data);
    }

    public JSONObject
    getDoc(String docid)
    throws IOException, JSONException, MalformedURLException
    {
        docid = URLEncoder.encode(docid, "UTF-8");
        String data = fetch(new URL(this.url, "/" + this.dbname + "/" + docid));
        return new JSONObject(data);
    }
    
    public JSONObject
    getDoc(String docid, String revision)
    throws IOException, JSONException, MalformedURLException
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
    nextBySequence(int curr_seq, int count)
    throws IOException, JSONException, MalformedURLException
    {
        URL loc = new URL(this.url, "/" + this.dbname + "/_all_docs_by_seq?startkey=" + curr_seq + "&limit=" + count);
        return new JSONObject(fetch(loc));
    }

    public JSONObject
    view(String view, List<String> docids)
    throws IOException, JSONException, MalformedURLException
    {
        URL loc = new URL(this.url, "/" + this.dbname + "/_view/" + view);
        
        JSONStringer out = new JSONStringer();
        out.object().key("keys").array();

        Iterator<String> iter = docids.iterator();
        while(iter.hasNext())
        {
            out.value((String) iter.next());
        }
        
        out.endArray().endObject();
        
        return new JSONObject(this.fetch(loc, out.toString()));
    }

    private String
    fetch(URL url)
    throws IOException
    {
        return this.fetch(url, null);
    }
    
    private String
    fetch(URL url, String body)
    throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if(body != null)
        {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();
        }

        DataInputStream dis = new DataInputStream(conn.getInputStream());
        InputStreamReader isr = new InputStreamReader(dis);
        BufferedReader reader = new BufferedReader(isr);

        if(conn.getResponseCode() != 200)
        {
            throw new IOException("Invalid response code from server: " + conn.getResponseCode());
        }

        StringWriter data = new StringWriter();
        String line = null;
        do
        {
            line = reader.readLine();
            data.write(line);
        } while(line != null);

        reader.close();

        return data.toString();
    }
}