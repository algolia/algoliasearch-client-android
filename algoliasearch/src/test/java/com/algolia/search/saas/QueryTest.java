/*
 * Copyright (c) 2012-2016 Algolia
 * http://www.algolia.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.algolia.search.saas;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

<<<<<<<HEAD
        =======
        >>>>>>>master

/**
 * Unit tests for the `Query` class.
 */
public class QueryTest extends RobolectricTestCase  {

    // ----------------------------------------------------------------------
    // Build & parse
    // ----------------------------------------------------------------------

    /** Test serializing a query into a URL query string. */
    @Test
    public void testBuild() {
        Query query = new Query();
        query.set("c", "C");
        query.set("b", "B");
        query.set("a", "A");
        String queryString = query.build();
        assertEquals(queryString, "a=A&b=B&c=C");
    }

    /** Test parsing a query from a URL query string. */
    @Test
    public void testParse() {
        // Build the URL for a query.
        Query query1 = new Query();
        query1.set("foo", "bar");
        query1.set("abc", "xyz");
        String queryString = query1.build();

        // Parse the URL into another query.
        Query query2 = Query.parse(queryString);
        assertEquals(query1, query2);
    }

    /** Test that non-ASCII and special characters are escaped. */
    @Test
    public void testEscape() {
        Query query1 = new Query();
        query1.set("accented", "éêèàôù");
        query1.set("escaped", " %&=#+");
        String queryString = query1.build();
        assertEquals(queryString, "accented=%C3%A9%C3%AA%C3%A8%C3%A0%C3%B4%C3%B9&escaped=%20%25%26%3D%23%2B");

        // Test parsing of escaped characters.
        Query query2 = Query.parse(queryString);
        assertEquals(query1, query2);
    }

    // ----------------------------------------------------------------------
    // Low-level
    // ----------------------------------------------------------------------

    /** Test low-level accessors. */
    @Test
    public void testGetSet() {
        Query query = new Query();

        // Test accessors.
        query.set("a", "A");
        assertEquals(query.get("a"), "A");

        // Test setting null.
        query.set("a", null);
        assertNull(query.get("a"));
        query.set("b", null);
        assertNull(query.get("b"));
    }

    // ----------------------------------------------------------------------
    // High-level
    // ----------------------------------------------------------------------

    @Test
    public void test_minWordSizefor1Typo() {
        Query query1 = new Query();
        assertNull(query1.getMinWordSizefor1Typo());
        query1.setMinWordSizefor1Typo(123);
        assertEquals(query1.getMinWordSizefor1Typo(), new Integer(123));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getMinWordSizefor1Typo(), query1.getMinWordSizefor1Typo());
    }

    @Test
    public void test_minWordSizefor2Typos() {
        Query query1 = new Query();
        assertNull(query1.getMinWordSizefor2Typos());
        query1.setMinWordSizefor2Typos(456);
        assertEquals(query1.getMinWordSizefor2Typos(), new Integer(456));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getMinWordSizefor2Typos(), query1.getMinWordSizefor2Typos());
    }

    @Test
    public void test_minProximity() {
        Query query1 = new Query();
        assertNull(query1.getMinProximity());
        query1.setMinProximity(999);
        assertEquals(query1.getMinProximity(), new Integer(999));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getMinProximity(), query1.getMinProximity());
    }

    @Test
    public void test_getRankingInfo() {
        Query query1 = new Query();
        assertNull(query1.getGetRankingInfo());
        query1.setGetRankingInfo(true);
        assertEquals(query1.getGetRankingInfo(), Boolean.TRUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getGetRankingInfo(), query1.getGetRankingInfo());

        query1.setGetRankingInfo(false);
        assertEquals(query1.getGetRankingInfo(), Boolean.FALSE);
        Query query3 = Query.parse(query1.build());
        assertEquals(query3.getGetRankingInfo(), query1.getGetRankingInfo());
    }

    @Test
    public void test_ignorePlurals() {
        Query query1 = new Query();
        assertNull(query1.getIgnorePlurals());
        query1.setIgnorePlurals(true);
        assertEquals(query1.getIgnorePlurals(), Boolean.TRUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getIgnorePlurals(), query1.getIgnorePlurals());

        query1.setIgnorePlurals(false);
        assertEquals(query1.getIgnorePlurals(), Boolean.FALSE);
        Query query3 = Query.parse(query1.build());
        assertEquals(query3.getIgnorePlurals(), query1.getIgnorePlurals());
    }

    @Test
    public void test_distinct() {
        Query query1 = new Query();
        assertNull(query1.getDistinct());
        query1.setDistinct(100);
        assertEquals(query1.getDistinct(), new Integer(100));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getDistinct(), query1.getDistinct());
    }

    @Test
    public void test_page() {
        Query query1 = new Query();
        assertNull(query1.getPage());
        query1.setPage(0);
        assertEquals(query1.getPage(), new Integer(0));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getPage(), query1.getPage());
    }

    @Test
    public void test_hitsPerPage() {
        Query query1 = new Query();
        assertNull(query1.getHitsPerPage());
        query1.setHitsPerPage(50);
        assertEquals(query1.getHitsPerPage(), new Integer(50));
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getHitsPerPage(), query1.getHitsPerPage());
    }

    @Test
    public void test_allowTyposOnNumericTokens() {
        Query query1 = new Query();
        assertNull(query1.getAllowTyposOnNumericTokens());
        query1.setAllowTyposOnNumericTokens(true);
        assertEquals(query1.getAllowTyposOnNumericTokens(), Boolean.TRUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAllowTyposOnNumericTokens(), query1.getAllowTyposOnNumericTokens());

        query1.setAllowTyposOnNumericTokens(false);
        assertEquals(query1.getAllowTyposOnNumericTokens(), Boolean.FALSE);
        Query query3 = Query.parse(query1.build());
        assertEquals(query3.getAllowTyposOnNumericTokens(), query1.getAllowTyposOnNumericTokens());
    }

    @Test
    public void test_analytics() {
        Query query1 = new Query();
        assertNull(query1.getAnalytics());
        query1.setAnalytics(true);
        assertEquals(query1.getAnalytics(), Boolean.TRUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAnalytics(), query1.getAnalytics());
    }

    @Test
    public void test_synonyms() {
        Query query1 = new Query();
        assertNull(query1.getSynonyms());
        query1.setSynonyms(true);
        assertEquals(query1.getSynonyms(), Boolean.TRUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getSynonyms(), query1.getSynonyms());
    }

    @Test
    public void test_attributesToHighlight() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToHighlight());
        query1.setAttributesToHighlight("foo", "bar");
        assertArrayEquals(query1.getAttributesToHighlight(), new String[]{ "foo", "bar" });
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getAttributesToHighlight(), query1.getAttributesToHighlight());
    }

    @Test
    public void test_attributesToRetrieve() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToRetrieve());
        query1.setAttributesToRetrieve("foo", "bar");
        assertArrayEquals(query1.getAttributesToRetrieve(), new String[]{ "foo", "bar" });
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getAttributesToRetrieve(), query1.getAttributesToRetrieve());
    }

    @Test
    public void test_attributesToSnippet() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToSnippet());
        query1.setAttributesToSnippet("foo:3", "bar:7");
        assertArrayEquals(query1.getAttributesToSnippet(), new String[]{ "foo:3", "bar:7" });
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getAttributesToSnippet(), query1.getAttributesToSnippet());
    }

    @Test
    public void test_query() {
        Query query1 = new Query();
        assertNull(query1.getQuery());
        query1.setQuery("supercalifragilisticexpialidocious");
        assertEquals(query1.getQuery(), "supercalifragilisticexpialidocious");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getQuery(), query1.getQuery());
    }

    @Test
    public void test_queryType() {
        Query query1 = new Query();
        assertNull(query1.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_ALL);
        assertEquals(query1.getQueryType(), Query.QueryType.PREFIX_ALL);
        assertEquals(query1.get("queryType"), "prefixAll");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getQueryType(), query1.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_LAST);
        assertEquals(query1.getQueryType(), Query.QueryType.PREFIX_LAST);
        assertEquals(query1.get("queryType"), "prefixLast");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getQueryType(), query1.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_NONE);
        assertEquals(query1.getQueryType(), Query.QueryType.PREFIX_NONE);
        assertEquals(query1.get("queryType"), "prefixNone");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getQueryType(), query1.getQueryType());

        query1.set("queryType", "invalid");
        assertNull(query1.getQueryType());
    }

    @Test
    public void test_removeWordsIfNoResults() {
        Query query1 = new Query();
        assertNull(query1.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.ALL_OPTIONAL);
        assertEquals(query1.getRemoveWordsIfNoResults(), Query.RemoveWordsIfNoResults.ALL_OPTIONAL);
        assertEquals(query1.get("removeWordsIfNoResults"), "allOptional");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getRemoveWordsIfNoResults(), query1.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.FIRST_WORDS);
        assertEquals(query1.getRemoveWordsIfNoResults(), Query.RemoveWordsIfNoResults.FIRST_WORDS);
        assertEquals(query1.get("removeWordsIfNoResults"), "firstWords");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getRemoveWordsIfNoResults(), query1.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.LAST_WORDS);
        assertEquals(query1.getRemoveWordsIfNoResults(), Query.RemoveWordsIfNoResults.LAST_WORDS);
        assertEquals(query1.get("removeWordsIfNoResults"), "lastWords");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getRemoveWordsIfNoResults(), query1.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.NONE);
        assertEquals(query1.getRemoveWordsIfNoResults(), Query.RemoveWordsIfNoResults.NONE);
        assertEquals(query1.get("removeWordsIfNoResults"), "none");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getRemoveWordsIfNoResults(), query1.getRemoveWordsIfNoResults());

        query1.set("removeWordsIfNoResults", "invalid");
        assertNull(query1.getRemoveWordsIfNoResults());

        query1.set("removeWordsIfNoResults", "allOptional");
        assertEquals(query1.getRemoveWordsIfNoResults(), Query.RemoveWordsIfNoResults.ALL_OPTIONAL);
    }

    @Test
    public void test_typoTolerance() {
        Query query1 = new Query();
        assertNull(query1.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.TRUE);
        assertEquals(query1.getTypoTolerance(), Query.TypoTolerance.TRUE);
        assertEquals(query1.get("typoTolerance"), "true");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getTypoTolerance(), query1.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.FALSE);
        assertEquals(query1.getTypoTolerance(), Query.TypoTolerance.FALSE);
        assertEquals(query1.get("typoTolerance"), "false");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getTypoTolerance(), query1.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.MIN);
        assertEquals(query1.getTypoTolerance(), Query.TypoTolerance.MIN);
        assertEquals(query1.get("typoTolerance"), "min");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getTypoTolerance(), query1.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.STRICT);
        assertEquals(query1.getTypoTolerance(), Query.TypoTolerance.STRICT);
        assertEquals(query1.get("typoTolerance"), "strict");
        query2 = Query.parse(query1.build());
        assertEquals(query2.getTypoTolerance(), query1.getTypoTolerance());

        query1.set("typoTolerance", "invalid");
        assertNull(query1.getTypoTolerance());

        query1.set("typoTolerance", "true");
        assertEquals(query1.getTypoTolerance(), Query.TypoTolerance.TRUE);
    }

    @Test
    public void test_facets() {
        Query query1 = new Query();
        assertNull(query1.getFacets());
        query1.setFacets("foo", "bar");
        assertArrayEquals(query1.getFacets(), new String[]{ "foo", "bar" });
        assertEquals(query1.get("facets"), "[\"foo\",\"bar\"]");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getFacets(), query1.getFacets());
    }

    @Test
    public void test_optionalWords() {
        Query query1 = new Query();
        assertNull(query1.getOptionalWords());
        query1.setOptionalWords("foo", "bar");
        assertArrayEquals(query1.getOptionalWords(), new String[]{ "foo", "bar" });
        assertEquals(query1.get("optionalWords"), "[\"foo\",\"bar\"]");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getOptionalWords(), query1.getOptionalWords());
    }

    @Test
    public void test_restrictSearchableAttributes() {
        Query query1 = new Query();
        assertNull(query1.getRestrictSearchableAttributes());
        query1.setRestrictSearchableAttributes("foo", "bar");
        assertArrayEquals(query1.getRestrictSearchableAttributes(), new String[]{ "foo", "bar" });
        assertEquals(query1.get("restrictSearchableAttributes"), "[\"foo\",\"bar\"]");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getRestrictSearchableAttributes(), query1.getRestrictSearchableAttributes());
    }

    @Test
    public void test_highlightPreTag() {
        Query query1 = new Query();
        assertNull(query1.getHighlightPreTag());
        query1.setHighlightPreTag("<PRE[");
        assertEquals(query1.getHighlightPreTag(), "<PRE[");
        assertEquals(query1.get("highlightPreTag"), "<PRE[");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getHighlightPreTag(), query1.getHighlightPreTag());
    }

    @Test
    public void test_highlightPostTag() {
        Query query1 = new Query();
        assertNull(query1.getHighlightPostTag());
        query1.setHighlightPostTag("]POST>");
        assertEquals(query1.getHighlightPostTag(), "]POST>");
        assertEquals(query1.get("highlightPostTag"), "]POST>");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getHighlightPostTag(), query1.getHighlightPostTag());
    }

    @Test
    public void test_snippetEllipsisText() {
        Query query1 = new Query();
        assertNull(query1.getSnippetEllipsisText());
        query1.setSnippetEllipsisText("…");
        assertEquals(query1.getSnippetEllipsisText(), "…");
        assertEquals(query1.get("snippetEllipsisText"), "…");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getSnippetEllipsisText(), query1.getSnippetEllipsisText());
    }

    @Test
    public void test_analyticsTags() {
        Query query1 = new Query();
        assertNull(query1.getAnalyticsTags());
        query1.setAnalyticsTags("foo", "bar");
        assertArrayEquals(query1.getAnalyticsTags(), new String[]{ "foo", "bar" });
        assertEquals(query1.get("analyticsTags"), "[\"foo\",\"bar\"]");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getAnalyticsTags(), new String[]{ "foo", "bar" });
    }

    @Test
    public void test_disableTypoToleranceOnAttributes() {
        Query query1 = new Query();
        assertNull(query1.getDisableTypoToleranceOnAttributes());
        query1.setDisableTypoToleranceOnAttributes("foo", "bar");
        assertArrayEquals(query1.getDisableTypoToleranceOnAttributes(), new String[]{ "foo", "bar" });
        assertEquals(query1.get("disableTypoToleranceOnAttributes"), "[\"foo\",\"bar\"]");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getDisableTypoToleranceOnAttributes(), query1.getDisableTypoToleranceOnAttributes());
    }

    @Test
    public void test_aroundPrecision() {
        Query query1 = new Query();
        assertNull(query1.getAroundPrecision());
        query1.setAroundPrecision(12345);
        assertEquals(query1.getAroundPrecision(), new Integer(12345));
        assertEquals(query1.get("aroundPrecision"), "12345");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAroundPrecision(), query1.getAroundPrecision());
    }

    @Test
    public void test_aroundRadius() {
        Query query1 = new Query();
        assertNull(query1.getAroundRadius());
        query1.setAroundRadius(987);
        assertEquals(query1.getAroundRadius(), new Integer(987));
        assertEquals(query1.get("aroundRadius"), "987");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAroundRadius(), query1.getAroundRadius());
    }

    @Test
    public void test_aroundLatLngViaIP() {
        Query query1 = new Query();
        assertNull(query1.getAroundLatLngViaIP());
        query1.setAroundLatLngViaIP(true);
        assertEquals(query1.getAroundLatLngViaIP(), Boolean.TRUE);
        assertEquals(query1.get("aroundLatLngViaIP"), "true");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAroundLatLngViaIP(), query1.getAroundLatLngViaIP());
    }

    @Test
    public void test_aroundLatLng() {
        Query query1 = new Query();
        assertNull(query1.getAroundLatLng());
        query1.setAroundLatLng(new Query.LatLng(89.76, -123.45));
        assertEquals(query1.getAroundLatLng(), new Query.LatLng(89.76, -123.45));
        assertEquals(query1.get("aroundLatLng"), "89.76,-123.45");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAroundLatLng(), query1.getAroundLatLng());
    }

    @Test
    public void test_insideBoundingBox() {
        Query query1 = new Query();
        assertNull(query1.getInsideBoundingBox());
        final Query.GeoRect box1 = new Query.GeoRect(new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444));
        query1.setInsideBoundingBox(box1);
        assertArrayEquals(query1.getInsideBoundingBox(), new Query.GeoRect[]{ box1 });
        assertEquals(query1.get("insideBoundingBox"), "11.111111,22.222222,33.333333,44.444444");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getInsideBoundingBox(), query1.getInsideBoundingBox());

        final Query.GeoRect box2 = new Query.GeoRect(new Query.LatLng(-55.555555, -66.666666), new Query.LatLng(-77.777777, -88.888888));
        final Query.GeoRect[] boxes = { box1, box2 };
        query1.setInsideBoundingBox(boxes);
        assertArrayEquals(query1.getInsideBoundingBox(), boxes);
        assertEquals(query1.get("insideBoundingBox"), "11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666,-77.777777,-88.888888");
        query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getInsideBoundingBox(), query1.getInsideBoundingBox());
    }

    @Test
    public void test_insidePolygon() {
        Query query1 = new Query();
        assertNull(query1.getInsidePolygon());
        final Query.LatLng[] box = { new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444), new Query.LatLng(-55.555555, -66.666666) };
        query1.setInsidePolygon(box);
        assertArrayEquals(query1.getInsidePolygon(), box);
        assertEquals(query1.get("insidePolygon"), "11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666");
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query2.getInsidePolygon(), query1.getInsidePolygon());
    }

    @Test
    public void test_tagFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"tag1\", [\"tag2\", \"tag3\"]]");
        Query query1 = new Query();
        assertNull(query1.getTagFilters());
        query1.setTagFilters(VALUE);
        assertEquals(query1.getTagFilters(), VALUE);
        assertEquals(query1.get("tagFilters"), "[\"tag1\",[\"tag2\",\"tag3\"]]");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getTagFilters(), query1.getTagFilters());
    }

    @Test
    public void test_facetFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[[\"category:Book\", \"category:Movie\"], \"author:John Doe\"]");
        Query query1 = new Query();
        assertNull(query1.getFacetFilters());
        query1.setFacetFilters(VALUE);
        assertEquals(query1.getFacetFilters(), VALUE);
        assertEquals(query1.get("facetFilters"), "[[\"category:Book\",\"category:Movie\"],\"author:John Doe\"]");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getFacetFilters(), query1.getFacetFilters());
    }

    @Test
    public void test_advancedSyntax() {
        Query query1 = new Query();
        assertNull(query1.getAdvancedSyntax());
        query1.setAdvancedSyntax(true);
        assertEquals(query1.getAdvancedSyntax(), Boolean.TRUE);
        assertEquals(query1.get("advancedSyntax"), "true");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getAdvancedSyntax(), query1.getAdvancedSyntax());
    }

    @Test
    public void test_removeStopWords() {
        Query query1 = new Query();
        assertNull(query1.getRemoveStopWords());
        query1.setRemoveStopWords(true);
        assertEquals(query1.getRemoveStopWords(), Boolean.TRUE);
        assertEquals(query1.get("removeStopWords"), "true");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getRemoveStopWords(), query1.getRemoveStopWords());
    }

    @Test
    public void test_maxValuesPerFacet() {
        Query query1 = new Query();
        assertNull(query1.getMaxValuesPerFacet());
        query1.setMaxValuesPerFacet(456);
        assertEquals(query1.getMaxValuesPerFacet(), new Integer(456));
        assertEquals(query1.get("maxValuesPerFacet"), "456");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getMaxValuesPerFacet(), query1.getMaxValuesPerFacet());
    }

    @Test
    public void test_minimumAroundRadius() {
        Query query1 = new Query();
        assertNull(query1.getMinimumAroundRadius());
        query1.setMinimumAroundRadius(1000);
        assertEquals(query1.getMinimumAroundRadius(), new Integer(1000));
        assertEquals(query1.get("minimumAroundRadius"), "1000");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getMinimumAroundRadius(), query1.getMinimumAroundRadius());
    }

    @Test
    public void test_numericFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"code=1\", [\"price:0 to 10\", \"price:1000 to 2000\"]]");
        Query query1 = new Query();
        assertNull(query1.getNumericFilters());
        query1.setNumericFilters(VALUE);
        assertEquals(query1.getNumericFilters(), VALUE);
        assertEquals(query1.get("numericFilters"), "[\"code=1\",[\"price:0 to 10\",\"price:1000 to 2000\"]]");
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getNumericFilters(), query1.getNumericFilters());
    }

    @Test
    public void test_filters() {
        final String VALUE = "available=1 AND (category:Book OR NOT category:Ebook) AND publication_date: 1441745506 TO 1441755506 AND inStock > 0 AND author:\"John Doe\"";
        Query query1 = new Query();
        assertNull(query1.getFilters());
        query1.setFilters(VALUE);
        assertEquals(query1.getFilters(), VALUE);
        assertEquals(query1.get("filters"), VALUE);
        Query query2 = Query.parse(query1.build());
        assertEquals(query2.getFilters(), query1.getFilters());
    }

    @Test
    public void test_exactOnSingleWordQuery() {
        Query.ExactOnSingleWordQuery VALUE = Query.ExactOnSingleWordQuery.ATTRIBUTE;
        Query query = new Query();
        assertNull(query.getExactOnSingleWordQuery());

        query.setExactOnSingleWordQuery(VALUE);
        assertEquals(query.getExactOnSingleWordQuery(), VALUE);
        assertEquals(query.get("exactOnSingleWordQuery"), "attribute");
        Query query2 = Query.parse(query.build());
        assertEquals(query2.getExactOnSingleWordQuery(), query.getExactOnSingleWordQuery());
    }

    @Test
    public void test_alternativesAsExact() {
        Query.AlternativesAsExact VALUE1 = Query.AlternativesAsExact.IGNORE_PLURALS;
        Query.AlternativesAsExact VALUE2 = Query.AlternativesAsExact.MULTI_WORDS_SYNONYM;
        final Query.AlternativesAsExact[] VALUES = new Query.AlternativesAsExact[]{VALUE1, VALUE2};

        Query query = new Query();
        assertNull(query.getAlternativesAsExact());

        final Query.AlternativesAsExact[] array = {};
        query.setAlternativesAsExact(array);
        assertArrayEquals(query.getAlternativesAsExact(), array);

        query.setAlternativesAsExact(VALUES);
        assertArrayEquals(VALUES, query.getAlternativesAsExact());

        assertEquals("ignorePlurals,multiWordsSynonym", query.get("alternativesAsExact"));

        Query query2 = Query.parse(query.build());
        assertEquals(query.getExactOnSingleWordQuery(), query2.getExactOnSingleWordQuery());
    }

    @Test
    public void test_aroundRadius_all() {
        final Integer VALUE = 3;
        Query query = new Query();
        assertNull("A new query should have a null aroundRadius.", query.getAroundRadius());

        query.setAroundRadius(VALUE);
        assertEquals("After setting its aroundRadius to a given integer, we should return it from getAroundRadius.", VALUE, query.getAroundRadius());

        String queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=" + VALUE + "'.", queryStr.matches("aroundRadius=" + VALUE));

        query.setAroundRadius(Query.RADIUS_ALL);
        assertEquals("After setting it to RADIUS_ALL, a query should have this aroundRadius value.", Integer.valueOf(Query.RADIUS_ALL), query.getAroundRadius());

        queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=all', not _" + queryStr + "_.", queryStr.matches("aroundRadius=all"));
        Query query2 = Query.parse(query.build());
        assertEquals(query2.getAroundRadius(), query.getAroundRadius());
    }
}