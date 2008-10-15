#! /bin/bash

JQ=./bin/jsearch-query

echo '{"db":"baz","query":{"q":"alias:python","fields":{"alias":"pr*"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"f1:python OR f2:beaker","fields":{"f1":"body","f2":"filename"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"body:python AND title:beaker"}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"\\\"body.foo.there\\\":python AND body:beaker"}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:[1 TO 10]","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:python~","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:pyth*","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:\"beaker foo\"","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:beaker","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:beak*er","fields":{"alias":"body"}}}' | $JQ > /dev/null
echo ""
echo '{"db":"baz","query":{"q":"alias:\"foo* stuff\"","fields":{"alias":"body"}}}' | $JQ > /dev/null

