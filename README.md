
CouchDB-Lucene
==============

This is a preliminary implementation of a [Lucene][lucene] indexer for [CouchDB][couchdb].

Dependancies
------------

1. Java - Probably requires 1.5.something
1. CouchDB - trunk
1. Git

Installation
------------

This is assuming you have a working Java runtime environment and a working
Ant installed.

    $ git clone git://github.com/davisp/couchdb-lucene.git couchdb-lucene
    $ cd couchdb-lucene
    $ ant

Configuration
-------------

Assuming the build worked, you'll want to edit your local.ini config file for CouchDB to setup the indexer and query processes. You'll need to add the following sections:

    [update_notification]
    fti_indxer = /path/to/java -jar /path/to/couchdb-lucene/build/couchdb-lucene-0.1-dev.jar index
    
    [external]
    fti = /path/to/java -jar /path/to/couchdb-lucene/build/couchdb-lucene-0.1-dev.jar query
    
    [httpd_db_handlers]
    _fti = {couch_httpd_external, handle_external_req, <<"fti">>}

A couple things to note:

1. Remember to change /path/to/java and /path/to/couchdb-lucene with paths appropriate to your system
1. The `<<"fti">>` specification in the \[httpd\_db\_handlers\] section must match the entry in the \[external\] section.

Indexing
--------

The basic idea behind indexing is that any design doc can specify a list of views to index. Views that are indexed must emit(doc.\_id, string\_value). You can specify as many views as you want for indexing.

Example \_design document:

    {
        "_id": "_design/test",
        "_rev": "232924",
        "views": {
            "foo": {
                "map": "function(doc) {if(doc.body) emit(doc._id, doc.body);}"
            }
        },
        "lucene": ["foo"]
    }

*IMPORTANT*
1. You *must* emit(doc.\_id, value\_to\_index). If you don't emit a key that is the docid, nothing will get indexed.
1. You *must* specify a "lucene" member in your \_design docs that is an array of views to index.
1. That should hopefully be it.

Querying
--------

Parameters:

1. `q`: A query string. This is processed by Lucene's [QueryParser][parser]
1. `limit`: Limit the number of results returned.
1. `skip`: Skip over the first N results.


Examples:

* `http://127.0.0.1:5984/db_name/_fti?q="query terms"`
* `http://127.0.0.1:5984/db_name/_fti?q="foo bar"&limit=2&skip=3`

Results:

`{"rows":2,"docs":[{"docid":"test","score":0.26010897755622864},{"docid":"test2","score":0.2229505479335785}]}`

Feedback
--------

I'm looking for feedback on this whole Lucene business. So use it and report back with any errors or tracebacks or other generally unexpected behavior.


[couchdb]: http://incubator.apache.org/couchdb/ "Apache CouchDB"
[lucene]: http://lucene.apache.org/java/docs/index.html "Java Lucene"
[parser]: http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/queryParser/QueryParser.html "QueryParser"
