package org.apache.couchdb.lucene;

public class
IndexResult
{
    public final String dbname;
    public final boolean finished;
    public final String signature;
    public final int curr_seq;

    public
    IndexResult(String dbname, boolean finished, String signature, int curr_seq)
    {
        this.dbname = dbname;
        this.finished = finished;
        this.signature = signature;
        this.curr_seq = curr_seq;
    }
}