/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Shamelessly stolen from Solr.

 package org.apache.couchdb.lucene.utils;

/**
 * @version $Id$
 */
public class
Numbers
{
    public static String
    intToSortableStr(int val) {
        char[] arr = new char[3];
        intToSortableStr(val,arr,0);
        return new String(arr,0,3);
    }

    public static String
    intToSortableStr(String val)
    {
        return intToSortableStr(Integer.parseInt(val));
    }

    public static String
    sortableStrToInt(String val)
    {
        int ival = sortableStrToInt(val,0,3);
        return Integer.toString(ival);
    }
    
    public static String
    longToSortableStr(long val)
    {
        char[] arr = new char[5];
        longToSortableStr(val,arr,0);
        return new String(arr,0,5);
    }

    public static String
    longToSortableStr(String val)
    {
        return longToSortableStr(Long.parseLong(val));
    }

    public static String
    SortableStr2long(String val)
    {
        long ival = sortableStrToLong(val,0,5);
        return Long.toString(ival);
    }

    //
    // IEEE floating point format is defined so that it sorts correctly
    // when interpreted as a signed integer (or signed long in the case
    // of a double) for positive values.  For negative values, all the bits except
    // the sign bit must be inverted.
    // This correctly handles all possible float values including -Infinity and +Infinity.
    // Note that in float-space, NaN<x is false, NaN>x is false, NaN==x is false, NaN!=x is true
    // for all x (including NaN itself).  Internal to Solr, NaN==NaN is true and NaN
    // sorts higher than Infinity, so a range query of [-Infinity TO +Infinity] will
    // exclude NaN values, but a query of "NaN" will find all NaN values.
    // Also, -0==0 in float-space but -0<0 after this transformation.
    //

    public static String
    doubleToSortableStr(double val)
    {
        long f = Double.doubleToRawLongBits(val);
        if (f<0) f ^= 0x7fffffffffffffffL;
        return longToSortableStr(f);
    }

    public static String
    doubleToSortableStr(String val)
    {
        return doubleToSortableStr(Double.parseDouble(val));
    }

    public static double
    sortableStrToDouble(String val) {
        long f = sortableStrToLong(val,0,6);
        if (f<0) f ^= 0x7fffffffffffffffL;
        return Double.longBitsToDouble(f);
    }

    public static String
    SortableStr2doubleStr(String val)
    {
        return Double.toString(sortableStrToDouble(val));
    }

    // uses binary representation of an int to build a string of
    // chars that will sort correctly.  Only char ranges
    // less than 0xd800 will be used to avoid UCS-16 surrogates.
    public static int
    intToSortableStr(int val, char[] out, int offset) {
        val += Integer.MIN_VALUE;
        out[offset++] = (char)(val >>> 24);
        out[offset++] = (char)((val >>> 12) & 0x0fff);
        out[offset++] = (char)(val & 0x0fff);
        return 3;
    }

    public static int
    sortableStrToInt(String sval, int offset, int len) {
        int val = sval.charAt(offset++) << 24;
        val |= sval.charAt(offset++) << 12;
        val |= sval.charAt(offset++);
        val -= Integer.MIN_VALUE;
        return val;
    }

    // uses binary representation of an int to build a string of
    // chars that will sort correctly.  Only char ranges
    // less than 0xd800 will be used to avoid UCS-16 surrogates.
    // we can use the lowest 15 bits of a char, (or a mask of 0x7fff)
    public static int
    longToSortableStr(long val, char[] out, int offset)
    {
        val += Long.MIN_VALUE;
        out[offset++] = (char)(val >>>60);
        out[offset++] = (char)(val >>>45 & 0x7fff);
        out[offset++] = (char)(val >>>30 & 0x7fff);
        out[offset++] = (char)(val >>>15 & 0x7fff);
        out[offset] = (char)(val & 0x7fff);
        return 5;
    }

    public static long
    sortableStrToLong(String sval, int offset, int len)
    {
        long val = (long)(sval.charAt(offset++)) << 60;
        val |= ((long)sval.charAt(offset++)) << 45;
        val |= ((long)sval.charAt(offset++)) << 30;
        val |= sval.charAt(offset++) << 15;
        val |= sval.charAt(offset);
        val -= Long.MIN_VALUE;
        return val;
    }
}