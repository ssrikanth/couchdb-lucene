package org.apache.couchdb.lucene.index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.couchdb.lucene.db.Database;
import org.apache.couchdb.lucene.utils.Config;

public class
Index
{
    private File idxDir = null;
    private Database db = null;
    private int bulkSize = 1;
    private Analyzer analyzer = null;
    private Directory directory = null;
    private IndexWriter writer = null;
    
    public Index(File idxDir, Database db, int bulkSize)
    throws IOException
    {
        this.idxDir = idxDir;
        if(!this.idxDir.exists())
        {
            this.idxDir.mkdirs();
        }
        this.bulkSize = bulkSize;

        this.db = db;
        this.analyzer = new StandardAnalyzer();
        this.directory = FSDirectory.getDirectory(this.idxDir);
        boolean exists = IndexReader.indexExists(this.directory);
        
        if(IndexWriter.isLocked(this.directory))
        {
            IndexWriter.unlock(this.directory);
        }
        
        this.writer = new IndexWriter(this.directory, this.analyzer, !exists, IndexWriter.MaxFieldLength.UNLIMITED);
    }

    public void
    close()
    throws CorruptIndexException, IOException
    {
        if(this.writer != null) this.writer.close();
        if(this.directory != null) this.directory.close();
              
        this.writer = null;
        this.directory = null;
    }

    public void
    update()
    throws Exception
    {
        JSONArray updates = null;
        do
        {
            updates = db.nextBySequence(getCurrUpdateSequence(), this.bulkSize).getJSONArray("rows");
            for(int i = 0; i < updates.length(); i++)
            {
                JSONObject row = updates.getJSONObject(i);
                String curr_seq = row.getString("key");
                String docid = row.getString("id");
                String rev = row.getJSONObject("value").getString("rev");
                boolean deleted = row.getJSONObject("value").optBoolean("deleted", false);

                if(deleted)
                {
                    deleteDocument(docid);
                }
                else
                {
                    updateDocument(docid, rev);
                }

                setCurrUpdateSequence(curr_seq);
            }

            this.writer.commit();

        } while(updates.length() > 0);

        return;
    }
   
    public static void
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
    
    private void
    updateDocument(String docid, String rev)
    throws Exception
    {
        System.err.println("Indexing document: " + docid + " Rev: " + rev);
        JSONObject obj = this.db.getDoc(docid, rev);
        List<TermDocPair> docs = JsonToDoc.convert(obj, Config.DOCID, docid);
        Iterator<TermDocPair> iter = docs.iterator();
        while(iter.hasNext())
        {
            TermDocPair pair = (TermDocPair) iter.next();
            this.writer.updateDocument(pair.term, pair.doc);
        }
    }
    
    private void
    deleteDocument(String docid)
    throws Exception
    {
        this.writer.deleteDocuments(new Term(Config.DOCID, docid));
    }
    
    private String
    getCurrUpdateSequence()
    throws IOException
    {
        File seqFile = new File(this.idxDir.toString() + File.separator + Config.SEQID);
        if(!seqFile.exists())
        {
            return "0";
        }
        RandomAccessFile seq = new RandomAccessFile(seqFile, "r");
        return seq.readLine();
    }
    
    private void
    setCurrUpdateSequence(String seq)
    throws IOException
    {
        File seqFile = new File(this.idxDir.toString() + File.separator + Config.SEQID);
        RandomAccessFile data = new RandomAccessFile(seqFile, "rwd");
        data.writeBytes(seq);
    }
}