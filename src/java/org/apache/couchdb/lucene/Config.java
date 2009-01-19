package org.apache.couchdb.lucene;

public class
Config
{
    // CouchDB config options
    public static final String IDXDIR = System.getProperty("couchdb.directory", "./lucene");
    public static final String DBURI = System.getProperty("couchdb.url", "http://127.0.0.1:5984");
    public static final String DESIGN = System.getProperty("couchdb.design", "lucene");
    public static final String SEQID = System.getProperty("couchdb.seqid", "couchdb.seqid");
    public static final int BULKSIZE = Integer.parseInt(System.getProperty("couchdb.bulk", "100"));
    public static final int MAXDBS = Integer.parseInt(System.getProperty("couchdb.maxdbs", "50"));
    
    // Lucene Document field names
    public static final String DOCID = System.getProperty("couchdb.docid", "__COUCHDB_DOCID__");
}