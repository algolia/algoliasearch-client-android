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

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the `Query` class.
 */
public class QueryTest extends RobolectricTestCase {

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
        assertEquals("a=A&b=B&c=C", queryString);
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
        assertEquals("accented=%C3%A9%C3%AA%C3%A8%C3%A0%C3%B4%C3%B9&escaped=%20%25%26%3D%23%2B", queryString);

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
        assertEquals("A", query.get("a"));

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
        assertEquals(new Integer(123), query1.getMinWordSizefor1Typo());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getMinWordSizefor1Typo(), query2.getMinWordSizefor1Typo());
    }

    @Test
    public void test_minWordSizefor2Typos() {
        Query query1 = new Query();
        assertNull(query1.getMinWordSizefor2Typos());
        query1.setMinWordSizefor2Typos(456);
        assertEquals(new Integer(456), query1.getMinWordSizefor2Typos());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getMinWordSizefor2Typos(), query2.getMinWordSizefor2Typos());
    }

    @Test
    public void test_minProximity() {
        Query query1 = new Query();
        assertNull(query1.getMinProximity());
        query1.setMinProximity(999);
        assertEquals(new Integer(999), query1.getMinProximity());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getMinProximity(), query2.getMinProximity());
    }

    @Test
    public void test_getRankingInfo() {
        Query query1 = new Query();
        assertNull(query1.getGetRankingInfo());
        query1.setGetRankingInfo(true);
        assertEquals(Boolean.TRUE, query1.getGetRankingInfo());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getGetRankingInfo(), query2.getGetRankingInfo());

        query1.setGetRankingInfo(false);
        assertEquals(Boolean.FALSE, query1.getGetRankingInfo());
        Query query3 = Query.parse(query1.build());
        assertEquals(query1.getGetRankingInfo(), query3.getGetRankingInfo());
    }

    @Test
    public void test_ignorePlurals() {
        // No value
        Query query = new Query();
        assertFalse("By default, ignorePlurals should be disabled.", query.getIgnorePlurals().enabled);

        // Boolean values
        query.setIgnorePlurals(true);
        assertEquals("A true boolean should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("A true boolean should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals(false);
        assertEquals("A false boolean should disable ignorePlurals.", Boolean.FALSE, query.getIgnorePlurals().enabled);
        assertEquals("A false boolean should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        // List values
        query.setIgnorePlurals((List<String>) null);
        assertFalse("A null list value should disable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertEquals("A null list value should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals(new ArrayList<String>());
        assertFalse("Setting an empty list should disable ignorePlurals.", query.getIgnorePlurals().enabled);

        ArrayList<String> languageCodes = new ArrayList<>(java.util.Arrays.asList("en", "fr"));
        query.setIgnorePlurals(languageCodes);
        assertTrue("Setting a non-empty list should enable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertNotNull("The language codes should not be null", query.getIgnorePlurals().languageCodes);
        assertEquals("Two language codes should be in ignorePlurals.", 2, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The first language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains(languageCodes.get(0)));
        assertTrue("The second language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains(languageCodes.get(1)));

        // String[] values
        query.setIgnorePlurals("");
        assertFalse("An empty string should disable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertEquals("A empty string should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals("en");
        assertEquals("A single language code should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("A single language code should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());
        assertEquals("One language code should be in ignorePlurals.", 1, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("en"));

        query.setIgnorePlurals("en", "fr");
        assertEquals("Two language codes should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("Two language codes should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());
        assertEquals("Two language codes should be in ignorePlurals.", 2, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The first language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("en"));
        assertTrue("The second language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("fr"));
    }

    @Test
    public void test_distinct() {
        Query query1 = new Query();
        assertNull(query1.getDistinct());
        query1.setDistinct(100);
        assertEquals(new Integer(100), query1.getDistinct());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getDistinct(), query2.getDistinct());
    }

    @Test
    public void test_page() {
        Query query1 = new Query();
        assertNull(query1.getPage());
        query1.setPage(0);
        assertEquals(new Integer(0), query1.getPage());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getPage(), query2.getPage());
    }

    @Test
    public void test_hitsPerPage() {
        Query query1 = new Query();
        assertNull(query1.getHitsPerPage());
        query1.setHitsPerPage(50);
        assertEquals(new Integer(50), query1.getHitsPerPage());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getHitsPerPage(), query2.getHitsPerPage());
    }

    @Test
    public void test_allowTyposOnNumericTokens() {
        Query query1 = new Query();
        assertNull(query1.getAllowTyposOnNumericTokens());
        query1.setAllowTyposOnNumericTokens(true);
        assertEquals(Boolean.TRUE, query1.getAllowTyposOnNumericTokens());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAllowTyposOnNumericTokens(), query2.getAllowTyposOnNumericTokens());

        query1.setAllowTyposOnNumericTokens(false);
        assertEquals(Boolean.FALSE, query1.getAllowTyposOnNumericTokens());
        Query query3 = Query.parse(query1.build());
        assertEquals(query1.getAllowTyposOnNumericTokens(), query3.getAllowTyposOnNumericTokens());
    }

    @Test
    public void test_analytics() {
        Query query1 = new Query();
        assertNull(query1.getAnalytics());
        query1.setAnalytics(true);
        assertEquals(Boolean.TRUE, query1.getAnalytics());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAnalytics(), query2.getAnalytics());
    }

    @Test
    public void test_synonyms() {
        Query query1 = new Query();
        assertNull(query1.getSynonyms());
        query1.setSynonyms(true);
        assertEquals(Boolean.TRUE, query1.getSynonyms());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getSynonyms(), query2.getSynonyms());
    }

    @Test
    public void test_attributesToHighlight() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToHighlight());
        query1.setAttributesToHighlight("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getAttributesToHighlight());
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getAttributesToHighlight(), query2.getAttributesToHighlight());
    }

    @Test
    public void test_attributesToRetrieve() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToRetrieve());
        query1.setAttributesToRetrieve("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getAttributesToRetrieve());
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getAttributesToRetrieve(), query2.getAttributesToRetrieve());
    }

    @Test
    public void test_attributesToSnippet() {
        Query query1 = new Query();
        assertNull(query1.getAttributesToSnippet());
        query1.setAttributesToSnippet("foo:3", "bar:7");
        assertArrayEquals(new String[]{ "foo:3", "bar:7" }, query1.getAttributesToSnippet());
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getAttributesToSnippet(), query2.getAttributesToSnippet());
    }

    @Test
    public void test_query() {
        Query query1 = new Query();
        assertNull(query1.getQuery());
        query1.setQuery("supercalifragilisticexpialidocious");
        assertEquals("supercalifragilisticexpialidocious", query1.getQuery());
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getQuery(), query2.getQuery());
    }

    @Test
    public void test_queryType() {
        Query query1 = new Query();
        assertNull(query1.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_ALL);
        assertEquals(Query.QueryType.PREFIX_ALL, query1.getQueryType());
        assertEquals("prefixAll", query1.get("queryType"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getQueryType(), query2.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_LAST);
        assertEquals(Query.QueryType.PREFIX_LAST, query1.getQueryType());
        assertEquals("prefixLast", query1.get("queryType"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getQueryType(), query2.getQueryType());

        query1.setQueryType(Query.QueryType.PREFIX_NONE);
        assertEquals(query1.getQueryType(), Query.QueryType.PREFIX_NONE);
        assertEquals(query1.get("queryType"), "prefixNone");
        query2 = Query.parse(query1.build());
        assertEquals(query1.getQueryType(), query2.getQueryType());

        query1.set("queryType", "invalid");
        assertNull(query1.getQueryType());
    }

    @Test
    public void test_removeWordsIfNoResults() {
        Query query1 = new Query();
        assertNull(query1.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.ALL_OPTIONAL);
        assertEquals(Query.RemoveWordsIfNoResults.ALL_OPTIONAL, query1.getRemoveWordsIfNoResults());
        assertEquals("allOptional", query1.get("removeWordsIfNoResults"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getRemoveWordsIfNoResults(), query2.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.FIRST_WORDS);
        assertEquals(Query.RemoveWordsIfNoResults.FIRST_WORDS, query1.getRemoveWordsIfNoResults());
        assertEquals("firstWords", query1.get("removeWordsIfNoResults"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getRemoveWordsIfNoResults(), query2.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.LAST_WORDS);
        assertEquals(Query.RemoveWordsIfNoResults.LAST_WORDS, query1.getRemoveWordsIfNoResults());
        assertEquals("lastWords", query1.get("removeWordsIfNoResults"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getRemoveWordsIfNoResults(), query2.getRemoveWordsIfNoResults());

        query1.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.NONE);
        assertEquals(Query.RemoveWordsIfNoResults.NONE, query1.getRemoveWordsIfNoResults());
        assertEquals("none", query1.get("removeWordsIfNoResults"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getRemoveWordsIfNoResults(), query2.getRemoveWordsIfNoResults());

        query1.set("removeWordsIfNoResults", "invalid");
        assertNull(query1.getRemoveWordsIfNoResults());

        query1.set("removeWordsIfNoResults", "allOptional");
        assertEquals(Query.RemoveWordsIfNoResults.ALL_OPTIONAL, query1.getRemoveWordsIfNoResults());
    }

    @Test
    public void test_typoTolerance() {
        Query query1 = new Query();
        assertNull(query1.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.TRUE);
        assertEquals(Query.TypoTolerance.TRUE, query1.getTypoTolerance());
        assertEquals("true", query1.get("typoTolerance"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getTypoTolerance(), query2.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.FALSE);
        assertEquals(Query.TypoTolerance.FALSE, query1.getTypoTolerance());
        assertEquals("false", query1.get("typoTolerance"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getTypoTolerance(), query2.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.MIN);
        assertEquals(Query.TypoTolerance.MIN, query1.getTypoTolerance());
        assertEquals("min", query1.get("typoTolerance"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getTypoTolerance(), query2.getTypoTolerance());

        query1.setTypoTolerance(Query.TypoTolerance.STRICT);
        assertEquals(Query.TypoTolerance.STRICT, query1.getTypoTolerance());
        assertEquals("strict", query1.get("typoTolerance"));
        query2 = Query.parse(query1.build());
        assertEquals(query1.getTypoTolerance(), query2.getTypoTolerance());

        query1.set("typoTolerance", "invalid");
        assertNull(query1.getTypoTolerance());

        query1.set("typoTolerance", "true");
        assertEquals(Query.TypoTolerance.TRUE, query1.getTypoTolerance());
    }

    @Test
    public void test_facets() {
        Query query1 = new Query();
        assertNull(query1.getFacets());
        query1.setFacets("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getFacets());
        assertEquals("[\"foo\",\"bar\"]", query1.get("facets"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getFacets(), query2.getFacets());
    }

    @Test
    public void test_optionalWords() {
        Query query1 = new Query();
        assertNull(query1.getOptionalWords());
        query1.setOptionalWords("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getOptionalWords());
        assertEquals("[\"foo\",\"bar\"]", query1.get("optionalWords"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getOptionalWords(), query2.getOptionalWords());
    }

    @Test
    public void test_restrictSearchableAttributes() {
        Query query1 = new Query();
        assertNull(query1.getRestrictSearchableAttributes());
        query1.setRestrictSearchableAttributes("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getRestrictSearchableAttributes());
        assertEquals("[\"foo\",\"bar\"]", query1.get("restrictSearchableAttributes"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getRestrictSearchableAttributes(), query2.getRestrictSearchableAttributes());
    }

    @Test
    public void test_highlightPreTag() {
        Query query1 = new Query();
        assertNull(query1.getHighlightPreTag());
        query1.setHighlightPreTag("<PRE[");
        assertEquals("<PRE[", query1.getHighlightPreTag());
        assertEquals("<PRE[", query1.get("highlightPreTag"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getHighlightPreTag(), query2.getHighlightPreTag());
    }

    @Test
    public void test_highlightPostTag() {
        Query query1 = new Query();
        assertNull(query1.getHighlightPostTag());
        query1.setHighlightPostTag("]POST>");
        assertEquals("]POST>", query1.getHighlightPostTag());
        assertEquals("]POST>", query1.get("highlightPostTag"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getHighlightPostTag(), query2.getHighlightPostTag());
    }

    @Test
    public void test_snippetEllipsisText() {
        Query query1 = new Query();
        assertNull(query1.getSnippetEllipsisText());
        query1.setSnippetEllipsisText("…");
        assertEquals("…", query1.getSnippetEllipsisText());
        assertEquals("…", query1.get("snippetEllipsisText"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getSnippetEllipsisText(), query2.getSnippetEllipsisText());
    }

    @Test
    public void test_analyticsTags() {
        Query query1 = new Query();
        assertNull(query1.getAnalyticsTags());
        query1.setAnalyticsTags("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getAnalyticsTags());
        assertEquals("[\"foo\",\"bar\"]", query1.get("analyticsTags"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getAnalyticsTags(), query2.getAnalyticsTags());
    }

    @Test
    public void test_disableTypoToleranceOnAttributes() {
        Query query1 = new Query();
        assertNull(query1.getDisableTypoToleranceOnAttributes());
        query1.setDisableTypoToleranceOnAttributes("foo", "bar");
        assertArrayEquals(new String[]{ "foo", "bar" }, query1.getDisableTypoToleranceOnAttributes());
        assertEquals("[\"foo\",\"bar\"]", query1.get("disableTypoToleranceOnAttributes"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getDisableTypoToleranceOnAttributes(), query2.getDisableTypoToleranceOnAttributes());
    }

    @Test
    public void test_aroundPrecision() {
        Query query1 = new Query();
        assertNull(query1.getAroundPrecision());
        query1.setAroundPrecision(12345);
        assertEquals(new Integer(12345), query1.getAroundPrecision());
        assertEquals("12345", query1.get("aroundPrecision"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAroundPrecision(), query2.getAroundPrecision());
    }

    @Test
    public void test_aroundRadius() {
        Query query1 = new Query();
        assertNull(query1.getAroundRadius());
        query1.setAroundRadius(987);
        assertEquals(new Integer(987), query1.getAroundRadius());
        assertEquals("987", query1.get("aroundRadius"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAroundRadius(), query2.getAroundRadius());
    }

    @Test
    public void test_aroundLatLngViaIP() {
        Query query1 = new Query();
        assertNull(query1.getAroundLatLngViaIP());
        query1.setAroundLatLngViaIP(true);
        assertEquals(Boolean.TRUE, query1.getAroundLatLngViaIP());
        assertEquals("true", query1.get("aroundLatLngViaIP"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAroundLatLngViaIP(), query2.getAroundLatLngViaIP());
    }

    @Test
    public void test_aroundLatLng() {
        Query query1 = new Query();
        assertNull(query1.getAroundLatLng());
        query1.setAroundLatLng(new Query.LatLng(89.76, -123.45));
        assertEquals(new Query.LatLng(89.76, -123.45), query1.getAroundLatLng());
        assertEquals("89.76,-123.45", query1.get("aroundLatLng"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAroundLatLng(), query2.getAroundLatLng());
    }

    @Test
    public void test_insideBoundingBox() {
        Query query1 = new Query();
        assertNull(query1.getInsideBoundingBox());
        final Query.GeoRect box1 = new Query.GeoRect(new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444));
        query1.setInsideBoundingBox(box1);
        assertArrayEquals(new Query.GeoRect[]{ box1 }, query1.getInsideBoundingBox());
        assertEquals("11.111111,22.222222,33.333333,44.444444", query1.get("insideBoundingBox"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getInsideBoundingBox(), query2.getInsideBoundingBox());

        final Query.GeoRect box2 = new Query.GeoRect(new Query.LatLng(-55.555555, -66.666666), new Query.LatLng(-77.777777, -88.888888));
        final Query.GeoRect[] boxes = { box1, box2 };
        query1.setInsideBoundingBox(boxes);
        assertArrayEquals(boxes, query1.getInsideBoundingBox());
        assertEquals("11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666,-77.777777,-88.888888", query1.get("insideBoundingBox"));
        query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getInsideBoundingBox(), query2.getInsideBoundingBox());
    }

    @Test
    public void test_insidePolygon() {
        Query query1 = new Query();
        assertNull(query1.getInsidePolygon());
        final Query.LatLng[] box = { new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444), new Query.LatLng(-55.555555, -66.666666) };
        query1.setInsidePolygon(box);
        assertArrayEquals(box, query1.getInsidePolygon());
        assertEquals("11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666", query1.get("insidePolygon"));
        Query query2 = Query.parse(query1.build());
        assertArrayEquals(query1.getInsidePolygon(), query2.getInsidePolygon());
    }

    @Test
    public void test_tagFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"tag1\", [\"tag2\", \"tag3\"]]");
        Query query1 = new Query();
        assertNull(query1.getTagFilters());
        query1.setTagFilters(VALUE);
        assertEquals(VALUE, query1.getTagFilters());
        assertEquals("[\"tag1\",[\"tag2\",\"tag3\"]]", query1.get("tagFilters"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getTagFilters(), query2.getTagFilters());
    }

    @Test
    public void test_facetFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[[\"category:Book\", \"category:Movie\"], \"author:John Doe\"]");
        Query query1 = new Query();
        assertNull(query1.getFacetFilters());
        query1.setFacetFilters(VALUE);
        assertEquals(VALUE, query1.getFacetFilters());
        assertEquals("[[\"category:Book\",\"category:Movie\"],\"author:John Doe\"]", query1.get("facetFilters"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getFacetFilters(), query2.getFacetFilters());
    }

    @Test
    public void test_advancedSyntax() {
        Query query1 = new Query();
        assertNull(query1.getAdvancedSyntax());
        query1.setAdvancedSyntax(true);
        assertEquals(Boolean.TRUE, query1.getAdvancedSyntax());
        assertEquals("true", query1.get("advancedSyntax"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getAdvancedSyntax(), query2.getAdvancedSyntax());
    }

    @Test
    public void test_removeStopWordsBoolean() throws Exception {
        Query query1 = new Query();
        assertNull(query1.getRemoveStopWords());
        query1.setRemoveStopWords(true);
        assertEquals(Boolean.TRUE, query1.getRemoveStopWords());
        assertEquals("true", query1.get("removeStopWords"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getRemoveStopWords(), query2.getRemoveStopWords());
    }

    @Test
    public void test_removeStopWordsString() throws Exception {
        Query query1 = new Query();
        assertNull(query1.getRemoveStopWords());

        query1.setRemoveStopWords("fr,en");
        final Object[] removeStopWords = (Object[]) query1.getRemoveStopWords();
        assertArrayEquals(new String[]{"fr", "en"}, removeStopWords);
        assertEquals("fr,en", query1.get("removeStopWords"));

        Query query2 = Query.parse(query1.build());
        assertArrayEquals((Object[]) query1.getRemoveStopWords(), (Object[]) query2.getRemoveStopWords());
    }

    @Test
    public void test_removeStopWordsInvalidClass() throws Exception {
        Query query1 = new Query();
        try {
            query1.setRemoveStopWords(42);
        } catch (AlgoliaException ignored) {
            return; //pass
        }
        fail("setRemoveStopWords should throw when its parameter is neither Boolean nor String.");
    }

    @Test
    public void test_maxValuesPerFacet() {
        Query query1 = new Query();
        assertNull(query1.getMaxValuesPerFacet());
        query1.setMaxValuesPerFacet(456);
        assertEquals(new Integer(456), query1.getMaxValuesPerFacet());
        assertEquals("456", query1.get("maxValuesPerFacet"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getMaxValuesPerFacet(), query2.getMaxValuesPerFacet());
    }

    @Test
    public void test_minimumAroundRadius() {
        Query query1 = new Query();
        assertNull(query1.getMinimumAroundRadius());
        query1.setMinimumAroundRadius(1000);
        assertEquals(new Integer(1000), query1.getMinimumAroundRadius());
        assertEquals("1000", query1.get("minimumAroundRadius"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getMinimumAroundRadius(), query2.getMinimumAroundRadius());
    }

    @Test
    public void test_numericFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"code=1\", [\"price:0 to 10\", \"price:1000 to 2000\"]]");
        Query query1 = new Query();
        assertNull(query1.getNumericFilters());
        query1.setNumericFilters(VALUE);
        assertEquals(VALUE, query1.getNumericFilters());
        assertEquals("[\"code=1\",[\"price:0 to 10\",\"price:1000 to 2000\"]]", query1.get("numericFilters"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getNumericFilters(), query2.getNumericFilters());
    }

    @Test
    public void test_filters() {
        final String VALUE = "available=1 AND (category:Book OR NOT category:Ebook) AND publication_date: 1441745506 TO 1441755506 AND inStock > 0 AND author:\"John Doe\"";
        Query query1 = new Query();
        assertNull(query1.getFilters());
        query1.setFilters(VALUE);
        assertEquals(VALUE, query1.getFilters());
        assertEquals(VALUE, query1.get("filters"));
        Query query2 = Query.parse(query1.build());
        assertEquals(query1.getFilters(), query2.getFilters());
    }

    @Test
    public void test_exactOnSingleWordQuery() {
        Query.ExactOnSingleWordQuery VALUE = Query.ExactOnSingleWordQuery.ATTRIBUTE;
        Query query = new Query();
        assertNull(query.getExactOnSingleWordQuery());

        query.setExactOnSingleWordQuery(VALUE);
        assertEquals(VALUE, query.getExactOnSingleWordQuery());
        assertEquals("attribute", query.get("exactOnSingleWordQuery"));
        Query query2 = Query.parse(query.build());
        assertEquals(query.getExactOnSingleWordQuery(), query2.getExactOnSingleWordQuery());
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
        assertArrayEquals(array, query.getAlternativesAsExact());

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
        assertTrue("The built query should contain 'aroundRadius=" + VALUE + "'.", queryStr.contains("aroundRadius=" + VALUE));

        query.setAroundRadius(Query.RADIUS_ALL);
        assertEquals("After setting it to RADIUS_ALL, a query should have this aroundRadius value.", Integer.valueOf(Query.RADIUS_ALL), query.getAroundRadius());

        queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=all', not _" + queryStr + "_.", queryStr.contains("aroundRadius=all"));
        Query query2 = Query.parse(query.build());
        assertEquals("The built query should be parsed and built successfully.", query.getAroundRadius(), query2.getAroundRadius());
    }
}