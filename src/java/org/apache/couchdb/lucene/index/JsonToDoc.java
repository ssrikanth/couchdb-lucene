/*
 * Created on Oct 11, 2008 by Paul Davis.
 *
 */
package org.apache.couchdb.lucene.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.couchdb.lucene.utils.Config;
import org.apache.couchdb.lucene.utils.Numbers;

/**
 * Convert a JSON object into a Lucene Document.
 * Preserves structural infromation in field names.
 */
public class
JsonToDoc
{
    static public List<TermDocPair>
    convert(JSONObject obj, String docidField, String docid)
    throws JSONException
    {
        Document doc = new Document();
        List<TermDocPair> pathDocs = new ArrayList<TermDocPair>();
        convert(pathDocs, doc, "", obj);
        doc.add(new Field(docidField, docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
        pathDocs.add(new TermDocPair(docidField, docid, doc));
        return pathDocs;
    }
    
    static private void
    next(List<TermDocPair> pathDocs, Document doc, String path, Object obj)
    throws JSONException
    {
        if(obj instanceof JSONObject)
        {
            convert(pathDocs, doc, path, (JSONObject) obj);
        }
        else if(obj instanceof JSONArray)
        {
            convert(pathDocs, doc, path, (JSONArray) obj);
        }
        else if(obj instanceof JSONObject.Null)
        {
            convert(pathDocs, doc, path, (JSONObject.Null) obj);
        }
        else if(obj instanceof Boolean)
        {
            convert(pathDocs, doc, path, (Boolean) obj);
        }
        else if(obj instanceof Integer)
        {
            convert(pathDocs, doc, path, (Integer) obj);
        }
        else if(obj instanceof Long)
        {
            convert(pathDocs, doc, path, (Long) obj);
        }
        else if(obj instanceof Double)
        {
            convert(pathDocs, doc, path, (Double) obj);
        }
        else if(obj instanceof String)
        {
            convert(pathDocs, doc, path, (String) obj);
        }
        else
        {
            throw new RuntimeException("Unable to convert class: " + obj.getClass().getName());
        }
    }
    
    static private void
    addPath(List<TermDocPair> pathDocs, Document doc, String path)
    {
        Document pdoc = new Document();
        pdoc.add(new Field(Config.PATHID, path, Field.Store.YES, Field.Index.NOT_ANALYZED));
        pathDocs.add(new TermDocPair(Config.PATHID, path, pdoc));
        doc.add(new Field(Config.PATH_ELEMENT, path, Field.Store.NO, Field.Index.NOT_ANALYZED));
    }
    
    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, JSONObject obj)
    throws JSONException
    {
        Iterator<String> iter = obj.keys();
        while(iter.hasNext())
        {
            String key = (String) iter.next();
            String newpath ;
            if(path.length() == 0)
            {
                newpath = key;
            }
            else
            {
                newpath = path + Config.FIELDSEP + key;
            }
            next(pathDocs, doc, newpath, obj.get(key));
        }
    }
    
    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, JSONArray obj)
    throws JSONException
    {
        for(int i = 0; i < obj.length(); i++)
        {
            String newpath;
            if(path.length() == 0)
            {
                newpath = Config.ARRAYPRE + i;
            }
            else
            {
                newpath = path + Config.FIELDSEP + Config.ARRAYPRE + i;
            }
            next(pathDocs, doc, newpath, obj.get(i));
        }
    }

    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, JSONObject.Null obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, "\0", Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, boolean obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, obj ? "true" : "false", Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, Integer obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, Numbers.intToSortableStr(obj), Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, Long obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, Numbers.longToSortableStr(obj), Field.Store.NO, Field.Index.NOT_ANALYZED));
    }

    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, Double obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, Numbers.doubleToSortableStr(obj), Field.Store.NO, Field.Index.NOT_ANALYZED));
    }
    
    static private void
    convert(List<TermDocPair> pathDocs, Document doc, String path, String obj)
    {
        addPath(pathDocs, doc, path);
        doc.add(new Field(path, obj, Field.Store.NO, Field.Index.ANALYZED));
    }
}

