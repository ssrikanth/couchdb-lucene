package org.apache.couchdb.lucene;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class
IndexCache
extends LinkedHashMap<String, Index>
{
    private final int max_size;
    
    public
    IndexCache(int max_size)
    {
        this.max_size = max_size;
    }

    public boolean
    removeEldestEntry(Map.Entry<String, Index> entry)
    {
        if(this.size() > this.max_size)
        {
            Index idx = (Index) entry.getValue();
            idx.close();
            return true;
        }
        
        return false;
    }
    
    public void
    closeAll()
    {
        Iterator<Index> iter = this.values().iterator();
        while(iter.hasNext())
        {
            Index idx = (Index) iter.next();
            idx.close();
        }
    }
}