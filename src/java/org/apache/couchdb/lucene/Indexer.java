package org.apache.couchdb.lucene;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.couchdb.lucene.Config;

public class
Indexer
{    
    public static void
    index(JSONObject notice)
    throws Exception
    {
        IndexWriter writer = null;

        try
        {
            String dbname = notice.getString("db");
            String group = notice.getString("group");
            writer = createIndexWriter(dbname, notice.getString("current_seq"));
            
            JSONArray views = notice.getJSONArray("views");

            JSONArray toRemove = notice.getJSONArray("remove");
            removeDocuments(writer, dbname, group, views, toRemove);

            JSONArray toInsert = notice.getJSONArray("insert");
            insertDocuments(writer, dbname, group, views, toInsert);
        }
        
        finally
        {
            closeIndexWriter(writer, notice.getString("db"), notice.getString("new_seq"));
        }
        
        System.out.println("true");
        System.out.flush();
    }
 
    private static void
    removeDocuments(IndexWriter writer, String dbname, String group, JSONArray views, JSONArray toRemove)
    throws CorruptIndexException, IOException, JSONException
    {
        for(int i = 0; i < toRemove.length(); i++)
        {
            JSONObject rem = toRemove.getJSONObject(i);
            BooleanQuery q  = new BooleanQuery();
            q.add(new TermQuery(new Term(Config.DB_FIELD, dbname)), BooleanClause.Occur.MUST);
            q.add(new TermQuery(new Term(Config.GROUP_FIELD, group)), BooleanClause.Occur.MUST);
            q.add(new TermQuery(new Term(Config.DOCID_FIELD, rem.getString("docid"))), BooleanClause.Occur.MUST);
            q.add(new TermQuery(new Term(Config.KEY_FIELD, rem.getString("key"))), BooleanClause.Occur.MUST);
            
            BooleanQuery vq = new BooleanQuery();
            for(int j = 0; j < views.length(); j++)
            {
                vq.add(new TermQuery(new Term(Config.VIEW_FIELD, views.getString(j))), BooleanClause.Occur.SHOULD);
            }
            q.add(vq, BooleanClause.Occur.MUST);
            
            writer.deleteDocuments(q);
        }
    }

    private static void
    insertDocuments(IndexWriter writer, String dbname, String group, JSONArray views, JSONArray toInsert)
    throws CorruptIndexException, IOException, JSONException
    {
        for(int i = 0; i < toInsert.length(); i++)
        {
            JSONObject upd = toInsert.getJSONObject(i);
            for(int j = 0; j < views.length(); j++)
            {
                Document d = new Document();
                d.add(new Field(Config.DB_FIELD, dbname, Field.Store.NO, Field.Index.NOT_ANALYZED));
                d.add(new Field(Config.GROUP_FIELD, group, Field.Store.NO, Field.Index.NOT_ANALYZED));
                d.add(new Field(Config.VIEW_FIELD, views.getString(j), Field.Store.NO, Field.Index.NOT_ANALYZED));
                d.add(new Field(Config.DOCID_FIELD, upd.getString("docid"), Field.Store.YES, Field.Index.NOT_ANALYZED));
                d.add(new Field(Config.KEY_FIELD, upd.getString("key"), Field.Store.YES, Field.Index.NOT_ANALYZED));
                d.add(new Field(Config.VALUE_FIELD, upd.get("value").toString(), Field.Store.YES, Field.Index.ANALYZED));
                writer.addDocument(d);
            }
        }
    }
    
    private static IndexWriter
    createIndexWriter(String dbname, String expected_seq)
    throws IOException
    {
        File idxDir = Config.getIndexDirectory(dbname);
        boolean exists = IndexReader.indexExists(idxDir);
        
        if(IndexWriter.isLocked(idxDir.toString()))
        {
            Directory dir = FSDirectory.getDirectory(idxDir);
            IndexWriter.unlock(dir);
            dir.close();
        }
        
        File seqFile = new File(Config.getIndexDirectory(dbname) + File.separator + Config.SEQID);
        if(seqFile.exists())
        {
            RandomAccessFile seq = new RandomAccessFile(seqFile, "r");
            if(!seq.readLine().equals(expected_seq))
            {
                throw new CorruptIndexException("Database Update Sequence Mismatch");
            }
        }

        Analyzer analyzer = new StandardAnalyzer();   
        return new IndexWriter(idxDir, analyzer, !exists, IndexWriter.MaxFieldLength.UNLIMITED);
    }
    
    private static void
    closeIndexWriter(IndexWriter writer, String dbname, String seq)
    throws IOException
    {        
        File seqFile = new File(Config.getIndexDirectory(dbname) + File.separator + Config.SEQID);
        RandomAccessFile data = new RandomAccessFile(seqFile, "rwd");
        data.writeBytes(seq);

        if(writer == null) return;
        writer.commit();
        writer.close();
    }
}