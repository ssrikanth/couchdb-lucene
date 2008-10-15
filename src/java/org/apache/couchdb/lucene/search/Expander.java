package org.apache.couchdb.lucene.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.json.JSONException;
import org.json.JSONObject;

import org.apache.couchdb.lucene.utils.Config;

public class
Expander
{
    public static Query
    expand(IndexReader reader, Query q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        System.err.println("Expanding " + q.getClass().getName() + " => " + q);
        if(q instanceof BooleanQuery)
        {
            return expand(reader, (BooleanQuery) q, obj);
        }
        else if(q instanceof ConstantScoreRangeQuery)
        {
            return expand(reader, (ConstantScoreRangeQuery) q, obj);
        }
        else if(q instanceof FuzzyQuery)
        {
            return expand(reader, (FuzzyQuery) q, obj);
        }
        else if(q instanceof PhraseQuery)
        {
            return expand(reader, (PhraseQuery) q, obj);
        }
        else if(q instanceof PrefixQuery)
        {
            return expand(reader, (PrefixQuery) q, obj);
        }
        else if(q instanceof TermQuery)
        {
            return expand(reader, (TermQuery) q, obj);
        }
        else if(q instanceof WildcardQuery)
        {
            return expand(reader, (WildcardQuery) q, obj);
        }
        else
        {
            System.err.println("Unable to expand query: " + q.getClass().getName() + " => " + q);
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, BooleanQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        Iterator<BooleanClause> iter = q.clauses().iterator();
        BooleanQuery expanded = new BooleanQuery(q.isCoordDisabled());
        while(iter.hasNext())
        {
            BooleanClause clause = (BooleanClause) iter.next();
            expanded.add(expand(reader, clause.getQuery(), obj), clause.getOccur());
        }
        return expanded;
    }
    
    public static Query
    expand(IndexReader reader, ConstantScoreRangeQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        if(obj != null && obj.optString(q.getField()) != null)
        {
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, q.getField(), obj.getString(q.getField()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                Query clause = new ConstantScoreRangeQuery(
                    field, q.getLowerVal(), q.getUpperVal(), q.includesLower(), q.includesUpper()
                );
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }
            return ret;
        }
        else
        {
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, FuzzyQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        if(obj != null && obj.optString(q.getTerm().field()) != null)
        {
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, q.getTerm().field(), obj.getString(q.getTerm().field()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                Query clause = new FuzzyQuery(
                    new Term(field, q.getTerm().text()), q.getMinSimilarity(), q.getPrefixLength()
                );
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }
            return ret;
        }
        else
        {
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, PhraseQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        Term terms[] = q.getTerms();
        int pos[] = q.getPositions();
        
        if(terms.length == 0)
        {
            return q;
        }
        
        if(obj != null && obj.optString(terms[0].field()) != null)
        {
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, terms[0].field(), obj.getString(terms[0].field()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                PhraseQuery clause = new PhraseQuery();
                for(int i = 0; i < terms.length; i++)
                {
                    clause.add(new Term(field, terms[i].text()), pos[i]);
                }
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }
            return ret;
        }
        else
        {
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, PrefixQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        if(obj != null && obj.optString(q.getPrefix().field()) != null)
        {
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, q.getPrefix().field(), obj.getString(q.getPrefix().field()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                Query clause = new PrefixQuery(new Term(field, q.getPrefix().text()));
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }

            return ret;
        }
        else
        {
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, TermQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        if(obj != null && obj.optString(q.getTerm().field()) != null)
        {    
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, q.getTerm().field(), obj.getString(q.getTerm().field()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                Query clause = new TermQuery(new Term(field, q.getTerm().text()));
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }
            
            return ret;
        }
        else
        {
            return q;
        }
    }
    
    public static Query
    expand(IndexReader reader, WildcardQuery q, JSONObject obj)
    throws IOException, JSONException, ParseException
    {
        if(obj != null && obj.optString(q.getTerm().field()) != null)
        {
            BooleanQuery ret = new BooleanQuery();
            HashSet<String> fields = expand(reader, q.getTerm().field(), obj.getString(q.getTerm().field()));
            Iterator<String> iter = fields.iterator();
            while(iter.hasNext())
            {
                String field = (String) iter.next();
                Query clause = new WildcardQuery(new Term(field, q.getTerm().text()));
                ret.add(new BooleanClause(clause, BooleanClause.Occur.SHOULD));
            }
            return ret;
        }
        else
        {
            return q;
        }        
    }
    
    public static HashSet<String>
    expand(IndexReader reader, String field, String query)
    throws IOException, ParseException
    {
        HashSet<String> fields = new HashSet<String>();
        QueryParser parser = new QueryParser(Config.PATHID, new StandardAnalyzer());
        Query q = parser.parse(query);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.search(q, new FieldCollector(reader, fields));
        return fields;
    }
    
    private static class
    FieldCollector
    extends HitCollector
    {
        private IndexReader reader;
        private HashSet<String> fields;
        
        public FieldCollector(IndexReader reader, HashSet<String> fields)
        {
            this.reader = reader;
            this.fields = fields;
        }
        
        public void
        collect(int docid, float score)
        {
            try
            {
                Document doc = reader.document(docid);
                if(doc.get(Config.PATHID) != null)
                {
                    fields.add(doc.get(Config.PATHID));
                }
            }
            
            catch(IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }
}