package org.apache.couchdb.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class
Indexer
extends Thread
{
    private static final String CREATED = "created";
    private static final String UPDATED = "updated";
    private static final String DELETED = "deleted";
    private static final String UNKNOWN = "unknown";
    
    private String status = CREATED;
    private File idxDir = null;
    private Database db = null;
    private int bulkSize = 1;
    private Analyzer analyzer = null;
    private Directory directory = null;
    private IndexWriter writer = null;
    private SequenceIds seqids = null;
    
    public Indexer(String dbname)
    throws IOException
    {
        super(dbname);

        this.idxDir = new File(Config.IDXDIR + File.separator + dbname);
        if(!this.idxDir.exists())
        {
            this.idxDir.mkdirs();
        }
        
        this.db = new Database(new URL(Config.DBURI), dbname);
        this.analyzer = new StandardAnalyzer();
        this.directory = FSDirectory.getDirectory(this.idxDir);
        boolean exists = IndexReader.indexExists(this.directory);
        
        if(IndexWriter.isLocked(this.directory))
        {
            IndexWriter.unlock(this.directory);
        }
        
        this.writer = new IndexWriter(this.directory, this.analyzer, !exists, IndexWriter.MaxFieldLength.UNLIMITED);
        this.seqids = new SequenceIds(this.idxDir.toString() + File.separator + Config.SEQIDS);
    }

    public synchronized void
    notify(JSONObject notice)
    throws JSONException
    {
        String status = notice.getString("type");

        if(
                !status.equalsIgnoreCase(CREATED)
            &&  !status.equalsIgnoreCase(UPDATED)
            &&  !status.equalsIgnoreCase(DELETED)
        )
        {
            System.err.println("Unknown status notification: " + status);
            this.status = UNKNOWN;
        }
        else
        {
            this.status = status;
        }
    }
    
    public synchronized boolean
    deleted()
    {
        return this.status == DELETED;
    }
    
    public void
    run()
    {
        try
        {
            boolean updated = true;
            while(updated)
            {
                updated = false;

                List views = this.getViews();
                Iterator viewIter = views.iterator();
                while(viewIter.hasNext())
                {
                    String view = (String) viewIter.next();
                    if(this.updateView(view))
                    {
                        updated = true;
                    }
                }                
            }
        }
        
        catch(Exception exc)
        {
            System.err.println("Indexer thread for db: " + this.db.getName() + " died with an exception.");
            exc.printStackTrace();
        }
        
        finally
        {
            this.close();
        }
    }

    private void
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
            System.err.println("Indexer thread for db: " + this.db.getName() + " caught exception when closing.");
            exc.printStackTrace();
        }
        
        try
        {
            if(this.deleted())
            {
                this.delete(this.idxDir);
            }
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to delete index directory: " + this.idxDir.toString());
            exc.printStackTrace();
        }
    }
    
    private List<String>
    getViews()
    throws Exception
    {
        List indexViews = new ArrayList<String>();
        JSONObject designDocs = this.db.getDesignDocs();
        JSONArray rows = designDocs.getJSONArray("rows");
        for(int i = 0; i < rows.length(); i++)
        {
            JSONArray views = rows.getJSONObject(i).getJSONObject("doc").optJSONArray("lucene");
            if(views == null)
            {
                continue;
            }
            
            for(int j = 0; j < views.length(); j++)
            {
                String ddocid = rows.getJSONObject(i).getString("id").substring(8);
                indexViews.add(ddocid + "/" + views.getString(j));
            }
        }
        
        return indexViews;
    }
    
    private boolean
    updateView(String view)
    throws IOException, JSONException
    {
        boolean did_update = false;
        JSONArray updates = this.db.nextBySequence(this.seqids.getSequence(view), Config.BULKSIZE).getJSONArray("rows");
        
        while(updates.length() > 0)
        {
            did_update = true;

            Map<String, Document> documents = new HashMap<String, Document>();
            
            // List updated docids.
            List docids = new ArrayList<String>();
            for(int i = 0; i < updates.length(); i++)
            {
                String docid = updates.getJSONObject(i).getString("id");
                documents.put(docid, null);
                docids.add(docid);
            }
            
            // Get data for updated docs.
            JSONArray rows = this.db.view(view, docids).getJSONArray("rows");
            for(int i = 0; i < rows.length(); i++)
            {
                JSONObject curr = rows.getJSONObject(i);
                String docid = curr.getString("id");
                Document doc = documents.get(docid);
                if(doc == null)
                {
                    System.err.println("Creating document for: " + docid);
                    doc = new Document();
                    doc.add(new Field(Config.DOCID, docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
                    doc.add(new Field(Config.VIEW, view, Field.Store.YES, Field.Index.NOT_ANALYZED));
                    
                }
                String val = curr.get("value").toString();
                doc.add(new Field(Config.DATA, val, Field.Store.NO, Field.Index.ANALYZED));
            }

            // Add docs to index
            this.updateDocuments(view, documents);
            
            // Keep state
            this.seqids.setSequence(view, updates.getJSONObject(updates.length()-1).getInt("key"));
            
            // Next set
            updates = db.nextBySequence(this.seqids.getSequence(view), Config.BULKSIZE).getJSONArray("rows");
        }

        // Flush updates to disk.
        if(did_update)
        {
            this.writer.commit();
            this.seqids.write();
        }
        
        return did_update;
    }
    
    private void
    updateDocuments(String view, Map<String, Document> docs)
    throws IOException
    {
        Iterator<Map.Entry<String, Document>> iter = docs.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry<String, Document> entry = iter.next();
            String docid = (String) entry.getKey();
            Document doc = (Document) entry.getValue();
            
            BooleanQuery q = new BooleanQuery();
            q.add(new TermQuery(new Term(Config.DOCID, docid)), Occur.MUST);
            q.add(new TermQuery(new Term(Config.VIEW, view)), Occur.MUST);
            
            this.writer.deleteDocuments(q);
            
            if(doc != null)
            {
                this.writer.addDocument(doc);
            }
        }
    }
    
    private void
    delete(File dir)
    throws Exception
    {
        File[] files = dir.listFiles();
        for(int i = 0; i < files.length; i++)
        {
            if(files[i].isDirectory())
            {
                delete(files[i]);
            }
            else
            {
                files[i].delete();
            }
        }
        dir.delete();
    }
    
    private class
    SequenceIds
    {
        private File fname = null;
        private Map<String, Integer> ids = null;
        
        public SequenceIds(String fname)
        {
            this.fname = new File(fname);
            this.read();
        }
        
        public int
        getSequence(String view)
        {
            Integer ret = this.ids.get(view);
            if(ret == null)
            {
                return 0;
            }
            return ret.intValue();
        }

        public void
        setSequence(String view, int value)
        {
            this.ids.put(view, new Integer(value));
        }
        
        public void
        read()
        {
            if(!this.fname.exists())
            {
                this.ids = new HashMap<String, Integer>();
                return;
            }
            
            try
            {    
                FileInputStream fis = new FileInputStream(this.fname);
                ObjectInputStream ois = new ObjectInputStream(fis);
                this.ids = (HashMap) ois.readObject();
                ois.close();
                fis.close();
            }
            
            catch(Exception exc)
            {
                System.err.println("Failed to read sequence ids from: " + fname);
                exc.printStackTrace();
            }
        }
        
        public void
        write()
        {
            try
            {
                FileOutputStream fos = new FileOutputStream(this.fname);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(this.ids);
                oos.close();
                fos.close();
            }
            
            catch(Exception exc)
            {
                System.err.println("Failed to write sequence ids to: " + fname);
                exc.printStackTrace();
            }
        }
    }
}