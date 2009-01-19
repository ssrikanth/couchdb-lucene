package org.apache.couchdb.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class
SequenceId
{
    private File fname = null;
    private Integer seq = null;
    private String sig = null;

    public SequenceId(String fname)
    {
        this.fname = new File(fname);
        this.read();
    }
    
    public boolean
    signatureChanged(int update_seq, String definition)
    throws NoSuchAlgorithmException
    {
        //String curr = this.digest(definition);
        String curr = definition;
        if(this.sig == null || update_seq < this.seq.intValue() || !curr.equalsIgnoreCase(this.sig))
        {
            this.seq = new Integer(0);
            this.sig = curr;
            return true;
        }
        return false;
    }
    
    public int
    get()
    {
        return this.seq.intValue();
    }

    public void
    set(int value)
    {
        this.seq = new Integer(value);
    }
    
    public void
    read()
    {
        if(!this.fname.exists())
        {
            this.seq = new Integer(0);
            this.sig = "";
            return;
        }
        
        try
        {    
            FileInputStream fis = new FileInputStream(this.fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.seq = (Integer) ois.readObject();
            this.sig = (String) ois.readObject();
            ois.close();
            fis.close();
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to read sequence ids from: " + fname);
            exc.printStackTrace();
            this.seq = new Integer(0);
            this.sig = "";
        }
    }
    
    public void
    write()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(this.fname);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.seq);
            oos.writeObject(this.sig);
            oos.close();
            fos.close();
        }
        
        catch(Exception exc)
        {
            System.err.println("Failed to write sequence ids to: " + fname);
            exc.printStackTrace();
        }
    }
    
    private String
    digest(String data)
    throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(data.getBytes());
        BigInteger bi = new BigInteger(md.digest());
        return bi.toString(16);
    }
}