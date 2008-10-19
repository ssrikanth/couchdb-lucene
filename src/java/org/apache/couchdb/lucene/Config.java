package org.apache.couchdb.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.File;
import java.io.IOException;

public class
Config
{
    public static final String DB_FIELD = System.getProperty("couchdb.field.db", "__COUCHDB_DBNAME__");
    public static final String GROUP_FIELD = System.getProperty("couchdb.field.db", "__COUCHDB_GROUP__");
    public static final String DOCID_FIELD = System.getProperty("couchdb.field.docid", "__COUCHDB_DOCID__");
    public static final String VIEW_FIELD = System.getProperty("couchdb.field.view", "__COUCHDB_VIEW__");
    public static final String KEY_FIELD = System.getProperty("couchdb.field.key", "__COUCHDB_KEY__");
    public static final String VALUE_FIELD = System.getProperty("couchdb.field.value", "value");
    public static final String SEQID = System.getProperty("couchdb.seqid", "couchdb.seqid");
    public static final String IDXDIR = System.getProperty("couchdb.directory", "./");

    private static Class ANALYZER;
    static
    {
        try
        {
            String name = System.getProperty("couchdb.analyzer", StandardAnalyzer.class.getName());
            ANALYZER = Class.forName(name);
        }
        
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Failed to load analyzer class: " + e.toString(), e);
        }

        catch(SecurityException se)
        {
            throw new RuntimeException("Failed to load analyzer class: " + se.toString(), se);
        }
    }

    public static Analyzer
    getAnalyzer()
    throws InstantiationException, IllegalAccessException
    {
        return (Analyzer) ANALYZER.newInstance();
    }

    public static File
    getIndexDirectory(String dbname)
    throws IOException
    {
        File ret = new File(IDXDIR + File.separator + "." + dbname + "_design" + File.separator + "lucene");
        if(!ret.exists()) ret.mkdirs();
        return ret;
    }
}