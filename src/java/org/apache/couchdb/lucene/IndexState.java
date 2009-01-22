package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class
IndexState
{
    private final File stateFile;
    private long last_commit;
    private long num_updates;
    private ConcurrentHashMap<String, String> signatures;
    private ConcurrentHashMap<String, Integer> db_update_seqs;
    private ConcurrentHashMap<String, Integer> idx_update_seqs;
    private ConcurrentHashMap<String, Boolean> running;
    
    public
    IndexState(File idxDir)
    throws IOException, JSONException
    {
        this.stateFile = new File(idxDir.toString() + File.separator + Config.LUCENE_STATE_FILE);
        this.last_commit = System.currentTimeMillis() / 1000;
        this.num_updates = 0;
        this.read();
    }

    public boolean
    needsCommit()
    {        
        boolean interval_exceeded = false;
        boolean updates_exceeded = false;
        
        if(Config.COMMIT_INTERVAL > 0)
        {
            long current = System.currentTimeMillis() / 1000;
            interval_exceeded = (current - this.last_commit) > Config.COMMIT_INTERVAL;
        }
        
        if(Config.COMMIT_UPDATES > 0)
        {
            updates_exceeded = this.num_updates > Config.COMMIT_UPDATES;
        }
        
        if(interval_exceeded || updates_exceeded)
        {
            this.last_commit = System.currentTimeMillis() / 1000;
            this.num_updates = 0;
            return true;
        }
        
        return false;
    }
    
    public String
    getDBSignature(String dbname)
    {
        String sig = this.signatures.get(dbname);
        return sig == null ? "" : sig;
    }
    
    public void
    setDBSignature(String dbname, String val)
    {
        this.signatures.put(dbname, val);
    }

    public int
    getDBUpdateSequence(String dbname)
    {
        Integer seq = this.db_update_seqs.get(dbname);
        return seq == null ? 0 : seq.intValue();
    }
    
    public void
    setDBUpdateSequence(String dbname, int val)
    {
        this.db_update_seqs.put(dbname, new Integer(val));
    }
    
    public int
    getIDXUpdateSequence(String dbname)
    {
        Integer seq = this.idx_update_seqs.get(dbname);
        return seq == null ? 0 : seq.intValue();
    }
    
    public void
    setIDXUpdateSequence(String dbname, int val)
    {
        Integer last_val = this.idx_update_seqs.put(dbname, new Integer(val));

        if(last_val != null && last_val < val)
        {
            this.num_updates += val - last_val.intValue();
        }
        else
        {
            this.num_updates += val;
        }
    }
    
    public boolean
    getRunning(String dbname)
    {
        Boolean run = this.running.get(dbname);
        return run == null ? false : run.booleanValue();
    }
    
    public void
    setRunning(String dbname, boolean val)
    {
        this.running.put(dbname, new Boolean(val));
    }

    public void
    read()
    throws IOException
    {
        this.initEmpty();

        if(!stateFile.exists())
        {
            return;
        }
        
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(stateFile));
            String json = reader.readLine();
            
            JSONObject obj = new JSONObject(json);
            Iterator<String> iter = obj.keys();
            while(iter.hasNext())
            {
                String key = iter.next();
                JSONObject db = obj.getJSONObject(key);
                this.setDBSignature(key, db.getString("signature"));
                this.setDBUpdateSequence(key, db.getInt("update_seq"));
                this.setIDXUpdateSequence(key, db.getInt("update_seq"));
                this.setRunning(key, false);
            }
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to read state data.");
            exc.printStackTrace();
            this.initEmpty();
            return;
        }
        
        finally
        {
            if(reader != null)
            {
                reader.close();
            }
        }
    }
    
    public void
    write()
    throws IOException, JSONException
    {
        BufferedWriter writer = null;
        try
        {
            JSONObject state = new JSONObject();
            Iterator<String> iter = this.signatures.keySet().iterator();
            while(iter.hasNext())
            {
                String db = iter.next();
                JSONObject dbstate = new JSONObject();
                dbstate.put("signature", this.signatures.get(db));
                dbstate.put("update_seq", this.idx_update_seqs.get(db).intValue());
                state.put(db, dbstate);
            }
            String data = state.toString();
            writer = new BufferedWriter(new FileWriter(this.stateFile));
            writer.write(data, 0, data.length());
            writer.newLine();
        }
        
        finally
        {
            if(writer != null)
            {
                writer.close();
            }
        }
    }
    
    private void
    initEmpty()
    {
        this.signatures = new ConcurrentHashMap<String, String>();
        this.db_update_seqs = new ConcurrentHashMap<String, Integer>();
        this.idx_update_seqs = new ConcurrentHashMap<String, Integer>();
        this.running = new ConcurrentHashMap<String, Boolean>();        
    }
}