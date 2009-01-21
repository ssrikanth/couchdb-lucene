package org.apache.couchdb.lucene;

import org.json.JSONException;
import org.json.JSONObject;

public class
QueryInfo
{
    private final JSONObject info;
    
    public
    QueryInfo(JSONObject info)
    {
        this.info = info;
    }
    
    String
    getDBName()
    throws JSONException
    {
        return info.getJSONObject("info").getString("db_name");
    }
    
    String
    getInstanceStartTime()
    throws JSONException
    {
        return info.getJSONObject("info").getString("instance_start_time");
    }
    
    int
    getUpdateSequence()
    throws JSONException
    {
        return info.getJSONObject("info").getInt("update_seq");
    }
    
    String
    getQuery()
    throws JSONException
    {
        return info.getJSONObject("query").getString("q");
    }
    
    int
    getLimit()
    throws JSONException
    {
        return info.getJSONObject("query").optInt("limit", 25);
    }
    
    int
    getSkip()
    throws JSONException
    {
        return info.getJSONObject("query").optInt("skip", 0);
    }
    
    boolean
    getWait()
    throws JSONException
    {
        return info.getJSONObject("query").optBoolean("wait", true);
    }
}