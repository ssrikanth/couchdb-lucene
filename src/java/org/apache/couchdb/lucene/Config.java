package org.apache.couchdb.lucene;

public class
Config
{
    public static final String DOCID = System.getProperty("couchdb.docid", "__COUCHDB_DOCID__");
    public static final String VIEW = System.getProperty("couchdb.field.view", "__COUCHDB_VIEW__");
    public static final String DATA = System.getProperty("couchdb.field.data", "__COUCHDB_DATA__");
    public static final String SEQIDS = System.getProperty("couchdb.seqid", "couchdb.seqid");
    public static final String IDXDIR = System.getProperty("couchdb.directory", "./lucene");
    public static final String DBURI = System.getProperty("couchdb.url", "http://127.0.0.1:5984");
    public static final int BULKSIZE = Integer.parseInt(System.getProperty("couchdb.bulk", "100"));
}