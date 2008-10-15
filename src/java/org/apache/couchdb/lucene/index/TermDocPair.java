package org.apache.couchdb.lucene.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

public class
TermDocPair
{
    public Term term;
    public Document doc;
    
    public TermDocPair(String field, String text, Document doc)
    {
        this.term = new Term(field, text);
        this.doc = doc;
    }
    
    public TermDocPair(Term term, Document doc)
    {
        this.term = term;
        this.doc = doc;
    }
}