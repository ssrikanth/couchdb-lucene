import time
import unittest

import couchdb

URL = "http://127.0.0.1:5984"
DB = "test_suite_db"

class BasicTests(unittest.TestCase):
    def setUp(self):
        self.server = couchdb.Server(URL)
        if DB in self.server:
            del self.server[DB]
        self.db = self.server.create(DB)
        desdoc = {"views": {
            "foo": {"map": "function(doc) {if(doc.foo) emit(doc._id, doc.foo);}"},
            "bar": {"map": "function(doc) {if(doc.bar) emit(doc._id, doc.bar);}"}
        }}
        self.db["_design/lucene"] = desdoc
    
    def basic_tests(self):
        docs = [{"foo": "This is document %d" % i} for i in range(10)]
        self.db.update(docs)
        resp, data = self.db.resource.get("_fti", q="foo:document")
        self.assertEqual(data["total_rows"], 10)
        self.assertEqual(data["offset"], 0)
        self.assertEqual(isinstance(data["rows"], list), True)
        self.assertEqual(len(data["rows"]), 10)

    def multi_test(self):
        self.db["test1"] = {"foo": "plankton", "bar": "goat"}
        self.db["test2"] = {"foo": "plankton"}
        resp, data = self.db.resource.get("_fti", q="foo:plankton")
        self.assertEqual(data["total_rows"], 2)
        for row in data["rows"]:
            self.assertEqual("id" in row, True)
            self.assertEqual("score" in row, True)
            self.assertEqual(row["id"] in ["test1", "test2"], True)
            self.assertEqual(row["score"] > 0, True)
        #Testing scoring
        resp, data = self.db.resource.get("_fti", q="foo:plankton bar:goat")
        self.assertEqual(data["total_rows"], 2)
        t1score = 0.0
        t2score = 0.0
        for row in data["rows"]:
            if row["id"] == "test1":
                t1score = row["score"]
            else:
                t2score = row["score"]
        self.assertEqual(t1score > t2score, True)
        #Testing limiting
        resp, data = self.db.resource.get("_fti", q="foo:plankton AND bar:goat")
        self.assertEqual(data["total_rows"], 1)
        self.assertEqual(data["rows"][0]["id"], "test1")
