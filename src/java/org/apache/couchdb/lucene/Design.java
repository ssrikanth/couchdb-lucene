package org.apache.couchdb.lucene;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

public class
Design
{
    private final Database db;
    private final String dbname;
    private final JSONObject json;
    
    public
    Design(Database db, String dbname)
    throws IOException, JSONException
    {
        this.db = db;
        this.dbname = dbname;
        this.json = db.getDoc(this.dbname, "_design/" + Config.COUCHDB_DDOC);
    }

    public String
    getDocID()
    throws JSONException
    {
        return this.json.getString("_id");
    }
    
    public String
    getDBName()
    throws JSONException
    {
        return this.dbname;
    }

    public JSONObject
    getViews()
    throws JSONException
    {
        return this.json.getJSONObject("views");
    }

    public String
    getSignature()
    throws JSONException, NoSuchAlgorithmException
    {
        JSONObject sig = new JSONObject();
        sig.put("views", this.json.getJSONObject("views"));
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(sig.toString().getBytes());
        BigInteger bi = new BigInteger(md.digest());
        return bi.toString(16);
    }
}