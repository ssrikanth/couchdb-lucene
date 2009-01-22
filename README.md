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
1. By default, indexing is only committed every 500 updates or every 60 seconds, whichever comes first.

Config options can be added to the command line in the `[external]` section. These are just a few of the possible settings. For a full list see `org.apache.couchdb.lucene.Config`.

    -Xcouchdb.index.bulk=500        # Number of documents to request at once
    -Xcouchdb.index.rambuf=128      # RAM buffer size in MiB
    -Xcouchdb.commit.interval=60    # Commit changes a minimum of every N seconds
    -Xcouchdb.commit.updates=500    # Commit changes a minimum of every N updates (Node wide)
    -Xcouchdb.debug.enabled="true"  # Enable DEBUG mode that addes a few query string parameters.

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

**IMPORTANT** You **must** `emit(doc._id, value_to_index)`. If you don't emit a key that is the docid, nothing will get indexed.

Querying
--------

Parameters:

1. `q`: A query string. This is processed by Lucene's [QueryParser][parser]
1. `limit`: Limit the number of results returned.
1. `skip`: Skip over the first N results.

Debug Parameters:

1. `debug=true`: Wait until the indexing catches up to the database update_seq that was passed with this request. If -Xcouchdb.debug.enabled="true" is specified on the command line, this defaults to true. If debug is not enabled, this parameter has no effect.
1. `destroy=true`: Remove the entire database from the Lucene index. Only available when debug mode is enabled.

Example URLs:

* `http://127.0.0.1:5984/db_name/_fti?q=foo:query`
* `http://127.0.0.1:5984/db_name/_fti?q=foo:plankton+bar:goat&limit=1&skip=1`

Example Results:

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
