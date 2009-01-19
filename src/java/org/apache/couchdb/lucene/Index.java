package org.apache.couchdb.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

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
{
    private final String dbname;
    private File idxDir = null;
    private Database db = null;
    private Analyzer analyzer = null;
    private Directory directory = null;
    private IndexWriter writer = null;
    private IndexReader reader = null;
    private IndexSearcher searcher = null;
    private QueryParser parser = null;
    private SequenceId seqid = null;
    
    public Index(String dbname)
    throws CorruptIndexException, IOException, LockObtainFailedException
    {
        this.dbname = dbname;

        this.idxDir = new File(Config.IDXDIR + File.separator + this.dbname);
        if(!this.idxDir.exists()) this.idxDir.mkdirs();
        
        this.db = new Database(new URL(Config.DBURI), this.dbname);
        this.directory = NIOFSDirectory.getDirectory(this.idxDir);

        boolean exists = IndexReader.indexExists(this.directory);
        
        if(IndexWriter.isLocked(this.directory))
        {
            IndexWriter.unlock(this.directory);
        }

        this.analyzer = new StandardAnalyzer();
        this.writer = this.createWriter(this.directory, this.analyzer, !exists);
        this.reader = IndexReader.open(this.directory, true);
        this.searcher = new IndexSearcher(this.reader);
        this.parser = new QueryParser("text", this.analyzer);
        this.seqid = new SequenceId(this.idxDir.toString() + File.separator + Config.SEQID);
    }

    private IndexWriter
    createWriter(Directory directory, Analyzer analyzer, boolean create)
    throws CorruptIndexException, IOException, LockObtainFailedException
    {
        IndexWriter ret = new IndexWriter(directory, analyzer, create, IndexWriter.MaxFieldLength.UNLIMITED);
        ret.setUseCompoundFile(false);
        ret.setRAMBufferSizeMB(Config.MAXRAM);
        return ret;
    }

    public void
    close()
    {
        try
        {
            if(this.searcher != null) this.searcher.close();
            if(this.reader != null) this.reader.close();
            if(this.writer != null) this.writer.close();
            if(this.directory != null) this.directory.close();
            
            this.searcher = null;
            this.reader = null;
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
    query(int update_seq, String query, int limit, int skip)
    throws CorruptIndexException, IOException, JSONException, NoSuchAlgorithmException, ParseException
    {
        this.index(update_seq);

        Query q = this.parser.parse(query);
        TopDocs hits = this.searcher.search(q, null, skip + limit, new Sort(Config.DOCID));
    
        JSONStringer out = new JSONStringer();
        out.object()
            .key("code").value(200)
            .key("json").object()
                .key("total_rows").value(hits.totalHits)
                .key("offset").value(skip)
                .key("rows").array();
        for(int i = skip; i < hits.scoreDocs.length; i++)
        {
            Document d = this.searcher.doc(hits.scoreDocs[i].doc);
            out.object()
                .key("id").value(new String(d.get(Config.DOCID)))
                .key("score").value(hits.scoreDocs[i].score)
            .endObject();
        }
        return out.endArray().endObject().endObject().toString();
    }
    
    private void
    index(int update_seq)
    throws CorruptIndexException, IOException, JSONException, NoSuchAlgorithmException
    {
        List views = this.getViewNames(update_seq);

        int curr_seq = this.seqid.get();
        
        while(curr_seq < update_seq)
        {
            JSONArray updates = db.nextBySequence(this.seqid.get(), update_seq, Config.BULKSIZE).getJSONArray("rows");

            List docids = new ArrayList<String>();
            Map<String, Document> documents = new HashMap<String, Document>();

            // List updated docids.
            for(int i = 0; i < updates.length(); i++)
            {
                String docid = updates.getJSONObject(i).getString("id");
                documents.put(docid, null);
                docids.add(docid);
            }

            Iterator<String> viewIter = views.iterator();
            while(viewIter.hasNext())
            {
                String view = (String) viewIter.next();
                this.addView(view, docids, documents);
            }
            
            this.updateDocuments(documents);
            
            curr_seq = updates.getJSONObject(updates.length()-1).getInt("key");
        }
        
        if(this.seqid.get() != curr_seq)
        {
            this.writer.commit();
            this.seqid.set(curr_seq);
            this.seqid.write();
            this.reader = this.reader.reopen();
            this.searcher.close();
            this.searcher = new IndexSearcher(this.reader);
        }
    }

    private List<String>
    getViewNames(int update_seq)
    throws IOException, JSONException, NoSuchAlgorithmException
    {
        JSONObject ddoc = null;
        
        try
        {
            ddoc = this.db.getDoc("_design/" + Config.DESIGN);
        }
        
        catch(FileNotFoundException exc)
        {
            throw new IOException("Failed to retrieve '_design/" + Config.DESIGN + "'");
        }
        
        
        List ret = new ArrayList<String>();
        JSONObject views = ddoc.optJSONObject("views");
        
        if(views == null)
        {
            throw new JSONException("No views member defined in '_design/" + Config.DESIGN + "'");
        }

        Iterator iter = views.keys();
        while(iter.hasNext())
        {
            ret.add((String) iter.next());            
        }

        if(ret.size() < 1)
        {
            throw new JSONException("No views defined in '_design/" + Config.DESIGN + "'");
        }

        //if(this.seqid.signatureChanged(update_seq, views.toString()))
        if(this.seqid.signatureChanged(update_seq, ddoc.getString("_rev")))
        {
            this.writer.deleteDocuments(new MatchAllDocsQuery());
        }
        
        return ret;
    }
    
    private void
    addView(String view, List<String> docids, Map<String, Document> documents)
    throws IOException, JSONException
    {
        // Get data for updated docs.
        JSONArray rows = this.db.view(Config.DESIGN + "/" + view, docids).getJSONArray("rows");
        for(int i = 0; i < rows.length(); i++)
        {
            JSONObject curr = rows.getJSONObject(i);
            String docid = curr.getString("id");
            Document doc = documents.get(docid);
            if(doc == null)
            {
                doc = new Document();
                doc.add(new Field(Config.DOCID, docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
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
            
            this.writer.deleteDocuments(new TermQuery(new Term(Config.DOCID, docid)));
            
            if(doc != null)
            {
                this.writer.addDocument(doc);
            }
        }
    }
}