
CouchDB-Lucene
==============

This is a preliminary implementation of indexing a [CouchDB][couchdb]
database via [Lucene][lucene]. Some major selling points for the project
include:

* Arbitrary structural indexing
* Field expansion (Searching fields selected by a second Lucene query)

Dependancies
------------

1. Java - Probably requires at least 1.5.something
1. Ant - I've got 1.6.5 installed
1. Git - No idea on a version
1. Lucene - lucene-core-2.4.0.jar (Included)
1. CouchDB branch [external2][external2]

Installation
------------

This is assuming you have a working Java runtime environment and a working
Ant installed.

    $ git clone git://github.com/davisp/couchdb-lucene.git couchdb-lucene
    $ cd couchdb-lucene
    $ ant

Configuration
-------------

Assuming the build worked, you'll want to edit your local.ini config file for
CouchDB to setup the indexer and searchers. Here's an example local.ini:

    ; CouchDB Configuration Settings
        
    [couchdb]
    ;max_document_size = 4294967296 ; bytes
    
    [httpd]
    ;port = 5984
    ;bind_address = 127.0.0.1
    
    [log]
    level = debug
    
    [update_notification]
    jsearch_indexer=/path/to/couchdb-lucene/bin/jsearch-index
    
    [daemons]
    external={couch_external_manager, start_link, []}
    
    [httpd_db_handlers]
    _external = {couch_httpd_external, handle_external_req}
    
    [external]
    fti={"/path/to/couchdb-lucene/bin/jsearch-query", 1}

Make sure that everything after the [log] section exists (and that you've
replaced /path/to/couchdb-lucene).

Hopefully that's all there is to setting things up. If you have any problems
shoot me an email.

[couchdb]: http://incubator.apache.org/couchdb/ "Apache CouchDB"
[lucene]: http://lucene.apache.org/java/docs/index.html "Java Lucene"
[external2]: http://github.com/davisp/couchdb/tree/external2 "CouchDB External2 Branch"