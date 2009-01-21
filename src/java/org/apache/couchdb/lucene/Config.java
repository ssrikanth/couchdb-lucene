package org.apache.couchdb.lucene;

public class
Config
{
    // CouchDB config options
    public static final String COUCHDB_URI = System.getProperty("couchdb.url", "http://127.0.0.1:5984");
    public static final String COUCHDB_DDOC= System.getProperty("couchdb.design", "lucene");
    
    // Threading Options
    public static final int COUCHDB_MIN_THREADS = Integer.getInteger("couchdb.threads.min", 25);
    public static final int COUCHDB_MAX_THREADS = Integer.getInteger("couchdb.threads.max", 100);
    public static final int COUCHDB_THREAD_TIMEOUT = Integer.getInteger("couchdb.threads.timeout", 500);
    
    // Defaults that could be overridden in the design doc.
    public static final int COUCHDB_BULK_GET = Integer.getInteger("couchdb.bulk", 100);
    
    // Lucene Options
    public static final String LUCENE_IDX_DIR = System.getProperty("couchdb.lucene.directory", "./lucene");
    public static final String LUCENE_STATE_FILE = System.getProperty("couchdb.lucene.state", "couchdb.state");
    public static final int LUCENE_MAX_RAM = Integer.getInteger("couchdb.lucene.ram", 512);
    
    // Lucene Document field names
    public static final String FIELD_DB = System.getProperty("couchdb.fields.database", "__COUCHDB_DATABASE__");
    public static final String FIELD_DOCID = System.getProperty("couchdb.fields.docid", "__COUCHDB_DOCID__");
}