
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
1. CouchDB branch [action2][action2]

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

Usage
-----

To have your database index you'll need to trigger an update notification.
This can be achieved by simply saving a document in Futon (without necessarily
editing anything). After this, the indexer should take off and index the db.
I haven't tested this on a large dataset, so I'm not certain how it'll perform
on a huge dataset (probably not too well).

To try your first query, try fetching a url like:

    http://127.0.0.1:5984/dbname/_external/fti?q=body/field:my_query

This would match a document that had a structure like:

    {
        "_id": "foo",
        "_rev": "220130",
        "body": {
            "field": "some text including my_query"
        }
    }

Notice that ``body/field`` refers to the ``field`` child of the ``body``
member. You can specify array indexes as well using a ``$\d+`` pattern. As in:

    {
        "_id": "bar",
        "_rev": "9831221",
        "fruit": ["apple", "orange", "strawberry"]
    }

Would be indexed with the following fields:

* ``_id``
* ``_rev``
* ``fruit/$0``
* ``fruit/$1``
* ``fruit/$2``

So, to see if "apple" is the second element of the fruit array, the query
would be ``?q=fruit/$1:apple``.

Field Expansion
---------------

Obviously, only being able to test individual members of an array or
specifically named fields wouldn't be very useful. But, we've got a trick up
our sleeves:

    ?q=alias_foo:apple&fields={"alias_foo":"fruit/*"}

Will search for apple in all fields below fruit. There's nothing special about
using ``alias_foo`` as the field name. The code only looks at each field
specified in the input query and if a member of the same name exists in the
fields object, the corresponding query is run and the original query becomes
an OR conjunction of all found fields.

After Now
---------

Things on the todo list:

1. Number parsing needs to be added to the query side.
1. Configuration work. Need to flesh out how to specify a configuration in a
design document and what exactly should be configurable.
1. Efficiency work. There are quite a few places that need to be cleaned up
and made to not be so blatantly inefficient.
1. The field expansion may need to be re-jiggered a bit. I sent an email to
the Lucene ML but haven't gotten a reply yet.
1. Stability work. I need people to start abusing this and the external branch
to see if there are common errors showing up that should get informative
messages returned.

[couchdb]: http://incubator.apache.org/couchdb/ "Apache CouchDB"
[lucene]: http://lucene.apache.org/java/docs/index.html "Java Lucene"
[action2]: http://github.com/jchris/couchdb/tree/action2 "CouchDB Action2 Branch"
