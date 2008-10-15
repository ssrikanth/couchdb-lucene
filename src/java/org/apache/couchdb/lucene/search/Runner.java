package org.apache.couchdb.lucene.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import org.json.JSONObject;
import org.json.JSONStringer;

import org.apache.couchdb.lucene.utils.Config;

public class
Runner
{
    static private BufferedReader input = null;

    public static void
    main(String args[])
    throws Exception
    {
        input = new BufferedReader(new InputStreamReader(System.in));
        String data = null;

        while((data = input.readLine()) != null)
        {
            String ret = null ;
            System.err.println(data);

            try
            {
                JSONObject req = new JSONObject(data);
                String dbname = req.getString("db");
                String query = req.getJSONObject("query").getString("q");
                int count = req.getJSONObject("query").optInt("count", 25);
                int offset = req.getJSONObject("query").optInt("offset", 0);
                
                File idxDir = new File(Config.IDXDIR + File.separator + dbname);
                if(!IndexReader.indexExists(idxDir))
                {
                    throw new RuntimeException("Failed to find index for database '" + dbname + "'.");
                }
                
                IndexReader reader = IndexReader.open(idxDir);
                Searcher searcher = new IndexSearcher(reader);
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser parser = new QueryParser(Config.DEFAULT, analyzer);
                Query q = parser.parse(query);
                System.err.println("Before: " + q);
                q = Expander.expand(reader, q, req.getJSONObject("query").optJSONObject("fields"));
                System.err.println("After: " + q);

                TopDocs hits = searcher.search(q, null, offset + count, new Sort(Config.DOCID));
                
                JSONStringer out = new JSONStringer();
                out.object()
                    .key("code").value(200)
                    .key("json").object()
                        .key("rows").value(hits.totalHits)
                        .key("docs").array();
                for(int i = offset; i < hits.scoreDocs.length; i++)
                {
                    Document d = reader.document(hits.scoreDocs[i].doc);
                    out.object()
                        .key("docid").value(new String(d.get(Config.DOCID)))
                        .key("score").value(hits.scoreDocs[i].score)
                    .endObject();
                }
                out.endArray().endObject().endObject();
             
                ret = out.toString();
            }
            catch(Exception ex)
            {
                ex.printStackTrace(System.err);
                ret = new JSONStringer().object()
                            .key("code").value(500)
                            .key("body").value("Failed to execute query.")
                        .endObject().toString();
            }
            
            finally
            {
                System.out.println(ret);
            }
        }
    }
}
