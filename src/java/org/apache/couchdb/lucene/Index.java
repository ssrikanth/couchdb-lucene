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
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
Index
extends Thread
{
    private File idxDir = null;
    private Database db = null;
    private Analyzer analyzer = null;
    private Directory directory = null;
    private IndexWriter writer = null;
    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    private QueryParser parser = null;
    private IndexState state = null;
    private BlockingQueue<Design> queue = null;

    public Index()
    throws CorruptIndexException, IOException, JSONException, LockObtainFailedException
    {
        this.setDaemon(true);
        this.db = new Database(new URL(Config.COUCHDB_URI));

        this.idxDir = new File(Config.LUCENE_IDX_DIR);
        if(!this.idxDir.exists()) this.idxDir.mkdirs();
        
        this.analyzer = new StandardAnalyzer();
        this.directory = NIOFSDirectory.getDirectory(this.idxDir);
        this.writer = this.createWriter(this.directory, this.analyzer);
        this.reader = IndexReader.open(this.directory, true);
        this.searcher = new IndexSearcher(this.reader);
        this.parser = new QueryParser("text", this.analyzer);
        this.state = new IndexState(this.idxDir);
        this.queue = new LinkedBlockingQueue<Design>();
    }

    public void
    close()
    throws IOException
    {
        this.writer.rollback();
    }

    public String
    query(QueryInfo info)
    throws CorruptIndexException, InterruptedException, IOException,
            JSONException, NoSuchAlgorithmException, ParseException
    {        
        if(info.getDestroy() && Config.DEBUG_ENABLED)
        {
            System.err.println("DEBUG CALL: Destroying database");
            this.state.setDBSignature(info.getDBName(), "");
            this.state.setDBUpdateSequence(info.getDBName(), 0);
            this.state.setIDXUpdateSequence(info.getDBName(), 0);
            this.writer.deleteDocuments(new MatchAllDocsQuery());
            return "{\"code\": 200, \"body\": \"ok\"}";
        }
        
        Design ddoc = new Design(this.db, info.getDBName());
        
        if(this.requiresIndex(ddoc, info.getUpdateSequence()))
        {
            this.queue.offer(ddoc);
        }
        
        if(info.getDebug() && Config.DEBUG_ENABLED)
        {
            while(this.state.getIDXUpdateSequence(info.getDBName()) < info.getUpdateSequence())
            {
                Thread.sleep(100);
            }
        }
        
        if(this.state.needsCommit())
        {
            this.commit();
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

    public void
    run()
    {
        while(true)
        {            
            try
            {
                Design ddoc = this.queue.take();
                this.index(ddoc);
            }
            
            catch(Exception exc)
            {
                System.err.println("Indexing operation failed.");
                exc.printStackTrace();
            }
        }
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
        ret.setRAMBufferSizeMB(Config.IDX_RAM_BUFFER);
        return ret;
    }
    
    private void
    commit()
    throws IOException, JSONException
    {
        this.writer.commit();
        this.state.write();
        IndexReader reader = this.reader.reopen();
        if(reader != this.reader)
        {
            this.reader.close();
            this.reader = reader;
            this.searcher.close();
            this.searcher = new IndexSearcher(this.reader);
        }
    }

    private boolean
    requiresIndex(Design ddoc, int update_seq)
    throws CorruptIndexException, IOException, JSONException, NoSuchAlgorithmException
    {
        this.state.setDBUpdateSequence(ddoc.getDBName(), update_seq);

        String new_sig = ddoc.getSignature();
        int idx_update_seq = this.state.getIDXUpdateSequence(ddoc.getDBName());

        boolean sigs_match = new_sig.equals(this.state.getDBSignature(ddoc.getDBName()));
        boolean seqs_ok = update_seq >= idx_update_seq - 1;
        if(!sigs_match || !seqs_ok)
        {
            
            System.err.println("Invalid State. Reseting index. Sigs match? " + sigs_match + " Seqs OK? " + seqs_ok);
            this.state.setDBSignature(ddoc.getDBName(), new_sig);
            this.state.setIDXUpdateSequence(ddoc.getDBName(), 0);
            this.writer.deleteDocuments(new Term(Config.FIELD_DB, ddoc.getDBName()));
            return true;
        }

        return false;
    }

    private void
    index(Design ddoc)
    throws CorruptIndexException, IOException, JSONException, NoSuchAlgorithmException
    {
        String dbname = ddoc.getDBName();

        List<String> docids = new ArrayList<String>();
        Map<String, Document> documents = new HashMap<String, Document>();
        
        int idx_update_seq = this.state.getIDXUpdateSequence(ddoc.getDBName());
        JSONObject resp = this.db.nextBySequence(dbname, idx_update_seq, Config.IDX_BULK_FETCH);
        JSONArray updates = resp.getJSONArray("rows");

        while(updates.length() > 0)
        {
            // List updated docids.
            for(int i = 0; i < updates.length(); i++)
            {
                String docid = updates.getJSONObject(i).getString("id");

                if(docid.equals(ddoc.getDocID()))
                {
                    Design newDdoc = new Design(this.db, ddoc.getDBName());
                    if(this.requiresIndex(newDdoc, updates.getJSONObject(i).getInt("key")))
                    {
                        System.err.println("Design document edited. Reseting index.");
                        this.writer.deleteDocuments(new Term(Config.FIELD_DB, ddoc.getDBName()));
                        this.state.setIDXUpdateSequence(ddoc.getDBName(), 0);
                        return;
                    }
                }

                documents.put(docid, null);
                docids.add(docid);
            }

            Iterator<String> iter = ddoc.getViews().keys();
            while(iter.hasNext())
            {
                this.addView(dbname, iter.next(), docids, documents);
            }

            this.updateDocuments(dbname, documents);

            // Notice the +1 to bump us past what's been indexed.
            idx_update_seq = 1 + updates.getJSONObject(updates.length()-1).getInt("key");
            this.state.setIDXUpdateSequence(dbname, idx_update_seq);
            
            // Next batch
            updates = this.db.nextBySequence(dbname, idx_update_seq, Config.IDX_BULK_FETCH).getJSONArray("rows");
        }
    }

    private JSONObject
    getDesignDoc(String dbname)
    throws IOException, JSONException
    {
        try
        {
            return this.db.getDoc(dbname, "_design/" + Config.COUCHDB_DDOC);
        }

        catch(FileNotFoundException exc)
        {
            throw new IOException("Failed to retrieve '_design/" + Config.COUCHDB_DDOC + "'");
        }
    }

    private void
    addView(String dbname, String view, List<String> docids, Map<String, Document> documents)
    throws IOException, JSONException
    {
        // Get data for updated docs.
        JSONArray rows = this.db.view(dbname, Config.COUCHDB_DDOC + "/" + view, docids).getJSONArray("rows");
        for(int i = 0; i < rows.length(); i++)
        {
            JSONObject curr = rows.getJSONObject(i);
            String docid = curr.getString("id");
            Document doc = documents.get(docid);
            if(doc == null)
            {
                doc = new Document();
                doc.add(new Field(Config.FIELD_DB, dbname, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(Config.FIELD_DOCID, docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
                documents.put(docid, doc);
            }
            String val = curr.get("value").toString();
            doc.add(new Field(view, val, Field.Store.NO, Field.Index.ANALYZED));            
        }
    }
    
    private void
    updateDocuments(String dbname, Map<String, Document> docs)
    throws IOException
    {
        Iterator<Map.Entry<String, Document>> iter = docs.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry<String, Document> entry = iter.next();
            String docid = (String) entry.getKey();
            Document doc = (Document) entry.getValue();
            
            BooleanQuery bq = new BooleanQuery();
            bq.add(new TermQuery(new Term(Config.FIELD_DB, dbname)), Occur.MUST);
            bq.add(new TermQuery(new Term(Config.FIELD_DOCID, docid)), Occur.MUST);
            this.writer.deleteDocuments(bq);
            
            if(doc != null)
            {
                this.writer.addDocument(doc);
            }
        }
    }
}