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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Test serializing a query into a URL query string.
     */
    @Test
    public void build() {
        Query query = new Query();
        query.set("c", "C");
        query.set("b", "B");
        query.set("a", "A");
        String queryString = query.build();
        assertEquals("a=A&b=B&c=C", queryString);
    }

    /**
     * Test parsing a query from a URL query string.
     */
    @Test
    public void parse() {
        // Build the URL for a query.
        Query query = new Query();
        query.set("foo", "bar");
        query.set("abc", "xyz");
        String queryString = query.build();

        // Parse the URL into another query.
        assertEquals(query, Query.parse(queryString));
    }

    /**
     * Test that non-ASCII and special characters are escaped.
     */
    @Test
    public void escape() {
        Query query = new Query();
        query.set("accented", "éêèàôù");
        query.set("escaped", " %&=#+");
        String queryString = query.build();
        assertEquals("accented=%C3%A9%C3%AA%C3%A8%C3%A0%C3%B4%C3%B9&escaped=%20%25%26%3D%23%2B", queryString);

        // Test parsing of escaped characters.
        assertEquals(query, Query.parse(queryString));
    }

    // ----------------------------------------------------------------------
    // Low-level
    // ----------------------------------------------------------------------

    /**
     * Test low-level accessors.
     */
    @Test
    public void getSet() {
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
    public void minWordSizefor1Typo() {
        Query query = new Query();
        assertNull(query.getMinWordSizefor1Typo());
        query.setMinWordSizefor1Typo(123);
        assertEquals(Integer.valueOf(123), query.getMinWordSizefor1Typo());
        assertEquals("123", query.get("minWordSizefor1Typo"));
        assertEquals(query.getMinWordSizefor1Typo(), Query.parse(query.build()).getMinWordSizefor1Typo());
    }

    @Test
    public void minWordSizefor2Typos() {
        Query query = new Query();
        assertNull(query.getMinWordSizefor2Typos());
        query.setMinWordSizefor2Typos(456);
        assertEquals(Integer.valueOf(456), query.getMinWordSizefor2Typos());
        assertEquals("456", query.get("minWordSizefor2Typos"));
        assertEquals(query.getMinWordSizefor2Typos(), Query.parse(query.build()).getMinWordSizefor2Typos());
    }

    @Test
    public void minProximity() {
        Query query = new Query();
        assertNull(query.getMinProximity());
        query.setMinProximity(999);
        assertEquals(Integer.valueOf(999), query.getMinProximity());
        assertEquals("999", query.get("minProximity"));
        assertEquals(query.getMinProximity(), Query.parse(query.build()).getMinProximity());
    }

    @Test
    public void getRankingInfo() {
        Query query = new Query();
        assertNull(query.getGetRankingInfo());
        query.setGetRankingInfo(true);
        assertEquals(Boolean.TRUE, query.getGetRankingInfo());
        assertEquals("true", query.get("getRankingInfo"));
        assertEquals(query.getGetRankingInfo(), Query.parse(query.build()).getGetRankingInfo());

        query.setGetRankingInfo(false);
        assertEquals(Boolean.FALSE, query.getGetRankingInfo());
        assertEquals("false", query.get("getRankingInfo"));
        assertEquals(query.getGetRankingInfo(), Query.parse(query.build()).getGetRankingInfo());
    }

    @Test
    public void ignorePlurals() {
        // No value
        Query query = new Query();
        assertFalse("By default, ignorePlurals should be disabled.", query.getIgnorePlurals().enabled);

        // Boolean values
        query.setIgnorePlurals(true);
        assertEquals("A true boolean should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("A true boolean should be in ignorePlurals.", "true", query.get("ignorePlurals"));
        assertEquals("A true boolean should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals(false);
        assertEquals("A false boolean should disable ignorePlurals.", Boolean.FALSE, query.getIgnorePlurals().enabled);
        assertEquals("A false boolean should should be in ignorePlurals.", "false", query.get("ignorePlurals"));
        assertEquals("A false boolean should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        // List values
        query.setIgnorePlurals((List<String>) null);
        assertFalse("A null list value should disable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertEquals("A null list value should disable ignorePlurals.", "false", query.get("ignorePlurals"));
        assertEquals("A null list value should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals(new ArrayList<String>());
        assertFalse("Setting an empty list should disable ignorePlurals.", query.getIgnorePlurals().enabled);

        ArrayList<String> languageCodes = new ArrayList<>(java.util.Arrays.asList("en", "fr"));
        query.setIgnorePlurals(languageCodes);
        assertTrue("Setting a non-empty list should enable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertEquals("Setting a non-empty list should be in ignorePlurals.", "en,fr", query.get("ignorePlurals"));
        assertNotNull("The language codes should not be null", query.getIgnorePlurals().languageCodes);
        assertEquals("Two language codes should be in ignorePlurals.", 2, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The first language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains(languageCodes.get(0)));
        assertTrue("The second language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains(languageCodes.get(1)));

        // String[] values
        query.setIgnorePlurals("");
        assertFalse("An empty string should disable ignorePlurals.", query.getIgnorePlurals().enabled);
        assertEquals("An empty string should be in ignorePlurals.", "", query.get("ignorePlurals"));
        assertEquals("A empty string should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());

        query.setIgnorePlurals("en");
        assertEquals("A single language code should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("A single language code should be in ignorePlurals.", "en", query.get("ignorePlurals"));
        assertEquals("A single language code should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());
        assertEquals("One language code should be in ignorePlurals.", 1, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("en"));

        query.setIgnorePlurals("en", "fr");
        assertEquals("Two language codes should enable ignorePlurals.", Boolean.TRUE, query.getIgnorePlurals().enabled);
        assertEquals("Two language codes should be in ignorePlurals.", "en,fr", query.get("ignorePlurals"));
        assertEquals("Two language codes should be built and parsed successfully.", query.getIgnorePlurals(), Query.parse(query.build()).getIgnorePlurals());
        assertEquals("Two language codes should be in ignorePlurals.", 2, query.getIgnorePlurals().languageCodes.size());
        assertTrue("The first language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("en"));
        assertTrue("The second language code should be in ignorePlurals", query.getIgnorePlurals().languageCodes.contains("fr"));
    }

    @Test
    public void distinct() {
        Query query = new Query();
        assertNull(query.getDistinct());
        query.setDistinct(100);
        assertEquals(Integer.valueOf(100), query.getDistinct());
        assertEquals("100", query.get("distinct"));
        assertEquals(query.getDistinct(), Query.parse(query.build()).getDistinct());
    }

    @Test
    public void page() {
        Query query = new Query();
        assertNull(query.getPage());
        query.setPage(0);
        assertEquals(Integer.valueOf(0), query.getPage());
        assertEquals("0", query.get("page"));
        assertEquals(query.getPage(), Query.parse(query.build()).getPage());
    }

    @Test
    public void percentileCalculation() {
        Query query = new Query();
        assertNull(query.getPercentileComputation());
        query.setPercentileComputation(true);
        assertEquals(Boolean.TRUE, query.getPercentileComputation());
        assertEquals("true", query.get("percentileComputation"));
        assertEquals(query.getPercentileComputation(), Query.parse(query.build()).getPercentileComputation());
    }

    @Test
    public void hitsPerPage() {
        Query query = new Query();
        assertNull(query.getHitsPerPage());
        query.setHitsPerPage(50);
        assertEquals(Integer.valueOf(50), query.getHitsPerPage());
        assertEquals("50", query.get("hitsPerPage"));
        assertEquals(query.getHitsPerPage(), Query.parse(query.build()).getHitsPerPage());
    }

    @Test
    public void allowTyposOnNumericTokens() {
        Query query = new Query();
        assertNull(query.getAllowTyposOnNumericTokens());
        query.setAllowTyposOnNumericTokens(true);
        assertEquals(Boolean.TRUE, query.getAllowTyposOnNumericTokens());
        assertEquals("true", query.get("allowTyposOnNumericTokens"));
        assertEquals(query.getAllowTyposOnNumericTokens(), Query.parse(query.build()).getAllowTyposOnNumericTokens());

        query.setAllowTyposOnNumericTokens(false);
        assertEquals(Boolean.FALSE, query.getAllowTyposOnNumericTokens());
        assertEquals("false", query.get("allowTyposOnNumericTokens"));
        assertEquals(query.getAllowTyposOnNumericTokens(), Query.parse(query.build()).getAllowTyposOnNumericTokens());
    }

    @Test
    public void analytics() {
        Query query = new Query();
        assertNull(query.getAnalytics());
        query.setAnalytics(true);
        assertEquals(Boolean.TRUE, query.getAnalytics());
        assertEquals("true", query.get("analytics"));
        assertEquals(query.getAnalytics(), Query.parse(query.build()).getAnalytics());
    }

    @Test
    public void clickAnalytics() {
        Query query = new Query();
        assertNull(query.getClickAnalytics());
        query.setClickAnalytics(true);
        assertEquals(Boolean.TRUE, query.getClickAnalytics());
        assertEquals("true", query.get("clickAnalytics"));
        assertEquals(query.getClickAnalytics(), Query.parse(query.build()).getClickAnalytics());
    }


    @Test
    public void sortFacetValuesBy() {
        Query query = new Query();
        assertNull(query.getSortFacetValuesBy());
        query.setSortFacetValuesBy(Query.SortFacetValuesBy.COUNT);
        assertEquals(Query.SortFacetValuesBy.COUNT, query.getSortFacetValuesBy());
        assertEquals("count", query.get("sortFacetValuesBy"));
        query.setSortFacetValuesBy(Query.SortFacetValuesBy.ALPHA);
        assertEquals(Query.SortFacetValuesBy.ALPHA, query.getSortFacetValuesBy());
        assertEquals("alpha", query.get("sortFacetValuesBy"));
        Query query2 = Query.parse(query.build());
        assertEquals(query.getSortFacetValuesBy(), query2.getSortFacetValuesBy());
    }

    @Test
    public void synonyms() {
        Query query = new Query();
        assertNull(query.getSynonyms());
        query.setSynonyms(true);
        assertEquals(Boolean.TRUE, query.getSynonyms());
        assertEquals("true", query.get("synonyms"));
        assertEquals(query.getSynonyms(), Query.parse(query.build()).getSynonyms());
    }

    @Test
    public void sumOrFiltersScores() {
        Query query = new Query();
        assertNull(query.getSumOrFiltersScores());
        query.setSumOrFiltersScores(true);
        assertEquals(Boolean.TRUE, query.getSumOrFiltersScores());
        assertEquals("true", query.get("sumOrFiltersScores"));
        assertEquals(query.getSumOrFiltersScores(), Query.parse(query.build()).getSumOrFiltersScores());
    }

    @Test
    public void attributesToHighlight() {
        Query query = new Query();
        assertNull(query.getAttributesToHighlight());
        query.setAttributesToHighlight("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getAttributesToHighlight());
        assertEquals("[\"foo\",\"bar\"]", query.get("attributesToHighlight"));
        assertArrayEquals(query.getAttributesToHighlight(), Query.parse(query.build()).getAttributesToHighlight());

        query.setAttributesToHighlight(Arrays.asList("foo", "bar"));
        assertArrayEquals(new String[]{"foo", "bar"}, query.getAttributesToHighlight());
        assertEquals("[\"foo\",\"bar\"]", query.get("attributesToHighlight"));
        assertArrayEquals(query.getAttributesToHighlight(), Query.parse(query.build()).getAttributesToHighlight());
    }

    @Test
    public void attributesToRetrieve() {
        Query query = new Query();
        assertNull(query.getAttributesToRetrieve());
        query.setAttributesToRetrieve("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getAttributesToRetrieve());
        assertEquals("[\"foo\",\"bar\"]", query.get("attributesToRetrieve"));
        assertArrayEquals(query.getAttributesToRetrieve(), Query.parse(query.build()).getAttributesToRetrieve());

        query.setAttributesToRetrieve(Arrays.asList("foo", "bar"));
        assertArrayEquals(new String[]{"foo", "bar"}, query.getAttributesToRetrieve());
        assertEquals("[\"foo\",\"bar\"]", query.get("attributesToRetrieve"));
        assertArrayEquals(query.getAttributesToRetrieve(), Query.parse(query.build()).getAttributesToRetrieve());
    }

    @Test
    public void attributesToSnippet() {
        Query query = new Query();
        assertNull(query.getAttributesToSnippet());
        query.setAttributesToSnippet("foo:3", "bar:7");
        assertArrayEquals(new String[]{"foo:3", "bar:7"}, query.getAttributesToSnippet());
        assertEquals("[\"foo:3\",\"bar:7\"]", query.get("attributesToSnippet"));
        assertArrayEquals(query.getAttributesToSnippet(), Query.parse(query.build()).getAttributesToSnippet());
    }

    @Test
    public void query() {
        Query query = new Query();
        assertNull(query.getQuery());
        query.setQuery("supercalifragilisticexpialidocious");
        assertEquals("supercalifragilisticexpialidocious", query.getQuery());
        assertEquals("supercalifragilisticexpialidocious", query.get("query"));
        assertEquals(query.getQuery(), Query.parse(query.build()).getQuery());
    }

    @Test
    public void queryType() {
        Query query = new Query();
        assertNull(query.getQueryType());

        query.setQueryType(Query.QueryType.PREFIX_ALL);
        assertEquals(Query.QueryType.PREFIX_ALL, query.getQueryType());
        assertEquals("prefixAll", query.get("queryType"));
        assertEquals(query.getQueryType(), Query.parse(query.build()).getQueryType());

        query.setQueryType(Query.QueryType.PREFIX_LAST);
        assertEquals(Query.QueryType.PREFIX_LAST, query.getQueryType());
        assertEquals("prefixLast", query.get("queryType"));
        assertEquals(query.getQueryType(), Query.parse(query.build()).getQueryType());

        query.setQueryType(Query.QueryType.PREFIX_NONE);
        assertEquals(query.getQueryType(), Query.QueryType.PREFIX_NONE);
        assertEquals(query.get("queryType"), "prefixNone");
        assertEquals(query.getQueryType(), Query.parse(query.build()).getQueryType());

        query.set("queryType", "invalid");
        assertNull(query.getQueryType());
    }

    @Test
    public void removeWordsIfNoResults() {
        Query query = new Query();
        assertNull(query.getRemoveWordsIfNoResults());

        query.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.ALL_OPTIONAL);
        assertEquals(Query.RemoveWordsIfNoResults.ALL_OPTIONAL, query.getRemoveWordsIfNoResults());
        assertEquals("allOptional", query.get("removeWordsIfNoResults"));
        assertEquals(query.getRemoveWordsIfNoResults(), Query.parse(query.build()).getRemoveWordsIfNoResults());

        query.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.FIRST_WORDS);
        assertEquals(Query.RemoveWordsIfNoResults.FIRST_WORDS, query.getRemoveWordsIfNoResults());
        assertEquals("firstWords", query.get("removeWordsIfNoResults"));
        assertEquals(query.getRemoveWordsIfNoResults(), Query.parse(query.build()).getRemoveWordsIfNoResults());

        query.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.LAST_WORDS);
        assertEquals(Query.RemoveWordsIfNoResults.LAST_WORDS, query.getRemoveWordsIfNoResults());
        assertEquals("lastWords", query.get("removeWordsIfNoResults"));
        assertEquals(query.getRemoveWordsIfNoResults(), Query.parse(query.build()).getRemoveWordsIfNoResults());

        query.setRemoveWordsIfNoResults(Query.RemoveWordsIfNoResults.NONE);
        assertEquals(Query.RemoveWordsIfNoResults.NONE, query.getRemoveWordsIfNoResults());
        assertEquals("none", query.get("removeWordsIfNoResults"));
        assertEquals(query.getRemoveWordsIfNoResults(), Query.parse(query.build()).getRemoveWordsIfNoResults());

        query.set("removeWordsIfNoResults", "invalid");
        assertNull(query.getRemoveWordsIfNoResults());

        query.set("removeWordsIfNoResults", "allOptional");
        assertEquals(Query.RemoveWordsIfNoResults.ALL_OPTIONAL, query.getRemoveWordsIfNoResults());
    }

    @Test
    public void typoTolerance() {
        Query query = new Query();
        assertNull(query.getTypoTolerance());

        query.setTypoTolerance(Query.TypoTolerance.TRUE);
        assertEquals(Query.TypoTolerance.TRUE, query.getTypoTolerance());
        assertEquals("true", query.get("typoTolerance"));
        assertEquals(query.getTypoTolerance(), Query.parse(query.build()).getTypoTolerance());

        query.setTypoTolerance(Query.TypoTolerance.FALSE);
        assertEquals(Query.TypoTolerance.FALSE, query.getTypoTolerance());
        assertEquals("false", query.get("typoTolerance"));
        assertEquals(query.getTypoTolerance(), Query.parse(query.build()).getTypoTolerance());

        query.setTypoTolerance(Query.TypoTolerance.MIN);
        assertEquals(Query.TypoTolerance.MIN, query.getTypoTolerance());
        assertEquals("min", query.get("typoTolerance"));
        assertEquals(query.getTypoTolerance(), Query.parse(query.build()).getTypoTolerance());

        query.setTypoTolerance(Query.TypoTolerance.STRICT);
        assertEquals(Query.TypoTolerance.STRICT, query.getTypoTolerance());
        assertEquals("strict", query.get("typoTolerance"));
        assertEquals(query.getTypoTolerance(), Query.parse(query.build()).getTypoTolerance());

        query.set("typoTolerance", "invalid");
        assertNull(query.getTypoTolerance());

        query.set("typoTolerance", "true");
        assertEquals(Query.TypoTolerance.TRUE, query.getTypoTolerance());
    }

    @Test
    public void facets() {
        Query query = new Query();
        assertNull(query.getFacets());
        query.setFacets("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getFacets());
        assertEquals("[\"foo\",\"bar\"]", query.get("facets"));
        Query query2 = Query.parse(query.build());
        assertArrayEquals(query.getFacets(), query2.getFacets());
    }


    @Test
    public void offset() {
        Query query = new Query();
        assertNull(query.getOffset());
        query.setOffset(0);
        assertEquals(Integer.valueOf(0), query.getOffset());
        assertEquals("0", query.get("offset"));
        assertEquals(query.getOffset(), Query.parse(query.build()).getOffset());
    }

    @Test
    public void optionalWords() {
        Query query = new Query();
        assertNull(query.getOptionalWords());
        query.setOptionalWords("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getOptionalWords());
        assertEquals("[\"foo\",\"bar\"]", query.get("optionalWords"));
        assertArrayEquals(query.getOptionalWords(), Query.parse(query.build()).getOptionalWords());
    }

    @Test
    public void optionalFilters() {
        Query query = new Query();
        assertNull(query.getOptionalFilters());
        query.setOptionalFilters("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getOptionalFilters());
        assertEquals("[\"foo\",\"bar\"]", query.get("optionalFilters"));
        assertArrayEquals(query.getOptionalFilters(), Query.parse(query.build()).getOptionalFilters());
    }

    @Test
    public void replaceSynonymsInHighlight() {
        Query query = new Query();
        assertNull(query.getReplaceSynonymsInHighlight());
        query.setReplaceSynonymsInHighlight(true);
        assertEquals(true, query.getReplaceSynonymsInHighlight());
        assertEquals("true", query.get("replaceSynonymsInHighlight"));
        assertEquals(query.getReplaceSynonymsInHighlight(), Query.parse(query.build()).getReplaceSynonymsInHighlight());
    }

    @Test
    public void restrictHighlightAndSnippetArrays() {
        Query query = new Query();
        assertNull(query.getRestrictHighlightAndSnippetArrays());
        query.setRestrictHighlightAndSnippetArrays(true);
        assertEquals(true, query.getRestrictHighlightAndSnippetArrays());
        assertEquals("true", query.get("restrictHighlightAndSnippetArrays"));
        assertEquals(query.getRestrictHighlightAndSnippetArrays(), Query.parse(query.build()).getRestrictHighlightAndSnippetArrays());
    }

    @Test
    public void restrictSearchableAttributes() {
        Query query = new Query();
        assertNull(query.getRestrictSearchableAttributes());
        query.setRestrictSearchableAttributes("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getRestrictSearchableAttributes());
        assertEquals("[\"foo\",\"bar\"]", query.get("restrictSearchableAttributes"));
        assertArrayEquals(query.getRestrictSearchableAttributes(), Query.parse(query.build()).getRestrictSearchableAttributes());
    }

    @Test
    public void highlightPreTag() {
        Query query = new Query();
        assertNull(query.getHighlightPreTag());
        query.setHighlightPreTag("<PRE[");
        assertEquals("<PRE[", query.getHighlightPreTag());
        assertEquals("<PRE[", query.get("highlightPreTag"));
        assertEquals(query.getHighlightPreTag(), Query.parse(query.build()).getHighlightPreTag());
    }

    @Test
    public void highlightPostTag() {
        Query query = new Query();
        assertNull(query.getHighlightPostTag());
        query.setHighlightPostTag("]POST>");
        assertEquals("]POST>", query.getHighlightPostTag());
        assertEquals("]POST>", query.get("highlightPostTag"));
        assertEquals(query.getHighlightPostTag(), Query.parse(query.build()).getHighlightPostTag());
    }

    @Test
    public void snippetEllipsisText() {
        Query query = new Query();
        assertNull(query.getSnippetEllipsisText());
        query.setSnippetEllipsisText("…");
        assertEquals("…", query.getSnippetEllipsisText());
        assertEquals("…", query.get("snippetEllipsisText"));
        Query query2 = Query.parse(query.build());
        assertEquals(query.getSnippetEllipsisText(), query2.getSnippetEllipsisText());
    }

    @Test
    public void analyticsTags() {
        Query query = new Query();
        assertNull(query.getAnalyticsTags());
        query.setAnalyticsTags("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getAnalyticsTags());
        assertEquals("[\"foo\",\"bar\"]", query.get("analyticsTags"));
        Query query2 = Query.parse(query.build());
        assertArrayEquals(query.getAnalyticsTags(), query2.getAnalyticsTags());
    }

    @Test
    public void disableExactOnAttributes() {
        Query query = new Query();
        assertNull(query.getDisableExactOnAttributes());
        query.setDisableExactOnAttributes("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getDisableExactOnAttributes());
        assertEquals("[\"foo\",\"bar\"]", query.get("disableExactOnAttributes"));
        Query query2 = Query.parse(query.build());
        assertArrayEquals(query.getDisableExactOnAttributes(), query2.getDisableExactOnAttributes());
    }

    @Test
    public void disableTypoToleranceOnAttributes() {
        Query query = new Query();
        assertNull(query.getDisableTypoToleranceOnAttributes());
        query.setDisableTypoToleranceOnAttributes("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getDisableTypoToleranceOnAttributes());
        assertEquals("[\"foo\",\"bar\"]", query.get("disableTypoToleranceOnAttributes"));
        Query query2 = Query.parse(query.build());
        assertArrayEquals(query.getDisableTypoToleranceOnAttributes(), query2.getDisableTypoToleranceOnAttributes());
    }

    @Test
    public void aroundPrecision() {
        Query query = new Query();
        assertNull(query.getAroundPrecision());
        query.setAroundPrecision(12345);
        assertEquals(Integer.valueOf(12345), query.getAroundPrecision());
        assertEquals("12345", query.get("aroundPrecision"));
        Query query2 = Query.parse(query.build());
        assertEquals(query.getAroundPrecision(), query2.getAroundPrecision());
    }

    @Test
    public void aroundRadius() {
        Query query = new Query();
        assertNull(query.getAroundRadius());
        query.setAroundRadius(987);
        assertEquals(Integer.valueOf(987), query.getAroundRadius());
        assertEquals("987", query.get("aroundRadius"));
        Query query2 = Query.parse(query.build());
        assertEquals(query.getAroundRadius(), query2.getAroundRadius());
    }

    @Test
    public void aroundLatLngViaIP() {
        Query query = new Query();
        assertNull(query.getAroundLatLngViaIP());
        query.setAroundLatLngViaIP(true);
        assertEquals(Boolean.TRUE, query.getAroundLatLngViaIP());
        assertEquals("true", query.get("aroundLatLngViaIP"));
        assertEquals(query.getAroundLatLngViaIP(), Query.parse(query.build()).getAroundLatLngViaIP());
    }

    @Test
    public void aroundLatLng() {
        Query query = new Query();
        assertNull(query.getAroundLatLng());
        query.setAroundLatLng(new Query.LatLng(89.76, -123.45));
        assertEquals(new Query.LatLng(89.76, -123.45), query.getAroundLatLng());
        assertEquals("89.76,-123.45", query.get("aroundLatLng"));
        assertEquals(query.getAroundLatLng(), Query.parse(query.build()).getAroundLatLng());
    }

    @Test
    public void insideBoundingBox() {
        Query query = new Query();
        assertNull(query.getInsideBoundingBox());
        final Query.GeoRect box1 = new Query.GeoRect(new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444));
        query.setInsideBoundingBox(box1);
        assertArrayEquals(new Query.GeoRect[]{box1}, query.getInsideBoundingBox());
        assertEquals("11.111111,22.222222,33.333333,44.444444", query.get("insideBoundingBox"));
        assertArrayEquals(query.getInsideBoundingBox(), Query.parse(query.build()).getInsideBoundingBox());

        final Query.GeoRect box2 = new Query.GeoRect(new Query.LatLng(-55.555555, -66.666666), new Query.LatLng(-77.777777, -88.888888));
        final Query.GeoRect[] boxes = {box1, box2};
        query.setInsideBoundingBox(boxes);
        assertArrayEquals(boxes, query.getInsideBoundingBox());
        assertEquals("11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666,-77.777777,-88.888888", query.get("insideBoundingBox"));
        assertArrayEquals(query.getInsideBoundingBox(), Query.parse(query.build()).getInsideBoundingBox());
    }

    @Test
    public void insidePolygon() {
        Query query = new Query();
        assertNull(query.getInsidePolygon());
        final Query.Polygon polygon = new Query.Polygon(new Query.LatLng(11.111111, 22.222222), new Query.LatLng(33.333333, 44.444444), new Query.LatLng(-55.555555, -66.666666));
        Query.Polygon[] polygons = {polygon};
        query.setInsidePolygon(polygons);
        assertArrayEquals(polygons, query.getInsidePolygon());
        assertEquals("11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666", query.get("insidePolygon"));
        assertArrayEquals(query.getInsidePolygon(), Query.parse(query.build()).getInsidePolygon());

        final Query.Polygon polygon2 = new Query.Polygon(new Query.LatLng(77.777777, 88.888888), new Query.LatLng(99.999999, 11.111111), new Query.LatLng(-11.111111, -22.222222));
        polygons = new Query.Polygon[]{polygon, polygon2};
        query.setInsidePolygon(polygons);
        assertArrayEquals(polygons, query.getInsidePolygon());
        assertEquals("[[11.111111,22.222222,33.333333,44.444444,-55.555555,-66.666666],[77.777777,88.888888,99.999999,11.111111,-11.111111,-22.222222]]", query.get("insidePolygon"));
        assertArrayEquals(query.getInsidePolygon(), Query.parse(query.build()).getInsidePolygon());
    }

    @Test
    public void tagFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"tag1\", [\"tag2\", \"tag3\"]]");
        Query query = new Query();
        assertNull(query.getTagFilters());
        query.setTagFilters(VALUE);
        assertEquals(VALUE, query.getTagFilters());
        assertEquals("[\"tag1\",[\"tag2\",\"tag3\"]]", query.get("tagFilters"));
        assertEquals(query.getTagFilters(), Query.parse(query.build()).getTagFilters());
    }

    @Test
    public void facetFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[[\"category:Book\", \"category:Movie\"], \"author:John Doe\"]");
        Query query = new Query();
        assertNull(query.getFacetFilters());
        query.setFacetFilters(VALUE);
        assertEquals(VALUE, query.getFacetFilters());
        assertEquals("[[\"category:Book\",\"category:Movie\"],\"author:John Doe\"]", query.get("facetFilters"));
        assertEquals(query.getFacetFilters(), Query.parse(query.build()).getFacetFilters());
    }

    @Test
    public void advancedSyntax() {
        Query query = new Query();
        assertNull(query.getAdvancedSyntax());
        query.setAdvancedSyntax(true);
        assertEquals(Boolean.TRUE, query.getAdvancedSyntax());
        assertEquals("true", query.get("advancedSyntax"));
        assertEquals(query.getAdvancedSyntax(), Query.parse(query.build()).getAdvancedSyntax());
    }

    @Test
    public void removeStopWordsBoolean() throws Exception {
        Query query = new Query();
        assertNull(query.getRemoveStopWords());
        query.setRemoveStopWords(true);
        assertEquals(Boolean.TRUE, query.getRemoveStopWords());
        assertEquals("true", query.get("removeStopWords"));
        assertEquals(query.getRemoveStopWords(), Query.parse(query.build()).getRemoveStopWords());
    }

    @Test
    public void removeStopWordsString() throws Exception {
        Query query = new Query();
        assertNull(query.getRemoveStopWords());

        query.setRemoveStopWords("fr,en");
        final Object[] removeStopWords = (Object[]) query.getRemoveStopWords();
        assertArrayEquals(new String[]{"fr", "en"}, removeStopWords);
        assertEquals("fr,en", query.get("removeStopWords"));

        assertArrayEquals((Object[]) query.getRemoveStopWords(), (Object[]) Query.parse(query.build()).getRemoveStopWords());
    }

    @Test
    public void removeStopWordsInvalidClass() throws Exception {
        Query query = new Query();
        try {
            query.setRemoveStopWords(42);
        } catch (AlgoliaException ignored) {
            return; //pass
        }
        fail("setRemoveStopWords should throw when its parameter is neither Boolean nor String.");
    }

    @Test
    public void length() {
        Query query = new Query();
        assertNull(query.getLength());
        query.setLength(456);
        assertEquals(Integer.valueOf(456), query.getLength());
        assertEquals("456", query.get("length"));
        assertEquals(query.getLength(), Query.parse(query.build()).getLength());
    }

    @Test
    public void maxFacetHits() {
        Query query = new Query();
        assertNull(query.getMaxFacetHits());
        query.setMaxFacetHits(456);
        assertEquals(Integer.valueOf(456), query.getMaxFacetHits());
        assertEquals("456", query.get("maxFacetHits"));
        assertEquals(query.getMaxFacetHits(), Query.parse(query.build()).getMaxFacetHits());
    }

    @Test
    public void maxValuesPerFacet() {
        Query query = new Query();
        assertNull(query.getMaxValuesPerFacet());
        query.setMaxValuesPerFacet(456);
        assertEquals(Integer.valueOf(456), query.getMaxValuesPerFacet());
        assertEquals("456", query.get("maxValuesPerFacet"));
        assertEquals(query.getMaxValuesPerFacet(), Query.parse(query.build()).getMaxValuesPerFacet());
    }

    @Test
    public void minimumAroundRadius() {
        Query query = new Query();
        assertNull(query.getMinimumAroundRadius());
        query.setMinimumAroundRadius(1000);
        assertEquals(Integer.valueOf(1000), query.getMinimumAroundRadius());
        assertEquals("1000", query.get("minimumAroundRadius"));
        assertEquals(query.getMinimumAroundRadius(), Query.parse(query.build()).getMinimumAroundRadius());
    }

    @Test
    public void numericFilters() throws JSONException {
        final JSONArray VALUE = new JSONArray("[\"code=1\", [\"price:0 to 10\", \"price:1000 to 2000\"]]");
        Query query = new Query();
        assertNull(query.getNumericFilters());
        query.setNumericFilters(VALUE);
        assertEquals(VALUE, query.getNumericFilters());
        assertEquals("[\"code=1\",[\"price:0 to 10\",\"price:1000 to 2000\"]]", query.get("numericFilters"));
        assertEquals(query.getNumericFilters(), Query.parse(query.build()).getNumericFilters());
    }

    @Test
    public void filters() {
        final String VALUE = "available=1 AND (category:Book OR NOT category:Ebook) AND publication_date: 1441745506 TO 1441755506 AND inStock > 0 AND author:\"John Doe\"";
        Query query = new Query();
        assertNull(query.getFilters());
        query.setFilters(VALUE);
        assertEquals(VALUE, query.getFilters());
        assertEquals(VALUE, query.get("filters"));
        assertEquals(query.getFilters(), Query.parse(query.build()).getFilters());
    }

    @Test
    public void exactOnSingleWordQuery() {
        Query.ExactOnSingleWordQuery VALUE = Query.ExactOnSingleWordQuery.ATTRIBUTE;
        Query query = new Query();
        assertNull(query.getExactOnSingleWordQuery());

        query.setExactOnSingleWordQuery(VALUE);
        assertEquals(VALUE, query.getExactOnSingleWordQuery());
        assertEquals("attribute", query.get("exactOnSingleWordQuery"));
        assertEquals(query.getExactOnSingleWordQuery(), Query.parse(query.build()).getExactOnSingleWordQuery());
    }

    @Test
    public void alternativesAsExact() {
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

        assertEquals(query.getExactOnSingleWordQuery(), Query.parse(query.build()).getExactOnSingleWordQuery());
    }

    @Test
    public void aroundRadius_all() {
        final Integer VALUE = 3;
        Query query = new Query();
        assertNull("A new query should have a null aroundRadius.", query.getAroundRadius());

        query.setAroundRadius(VALUE);
        assertEquals("After setting a query's aroundRadius to a given integer, we should return it from getAroundRadius.", VALUE, query.getAroundRadius());
        assertEquals("After setting a query's aroundRadius to a given integer, it should be in aroundRadius.", String.valueOf(VALUE), query.get("aroundRadius"));

        String queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=" + VALUE + "'.", queryStr.contains("aroundRadius=" + VALUE));

        query.setAroundRadius(Query.RADIUS_ALL);
        assertEquals("After setting a query's aroundRadius to RADIUS_ALL, it should have this aroundRadius value.", Integer.valueOf(Query.RADIUS_ALL), query.getAroundRadius());
        assertEquals("After setting a query's aroundRadius to RADIUS_ALL, its aroundRadius should be equal to \"all\".", "all", query.get("aroundRadius"));

        queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=all', not _" + queryStr + "_.", queryStr.contains("aroundRadius=all"));
        assertEquals("The built query should be parsed and built successfully.", query.getAroundRadius(), Query.parse(query.build()).getAroundRadius());
    }

    @Test
    public void responseFields() throws UnsupportedEncodingException {
        Query query = new Query();
        assertNull("A new query should have a null responseFields.", query.getResponseFields());
        String queryStr = query.build();
        assertFalse("The built query should not contain responseFields: \"" + queryStr + "\".", queryStr.contains("responseFields"));

        query.setResponseFields("*");
        assertEquals("After setting its responseFields to \"*\", getResponseFields should contain one element.", 1, query.getResponseFields().length);
        assertEquals("After setting its responseFields to \"*\", getResponseFields should contain \"*\".", "*", query.getResponseFields()[0]);
        queryStr = query.build();
        String expected = "responseFields=" + URLEncoder.encode("[\"*\"]", "UTF-8");
        assertTrue("The built query should contain \"" + expected + "\", but contains _" + queryStr + "_.", queryStr.contains(expected));

        query.setResponseFields("hits", "page");
        assertEquals("After setting its responseFields to [\"hits\",\"page\"], getResponseFields should contain two elements.", 2, query.getResponseFields().length);
        assertEquals("After setting its responseFields to [\"hits\",\"page\"], getResponseFields should contain \"hits\".", "hits", query.getResponseFields()[0]);
        assertEquals("After setting its responseFields to [\"hits\",\"page\"], getResponseFields should contain \"page\".", "page", query.getResponseFields()[1]);
        queryStr = query.build();
        expected = "responseFields=" + URLEncoder.encode("[\"hits\",\"page\"]", "UTF-8");
        assertTrue("The built query should contain \"" + expected + "\", but contains _" + queryStr + "_.", queryStr.contains(expected));
    }

    @Test
    public void ruleContexts() {
        Query query = new Query();
        assertNull(query.getRuleContexts());
        query.setRuleContexts("foo", "bar");
        assertArrayEquals(new String[]{"foo", "bar"}, query.getRuleContexts());
        assertEquals("[\"foo\",\"bar\"]", query.get("ruleContexts"));
        assertArrayEquals(query.getRuleContexts(), Query.parse(query.build()).getRuleContexts());
    }

    @Test
    public void enableRules() {
        Query query = new Query();
        assertNull(query.getEnableRules());
        query.setEnableRules(true);
        assertEquals(Boolean.TRUE, query.getEnableRules());
        assertEquals("true", query.get("enableRules"));
        assertEquals(query.getEnableRules(), Query.parse(query.build()).getEnableRules());
    }


    @Test
    public void facetingAfterDistinct() {
        Query query = new Query();
        assertNull(query.getFacetingAfterDistinct());
        query.setFacetingAfterDistinct(true);
        assertEquals(Boolean.TRUE, query.getFacetingAfterDistinct());
        assertEquals("true", query.get("facetingAfterDistinct"));
        assertEquals(query.getFacetingAfterDistinct(), Query.parse(query.build()).getFacetingAfterDistinct());
    }
}