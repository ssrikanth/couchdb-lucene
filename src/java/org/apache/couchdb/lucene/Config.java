package org.apache.couchdb.lucene;

public class
Config
{
    // CouchDB config options
    public static final String COUCHDB_URI = System.getProperty("couchdb.url", "http://127.0.0.1:5984");
    public static final String COUCHDB_DDOC= System.getProperty("couchdb.design", "lucene");
        
    // Indexing Options
    public static final int IDX_BULK_FETCH = Integer.getInteger("couchdb.index.bulk", 500);
    public static final int IDX_RAM_BUFFER = Integer.getInteger("couchdb.index.rambuf", 128); // Megabytes

    // Commit Options
    public static final int COMMIT_INTERVAL = Integer.getInteger("couchdb.commit.interval", 60); // Seconds
    public static final int COMMIT_UPDATES = Integer.getInteger("couchdb.commit.updates", 500);
    
    // Lucene Options
    public static final String LUCENE_IDX_DIR = System.getProperty("couchdb.lucene.directory", "./lucene");
    public static final String LUCENE_STATE_FILE = System.getProperty("couchdb.lucene.state", "couchdb.state");
    
    // Lucene Document field names
    public static final String FIELD_DB = System.getProperty("couchdb.fields.database", "__COUCHDB_DATABASE__");
    public static final String FIELD_DOCID = System.getProperty("couchdb.fields.docid", "__COUCHDB_DOCID__");
    
    // Debug Config
    public static final boolean DEBUG_ENABLED = Boolean.getBoolean("couchdb.debug.enabled");
}