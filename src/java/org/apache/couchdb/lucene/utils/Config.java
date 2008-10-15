package org.apache.couchdb.lucene.utils;

public class
Config
{
    public static final String DOCID = System.getProperty("couchdb.docid", "__COUCHDB_DOCID__");
    public static final String PATHID = System.getProperty("couchdb.pathid", "__COUCHDB_PATHID__");
    public static final String PATH_ELEMENT = System.getProperty("couchdb.path.element", "__COUCHDB_PATH_ELEMENT__");
    public static final String SEQID = System.getProperty("couchdb.seqid", "couchdb.seqid");
    public static final String IDXDIR = System.getProperty("couchdb.directory", "./fti");
    public static final String DBURI = System.getProperty("couchdb.url", "http://localhost:5984");
    public static final int BULKSIZE = Integer.parseInt(System.getProperty("couchdb.bulk", "100"));
    public static final String FIELDSEP = System.getProperty("couchdb.field.sep", "/");
    public static final String ARRAYPRE = System.getProperty("couchdb.array.prefix", "$");
    public static final String DEFAULT = System.getProperty("couchdb.default.field", "text");
}