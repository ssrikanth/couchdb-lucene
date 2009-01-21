package org.apache.couchdb.lucene;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class
IndexExecutor
extends ThreadPoolExecutor
{
    private File idxDir = null;
    private Database db = null;
    private Analyzer analyzer = null;
    private Directory directory = null;
    private IndexWriter writer = null;
    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    private QueryParser parser = null;
    private ConcurrentHashMap<String, JSONObject> state = null;
    private ConcurrentHashMap<String, Future<IndexResult>> running = null;
    
    public IndexExecutor()
    throws CorruptIndexException, IOException, LockObtainFailedException
    {
        super(
            Config.COUCHDB_MIN_THREADS, Config.COUCHDB_MAX_THREADS,
            Config.COUCHDB_THREAD_TIMEOUT, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>()
        );
                
        this.db = new Database(new URL(Config.COUCHDB_URI));

        this.idxDir = new File(Config.LUCENE_IDX_DIR);
        if(!this.idxDir.exists()) this.idxDir.mkdirs();
        
        this.analyzer = new StandardAnalyzer();
        this.directory = NIOFSDirectory.getDirectory(this.idxDir);
        this.writer = this.createWriter(this.directory, this.analyzer);
        this.reader = IndexReader.open(this.directory, true);
        this.searcher = new IndexSearcher(this.reader);
        this.parser = new QueryParser("text", this.analyzer);
        this.state = this.readState(this.idxDir);
        this.running = new ConcurrentHashMap<String, Future<IndexResult>>();
    }

    public void
    close()
    {
        try
        {
            if(this.writer != null) this.writer.close();
            if(this.directory != null) this.directory.close();
            
            this.writer = null;
            this.directory = null;
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to close Index");
            exc.printStackTrace();
        }
    }

    public String
    query(QueryInfo info)
    throws CorruptIndexException, ExecutionException, InterruptedException, IOException,
            JSONException, NoSuchAlgorithmException, ParseException
    {
        this.index(info);
        
        if(info.getWait())
        {
            this.wait(info.getDBName(), info.getUpdateSequence());
        }

        IndexReader reader = this.reader.reopen();
        if(reader != this.reader)
        {
            this.reader.close();
            this.reader = reader;
            this.searcher.close();
            this.searcher = new IndexSearcher(this.reader);
        }

        if(this.reader.numDocs() == 0)
        {
            return "{\"code\": 200, \"json\": {\"total_rows\": 0, \"offset\": 0, \"rows\": []}}";
        }

        Query q = this.parser.parse(info.getQuery());
        TopDocs hits = this.searcher.search(q, null, info.getSkip() + info.getLimit(), new Sort(Config.FIELD_DOCID));
    
        JSONStringer out = new JSONStringer();
        out.object()
            .key("code").value(200)
            .key("json").object()
                .key("total_rows").value(hits.totalHits)
                .key("offset").value(info.getSkip())
                .key("rows").array();
        for(int i = info.getSkip(); i < hits.scoreDocs.length; i++)
        {
            Document d = this.searcher.doc(hits.scoreDocs[i].doc);
            out.object()
                .key("id").value(new String(d.get(Config.FIELD_DOCID)))
                .key("score").value(hits.scoreDocs[i].score)
            .endObject();
        }
        return out.endArray().endObject().endObject().toString();
    }

    protected void
    afterExecute(Runnable r, Throwable t)
    {
        try
        {
            if(t != null)
            {
                System.err.println("Failed to index database.");
                t.printStackTrace();
                return;
            }
        
            Future<IndexResult> f = (Future<IndexResult>) r;
            IndexResult res = f.get();
            JSONObject next = new JSONObject();
            next.put("signature", res.signature);
            next.put("update_seq", res.curr_seq);
            this.state.replace(res.dbname, next);
        
            if(!res.finished)
            {
                IndexJob job = new IndexJob(this.db, this.writer, res.dbname,
                                                Integer.MAX_VALUE, res.signature, res.curr_seq);
                this.running.replace(res.dbname, this.submit(job));
            }
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to finish job.");
            exc.printStackTrace();
        }
    }

    private void
    index(QueryInfo info)
    throws JSONException
    {
        JSONObject dbinfo = this.state.get(info.getDBName());
        if(dbinfo == null)
        {
            dbinfo = new JSONObject();
        }
        
        String sig = dbinfo.optString("signature", "");
        int curr_seq = dbinfo.optInt("update_seq", 0);

        Future<IndexResult> f = this.submit(new IndexJob(this.db, this.writer, info, sig, curr_seq));
        this.running.put(info.getDBName(), f);
    }

    private void
    wait(String dbname, int update_seq)
    throws ExecutionException, InterruptedException, IOException, JSONException
    {
        Future<IndexResult> f = this.running.get(dbname);
        while(f != null && f.get().curr_seq < update_seq)
        {
            this.running.get(dbname);
        }
        this.commit();
    }

    private IndexWriter
    createWriter(Directory directory, Analyzer analyzer)
    throws CorruptIndexException, IOException, LockObtainFailedException
    {
        if(IndexWriter.isLocked(directory))
        {
            IndexWriter.unlock(directory);
        }
        
        boolean create = !IndexReader.indexExists(directory);
        IndexWriter ret = new IndexWriter(directory, analyzer, create, IndexWriter.MaxFieldLength.UNLIMITED);
        ret.setUseCompoundFile(false);
        ret.setRAMBufferSizeMB(Config.LUCENE_MAX_RAM);
        return ret;
    }
    
    private void
    commit()
    throws IOException, JSONException
    {
        this.writer.commit();
        this.writeState(this.idxDir, this.state);
    }
    
    private String
    getDBSignature(String dbname)
    {
        JSONObject dbinfo = this.state.get(dbname);
        if(dbinfo == null)
        {
            dbinfo = new JSONObject();
            this.state.put(dbname, dbinfo);
        }
        return dbinfo.optString("signature", "");
    }

    private int
    getDBUpdateSequence(String dbname)
    {
        JSONObject dbinfo = this.state.get(dbname);
        if(dbinfo == null)
        {
            dbinfo = new JSONObject();
            this.state.put(dbname, dbinfo);
        }
        return dbinfo.optInt("update_seq", 0);
    }
    
    private ConcurrentHashMap<String, JSONObject>
    readState(File idxDir)
    throws IOException
    {
        File stateFile = new File(idxDir.toString() + File.separator + Config.LUCENE_STATE_FILE);
        if(!stateFile.exists())
        {
            return new ConcurrentHashMap<String, JSONObject>();
        }
        
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(stateFile));
            String json = reader.readLine();
            
            JSONObject obj = new JSONObject(json);
            ConcurrentHashMap<String, JSONObject> ret = new ConcurrentHashMap<String, JSONObject>();
            Iterator<String> iter = obj.keys();
            while(iter.hasNext())
            {
                String key = iter.next();
                ret.put(key, (JSONObject) obj.get(key));
            }
            
            return ret;
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to read state data.");
            return new ConcurrentHashMap<String, JSONObject>();
        }
        
        finally
        {
            if(reader != null)
            {
                reader.close();
            }
        }
    }
    
    private void
    writeState(File idxDir, Map<String, JSONObject> state)
    throws IOException, JSONException
    {
        BufferedWriter writer = null;
        try
        {
            String data = new JSONObject(state).toString();
            writer = new BufferedWriter(new FileWriter(idxDir.toString() + File.separator + Config.LUCENE_STATE_FILE));
            writer.write(data, 0, data.length());
        }
        
        finally
        {
            if(writer != null)
            {
                writer.close();
            }
        }
    }
}