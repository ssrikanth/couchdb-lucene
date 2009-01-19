CouchDB-Lucene
==============

This is a preliminary implementation of a [Lucene][lucene] indexer for [CouchDB][couchdb].

Dependancies
------------

1. Java - Probably requires 1.5.something
1. CouchDB - trunk
1. Git - something

Installation
------------

This is assuming you have a working Java runtime environment and a working
Ant installed.

    $ git clone git://github.com/davisp/couchdb-lucene.git couchdb-lucene
    $ cd couchdb-lucene
    $ ant

Configuration
-------------

Assuming the build worked, you'll want to edit your local.ini config file for CouchDB to setup the external process
    
    [external]
    fti = /path/to/java -jar /path/to/couchdb-lucene/build/couchdb-lucene-0.1-dev.jar
    
    [httpd_db_handlers]
    _fti = {couch_httpd_external, handle_external_req, <<"fti">>}

A couple things to note:

1. Remember to change /path/to/java and /path/to/couchdb-lucene with paths appropriate to your system
1. The `<<"fti">>` specification in the `[httpd_db_handlers]` section must match the entry in the `[external]` section.

Indexing
--------

The basic idea for indexing is that you specific a `_design/lucene` document that in turn specifies a set of views that will be indexed by couchdb-lucene. You can specify as many views for indexing as you desire.

Example `_design` document:

    {
        "_id": "_design/lucene",
        "_rev": "232924",
        "views": {
            "foo": {
                "map": "function(doc) {if(doc.foo) emit(doc._id, doc.foo);}"
            },
            "bar": {
                "map": "function(doc) {if(doc.bar) emit(doc._id, doc.bar);}"
            }
        },
    }

**IMPORTANT**
1. You **must** `emit(doc._id, value_to_index)`. If you don't emit a key that is the docid, nothing will get indexed.

Querying
--------

Parameters:

1. `q`: A query string. This is processed by Lucene's [QueryParser][parser]
1. `limit`: Limit the number of results returned.
1. `skip`: Skip over the first N results.

The really cool part is that `q` should specify the views you want to search using the QueryParser syntax. For instance, given the example `_design` document above, you can search for things like `foo:value AND bar:pumpkin` and you get back the expected results.

Examples:

* `http://127.0.0.1:5984/db_name/_fti?q=foo:query`
* `http://127.0.0.1:5984/db_name/_fti?q=foo:plankton+bar:goat&limit=1&skip=1`

Results:

    {
        "total_rows":2,
        "offset": 0,
        "rows": [
            {"id":"test","score":0.26010897755622864},
            {"id":"test2","score":0.2229505479335785}
        ]
    }

Feedback
--------

I'm looking for feedback on this whole Lucene business. So use it and report back with any errors or tracebacks or other generally unexpected behavior.

[couchdb]: http://incubator.apache.org/couchdb/ "Apache CouchDB"
[lucene]: http://lucene.apache.org/java/docs/index.html "Java Lucene"
[parser]: http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/queryParser/QueryParser.html "QueryParser"
