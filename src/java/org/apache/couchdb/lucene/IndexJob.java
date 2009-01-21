package org.apache.couchdb.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class
IndexJob
implements Callable<IndexResult>
{
    private final Database db;
    private final IndexWriter writer;
    private final String dbname;
    private final String signature;
    private final int update_seq;
    private int curr_seq;
    
    public
    IndexJob(Database db, IndexWriter writer, QueryInfo info, String signature, int curr_seq)
    throws JSONException
    {
        this.db = db;
        this.writer = writer;
        this.dbname = info.getDBName();
        this.update_seq = info.getUpdateSequence();
        this.signature = signature;
        this.curr_seq = curr_seq;
    }
    
    public
    IndexJob(Database db, IndexWriter writer, String dbname, int update_seq, String signature, int curr_seq)
    {
        this.db = db;
        this.writer = writer;
        this.dbname = dbname;
        this.update_seq = update_seq;
        this.signature = signature;
        this.curr_seq = curr_seq;
    }
    
    public IndexResult
    call()
    throws CorruptIndexException, IOException, JSONException, NoSuchAlgorithmException
    {
        JSONObject design_doc = this.getDesignDoc(this.dbname);
        String next_signature = this.getSignature(design_doc);
        
        // Reset when views change
        if(!this.signature.equals(next_signature))
        {
            System.err.println("Removing documents indexed for database: " + this.dbname);
            this.writer.deleteDocuments(new Term(Config.FIELD_DB, this.dbname));
            this.curr_seq = 0;
        }
        // Short cicuit with no updates.
        else if(this.update_seq < this.curr_seq)
        {
            return new IndexResult(this.dbname, true, next_signature, this.curr_seq);
        }

        List<String> docids = new ArrayList<String>();
        Map<String, Document> documents = new HashMap<String, Document>();
        
        JSONObject resp = this.db.nextBySequence(this.dbname, this.curr_seq, Config.COUCHDB_BULK_GET);
        JSONArray updates = resp.getJSONArray("rows");

        if(updates.length() < 1)
        {
            return new IndexResult(this.dbname, true, next_signature, this.curr_seq);
        }

        // List updated docids.
        for(int i = 0; i < updates.length(); i++)
        {
            String docid = updates.getJSONObject(i).getString("id");
            documents.put(docid, null);
            docids.add(docid);
        }

        Iterator<String> iter = design_doc.getJSONObject("views").keys();
        while(iter.hasNext())
        {
            this.addView(iter.next(), docids, documents);
        }

        this.updateDocuments(documents);

        // Notice the +1 to bump us past what's been indexed.
        this.curr_seq = 1 + updates.getJSONObject(updates.length()-1).getInt("key");
        return new IndexResult(this.dbname, this.update_seq >= this.curr_seq, next_signature, this.curr_seq);
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

    private String
    getSignature(JSONObject design_doc)
    throws JSONException, NoSuchAlgorithmException
    {
        JSONObject sig = new JSONObject();
        sig.put("views", design_doc.getJSONObject("views"));
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        System.err.println("SIG: " + sig.toString());
        md.update(sig.toString().getBytes());
        BigInteger bi = new BigInteger(md.digest());
        return bi.toString(16);
    }

    private void
    addView(String view, List<String> docids, Map<String, Document> documents)
    throws IOException, JSONException
    {
        // Get data for updated docs.
        JSONArray rows = this.db.view(this.dbname, Config.COUCHDB_DDOC + "/" + view, docids).getJSONArray("rows");
        for(int i = 0; i < rows.length(); i++)
        {
            JSONObject curr = rows.getJSONObject(i);
            String docid = curr.getString("id");
            Document doc = documents.get(docid);
            if(doc == null)
            {
                doc = new Document();
                doc.add(new Field(Config.FIELD_DB, this.dbname, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(Config.FIELD_DOCID, docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
                documents.put(docid, doc);
            }
            String val = curr.get("value").toString();
            doc.add(new Field(view, val, Field.Store.NO, Field.Index.ANALYZED));            
        }
    }
    
    private void
    updateDocuments(Map<String, Document> docs)
    throws IOException
    {
        Iterator<Map.Entry<String, Document>> iter = docs.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry<String, Document> entry = iter.next();
            String docid = (String) entry.getKey();
            Document doc = (Document) entry.getValue();
            
            BooleanQuery bq = new BooleanQuery();
            bq.add(new TermQuery(new Term(Config.FIELD_DB, this.dbname)), Occur.MUST);
            bq.add(new TermQuery(new Term(Config.FIELD_DOCID, docid)), Occur.MUST);
            this.writer.deleteDocuments(bq);
            
            if(doc != null)
            {
                this.writer.addDocument(doc);
            }
        }
    }
}