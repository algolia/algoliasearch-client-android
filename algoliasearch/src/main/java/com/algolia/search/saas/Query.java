package com.algolia.search.saas;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

/*
 * Copyright (c) 2015 Algolia
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

public class Query {
    public enum QueryType {
        /** All query words are interpreted as prefixes. */
        PREFIX_ALL,
        /** Only the last word is interpreted as a prefix. */
        PREFIX_LAST,
        /** No query word is interpreted as a prefix. This option is not recommended. */
        PREFIX_NONE
    }

    public enum RemoveWordsType {
        REMOVE_LAST_WORDS,
        REMOVE_FIRST_WORDS,
        REMOVE_NONE,
        REMOVE_ALLOPTIONAL
    }

    public enum TypoTolerance {
        TYPO_TRUE,
        TYPO_FALSE,
        TYPO_MIN,
        TYPO_STRICT
    }

    // ----------------------------------------------------------------------
    // IMPLEMENTATION NOTES
    // ----------------------------------------------------------------------
    // The query parameters are stored as an untyped map of strings.
    // This class provides:
    // - low-level accessors to the untyped parameters;
    // - higher-level, typed accessors.
    // The latter simply serialize their values into the untyped map and parse
    // them back from it.
    // ----------------------------------------------------------------------

    /** Query parameters, as un untyped key-value array. */
    // NOTE: Using a tree map to have parameters sorted by key on output.
    private Map<String, String> parameters = new TreeMap<>();

    // ----------------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------------

    /**
     * Construct an empty query.
     */
    public Query() {
    }

    /**
     * Construct a query with the specified query text.
     * @param query Query text.
     */
    public Query(String query) {
        setQuery(query);
    }

    /**
     * Clone a query.
     * @param other The query to be cloned.
     */
    public Query(@NonNull Query other) {
        parameters = new TreeMap<>(other.parameters);
    }

    // ----------------------------------------------------------------------
    // Misc.
    // ----------------------------------------------------------------------

    /**
     * Obtain a debug representation of this query.
     * To get the raw query URL part, please see {@link #build()}.
     * @return A debug representation of this query.
     */
    @Override public @NonNull String toString() {
        return String.format("%s{%s}", this.getClass().getSimpleName(), this.build());
    }

    // ----------------------------------------------------------------------
    // High-level (typed) accessors
    // ----------------------------------------------------------------------
    // Please keep them alphabetically sorted.

    private static final String KEY_ADVANCED_SYNTAX = "advancedSyntax";

    /**
     * Enable the advanced query syntax. Defaults to false. - Phrase query: a
     * phrase query defines a particular sequence of terms. A phrase query is
     * build by Algolia's query parser for words surrounded by ". For example,
     * "search engine" will retrieve records having search next to engine only.
     * Typo-tolerance is disabled on phrase queries. - Prohibit operator: The
     * prohibit operator excludes records that contain the term after the -
     * symbol. For example search -engine will retrieve records containing
     * search but not engine.
     */
    public @NonNull Query setAdvancedSyntax(Boolean enabled) {
        return set(KEY_ADVANCED_SYNTAX, enabled);
    }

    public Boolean getAdvancedSyntax() {
        return parseBoolean(get(KEY_ADVANCED_SYNTAX));
    }

    private static final String KEY_ALLOW_TYPOS_ON_NUMERIC_TOKENS = "allowTyposOnNumericTokens";

    /**
     * @param enabled
     *            If set to false, disable typo-tolerance on numeric tokens.
     *            Default to true.
     */
    public @NonNull Query setAllowTyposOnNumericTokens(Boolean enabled) {
        return set(KEY_ALLOW_TYPOS_ON_NUMERIC_TOKENS, enabled);
    }

    public Boolean getAllowTyposOnNumericTokens() {
        return parseBoolean(get(KEY_ALLOW_TYPOS_ON_NUMERIC_TOKENS));
    }

    private static final String KEY_ANALYTICS = "analytics";

    /**
     * @param enabled
     *            If set to false, this query will not be taken into account in
     *            analytics feature. Default to true.
     */
    public @NonNull Query setAnalytics(Boolean enabled) {
        return set(KEY_ANALYTICS, enabled);
    }

    public Boolean getAnalytics() {
        return parseBoolean(get(KEY_ANALYTICS));
    }

    private static final String KEY_ANALYTICS_TAGS = "analyticsTags";

    /**
     * @param tags
     *            Set the analytics tags identifying the query
     */
    public @NonNull Query setAnalyticsTags(String... tags) {
        return set(KEY_ANALYTICS_TAGS, buildJSONArray(tags));
    }

    public String[] getAnalyticsTags() {
        return parseArray(get(KEY_ANALYTICS_TAGS));
    }

    private static final String KEY_AROUND_LAT_LNG = "aroundLatLng";

    public static final class LatLng {
        public double lat;
        public double lng;
    }

    /**
     * Search for entries around a given latitude/longitude.
     *
     */
    public @NonNull Query setAroundLatLng(LatLng location) {
        if (location == null) {
            return set(KEY_AROUND_LAT_LNG, null);
        } else {
            return set(KEY_AROUND_LAT_LNG, String.format("%f,%f", location.lat, location.lng));
        }
    }

    public LatLng getAroundLatLng() {
        String value = get(KEY_AROUND_LAT_LNG);
        if (value == null) {
            return null;
        }
        String[] components = value.split(",");
        if (components.length != 2) {
            return null;
        }
        try {
            LatLng location = new LatLng();
            location.lat = Float.valueOf(components[0]);
            location.lng = Float.valueOf(components[1]);
            return location;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String KEY_AROUND_LAT_LNG_VIA_IP = "aroundLatLngViaIP";

    /**
     * Search for entries around the latitude/longitude of user (using IP
     * geolocation)
     *
     */
    public @NonNull Query setAroundLatLngViaIP(Boolean enabled) {
        return set(KEY_AROUND_LAT_LNG_VIA_IP, enabled);
    }

    public Boolean getAroundLatLngViaIP() {
        return parseBoolean(get(KEY_AROUND_LAT_LNG_VIA_IP));
    }

    private static final String KEY_AROUND_PRECISION = "aroundPrecision";

    /**
     * Change the radius or around latitude/longitude query
     */
    public @NonNull Query setAroundPrecision(Integer precision) {
        return set(KEY_AROUND_PRECISION, precision);
    }

    public Integer getAroundPrecision() {
        return parseInt(get(KEY_AROUND_PRECISION));
    }

    private static final String KEY_AROUND_RADIUS = "aroundRadius";

    /**
     * Change the radius or around latitude/longitude query
     */
    public @NonNull Query setAroundRadius(Integer radius) {
        return set(KEY_AROUND_RADIUS, radius);
    }

    public Integer getAroundRadius() {
        return parseInt(get(KEY_AROUND_RADIUS));
    }

    private static final String KEY_ATTRIBUTES_TO_HIGHLIGHT = "attributesToHighlight";

    /**
     * Specify the list of attribute names to highlight. By default indexed
     * attributes are highlighted.
     */
    public @NonNull Query setAttributesToHighlight(String... attributes) {
        return set(KEY_ATTRIBUTES_TO_HIGHLIGHT, buildJSONArray(attributes));
    }

    public String[] getAttributesToHighlight() {
        return parseArray(get(KEY_ATTRIBUTES_TO_HIGHLIGHT));
    }

    private static final String KEY_ATTRIBUTES_TO_RETRIEVE = "attributesToRetrieve";
    private static final String KEY_ATTRIBUTES_TO_RETRIEVE_LEGACY = "attributes";

    /**
     * Specify the list of attribute names to retrieve. By default all
     * attributes are retrieved.
     *
     */
    public @NonNull Query setAttributesToRetrieve(String... attributes) {
        return set(KEY_ATTRIBUTES_TO_RETRIEVE, buildJSONArray(attributes));
    }

    public String[] getAttributesToRetrieve() {
        String[] result = parseArray(get(KEY_ATTRIBUTES_TO_RETRIEVE));
        if (result == null) {
            result = parseArray(get(KEY_ATTRIBUTES_TO_RETRIEVE_LEGACY));
        }
        return result;
    }

    private static final String KEY_ATTRIBUTES_TO_SNIPPET = "attributesToSnippet";

    // TODO: Use class instead of string for snippet specification
    /**
     * Specify the list of attribute names to Snippet alongside the number of
     * words to return (syntax is 'attributeName:nbWords'). By default no
     * snippet is computed.
     */
    public @NonNull Query setAttributesToSnippet(String... attributes) {
        return set(KEY_ATTRIBUTES_TO_SNIPPET, buildJSONArray(attributes));
    }

    public String[] getAttributesToSnippet() {
        return parseArray(get(KEY_ATTRIBUTES_TO_SNIPPET));
    }

    private static final String KEY_DISABLE_TYPO_TOLERANCE_ON_ATTRIBUTES = "disableTypoToleranceOnAttributes";

    /**
     * List of attributes on which you want to disable typo tolerance (must be a subset of the attributesToIndex index setting).
     */
    public @NonNull Query setDisableTypoToleranceOnAttributes(String... attributes) {
        return set(KEY_DISABLE_TYPO_TOLERANCE_ON_ATTRIBUTES, buildJSONArray(attributes));
    }

    public String[] getDisableTypoToleranceOnAttributes() {
        return parseArray(get(KEY_DISABLE_TYPO_TOLERANCE_ON_ATTRIBUTES));
    }

    private static final String KEY_DISTINCT = "distinct";

    /**
     * This feature is similar to the distinct just before but instead of
     * keeping the best value per value of attributeForDistinct, it allows to
     * keep N values.
     *
     * @param nbHitsToKeep
     *            Specify the maximum number of hits to keep for each distinct
     *            value
     */
    public @NonNull Query setDistinct(Integer nbHitsToKeep) {
        return set(KEY_DISTINCT, nbHitsToKeep);
    }

    public Integer getDistinct() {
        return parseInt(get(KEY_DISTINCT));
    }

    private static final String KEY_FACETS = "facets";

    /**
     * List of object attributes that you want to use for faceting.
     * Only attributes that have been added in **attributesForFaceting** index
     * setting can be used in this parameter. You can also use `*` to perform
     * faceting on all attributes specified in **attributesForFaceting**.
     */
    public @NonNull Query setFacets(String... facets) {
        return set(KEY_FACETS, buildJSONArray(facets));
    }

    public String[] getFacets() {
        return parseArray(get(KEY_FACETS));
    }

    private static final String KEY_FACET_FILTERS = "facetFilters";

    public @NonNull Query setFacetFilters(JSONArray filters) {
        return set(KEY_FACET_FILTERS, filters);
    }

    public JSONArray getFacetFilters() {
        try {
            String value = get(KEY_FACET_FILTERS);
            if (value != null) {
                return new JSONArray(value);
            }
        }
        catch (JSONException e) {
            // Will return null
        }
        return null;
    }

    // NOTE: `filters` left out because type too complex.

    private static final String KEY_GET_RANKING_INFO = "getRankingInfo";

    /**
     * if set, the result hits will contain ranking information in _rankingInfo
     * attribute.
     */
    public @NonNull Query setGetRankingInfo(Boolean enabled) {
        return set(KEY_GET_RANKING_INFO, enabled);
    }

    public Boolean getGetRankingInfo() {
        return parseBoolean(get(KEY_GET_RANKING_INFO));
    }

    private static final String KEY_HIGHLIGHT_POST_TAG = "highlightPostTag";

    public @NonNull Query setHighlightPostTag(String tag) {
        return set(KEY_HIGHLIGHT_POST_TAG, tag);
    }

    public String getHighlightPostTag() {
        return get(KEY_HIGHLIGHT_POST_TAG);
    }

    private static final String KEY_HIGHLIGHT_PRE_TAG = "highlightPreTag";

    public @NonNull Query setHighlightPreTag(String tag) {
        return set(KEY_HIGHLIGHT_PRE_TAG, tag);
    }

    public String getHighlightPreTag() {
        return get(KEY_HIGHLIGHT_PRE_TAG);
    }

    private static final String KEY_HITS_PER_PAGE = "hitsPerPage";

    /**
     * Set the number of hits per page. Defaults to 10.
     */
    public @NonNull Query setHitsPerPage(Integer nbHitsPerPage) {
        return set(KEY_HITS_PER_PAGE, nbHitsPerPage);
    }

    public Integer getHitsPerPage() {
        return parseInt(get(KEY_HITS_PER_PAGE));
    }

    private static final String KEY_IGNORE_PLURALS = "ignorePlurals";

    /**
     * If set to true, plural won't be considered as a typo (for example
     * car/cars will be considered as equals). Default to false.
     */
    public @NonNull Query setIgnorePlurals(Boolean enabled) {
        return set(KEY_IGNORE_PLURALS, enabled);
    }

    public Boolean getIgnorePlurals() {
        return parseBoolean(get(KEY_IGNORE_PLURALS));
    }

    public static final class GeoRect
    {
        public LatLng p1;
        public LatLng p2;
    }

    private static final String KEY_INSIDE_BOUNDING_BOX = "insideBoundingBox";

    /**
     * Search for entries inside a given area defined by the two extreme points
     * of a rectangle. At indexing, geoloc of an object should be set with
     * _geoloc attribute containing lat and lng attributes (for example
     * {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     *
     * You can use several bounding boxes (OR) by calling this method several times.
     */
    public @NonNull Query setInsideBoundingBox(GeoRect... boxes) {
        if (boxes == null) {
            set(KEY_INSIDE_BOUNDING_BOX, null);
        } else {
            StringBuilder sb = new StringBuilder();
            for (GeoRect box : boxes) {
                if (sb.length() != 0) {
                    sb.append(',');
                }
                sb.append(box.p1.lat);
                sb.append(',');
                sb.append(box.p1.lng);
                sb.append(',');
                sb.append(box.p2.lat);
                sb.append(',');
                sb.append(box.p2.lng);
            }
            set(KEY_INSIDE_BOUNDING_BOX, sb.toString());
        }
        return this;
    }

    public GeoRect[] getInsideBoundingBox() {
        try {
            String value = get(KEY_INSIDE_BOUNDING_BOX);
            if (value != null) {
                String[] fields = value.split(",");
                if (fields.length % 4 == 0) {
                    GeoRect[] result = new GeoRect[fields.length / 4];
                    for (int i = 0; i < result.length; ++i) {
                        GeoRect box = new GeoRect();
                        box.p1.lat = Double.parseDouble(fields[4 * i]);
                        box.p1.lng = Double.parseDouble(fields[4 * i + 1]);
                        box.p2.lat = Double.parseDouble(fields[4 * i + 2]);
                        box.p2.lng = Double.parseDouble(fields[4 * i + 3]);
                        result[i] = box;
                    }
                    return result;
                }
            }
            return null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String KEY_INSIDE_POLYGON = "insidePolygon";

    /**
     * Add a point to the polygon of geo-search (requires a minimum of three points to define a valid polygon)
     * At indexing, you should specify geoloc of an object with the _geoloc attribute (in the form "_geoloc":{"lat":48.853409, "lng":2.348800} or
     * "_geoloc":[{"lat":48.853409, "lng":2.348800},{"lat":48.547456, "lng":2.972075}] if you have several geo-locations in your record).
     */
    public @NonNull Query setInsidePolygon(LatLng... points) {
        if (points == null) {
            set(KEY_INSIDE_POLYGON, null);
        } else if (points.length < 3) {
            throw new IllegalArgumentException("A polygon must have at least three vertices");
        } else {
            StringBuilder sb = new StringBuilder();
            for (LatLng point : points) {
                if (sb.length() != 0) {
                    sb.append(',');
                }
                sb.append(point.lat);
                sb.append(',');
                sb.append(point.lng);
            }
            set(KEY_INSIDE_POLYGON, sb.toString());
        }
        return this;
    }

    public LatLng[] getInsidePolygon() {
        try {
            String value = get(KEY_INSIDE_POLYGON);
            if (value != null) {
                String[] fields = value.split(",");
                if (fields.length % 2 == 0 && fields.length / 2 >= 3) {
                    LatLng[] result = new LatLng[fields.length / 2];
                    for (int i = 0; i < result.length; ++i) {
                        LatLng point = new LatLng();
                        point.lat = Double.parseDouble(fields[2 * i]);
                        point.lng = Double.parseDouble(fields[2 * i + 1]);
                        result[i] = point;
                    }
                    return result;
                }
            }
            return null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String KEY_MAX_VALUES_PER_FACET = "maxValuesPerFacet";

    /**
     * Limit the number of facet values returned for each facet.
     */
    public @NonNull Query setMaxValuesPerFacet(Integer n) {
        return set(KEY_MAX_VALUES_PER_FACET, n);
    }

    public Integer getMaxValuesPerFacet() {
        return parseInt(get(KEY_MAX_VALUES_PER_FACET));
    }

    private static final String KEY_MINIMUM_AROUND_RADIUS = "minimumAroundRadius";

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public @NonNull Query setMinimumAroundRadius(Integer minimumAroundRadius) {
        return set(KEY_MINIMUM_AROUND_RADIUS, minimumAroundRadius);
    }

    public Integer getMinimumAroundRadius() {
        return parseInt(get(KEY_MINIMUM_AROUND_RADIUS));
    }

    private static final String KEY_MIN_PROXIMITY = "minProximity";

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public @NonNull Query setMinProximity(Integer nbChars) {
        return set(KEY_MIN_PROXIMITY, nbChars);
    }

    public Integer getMinProximity() {
        return parseInt(get(KEY_MIN_PROXIMITY));
    }

    private static final String KEY_MIN_WORD_SIZE_FOR_1_TYPO = "minWordSizefor1Typo";

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public @NonNull Query setMinWordSizeFor1Typo(Integer nbChars) {
        return set(KEY_MIN_WORD_SIZE_FOR_1_TYPO, nbChars);
    }

    public Integer getMinWordSizeFor1Typo() {
        return parseInt(get(KEY_MIN_WORD_SIZE_FOR_1_TYPO));
    }

    private static final String KEY_MIN_WORD_SIZE_FOR_2_TYPOS = "minWordSizefor2Typos";

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public @NonNull Query setMinWordSizeFor2Typos(Integer nbChars) {
        return set(KEY_MIN_WORD_SIZE_FOR_2_TYPOS, nbChars);
    }

    public Integer getMinWordSizeFor2Typos() {
        return parseInt(get(KEY_MIN_WORD_SIZE_FOR_2_TYPOS));
    }

    // NOTE: `numericFilters` left out because type too complex.

    private static final String KEY_OPTIONAL_WORDS = "optionalWords";

    /**
     * Set the list of words that should be considered as optional when found in
     * the query.
     *
     * @param words
     *            The list of optional words, comma separated.
     */
    public @NonNull Query setOptionalWords(String... words) {
        return set(KEY_OPTIONAL_WORDS, buildJSONArray(words));
    }

    public String[] getOptionalWords() {
        return parseArray(get(KEY_OPTIONAL_WORDS));
    }

    private static final String KEY_PAGE = "page";

    /**
     * Set the page to retrieve (zero base). Defaults to 0.
     */
    public @NonNull Query setPage(Integer page) {
        return set(KEY_PAGE, page);
    }

    public Integer getPage() {
        return parseInt(get(KEY_PAGE));
    }

    private static final String KEY_QUERY = "query";

    /**
     * Set the full text query
     */
    public @NonNull Query setQuery(String query) {
        return set(KEY_QUERY, query);
    }

    public String getQuery() {
        return get(KEY_QUERY);
    }

    private static final String KEY_QUERY_TYPE = "queryType";

    /**
     * Select how the query words are interpreted:
     */
    public @NonNull Query setQueryType(QueryType type) {
        if (type == null) {
            set(KEY_QUERY_TYPE, null);
        } else {
            switch (type) {
                case PREFIX_LAST:
                    set(KEY_QUERY_TYPE, "prefixLast");
                    break;
                case PREFIX_NONE:
                    set(KEY_QUERY_TYPE, "prefixNone");
                    break;
                case PREFIX_ALL:
                    set(KEY_QUERY_TYPE, "prefixAll");
                    break;
            }
        }
        return this;
    }

    public QueryType getQueryType()
    {
        String value = get(KEY_QUERY_TYPE);
        if (value != null) {
            switch (value) {
                case "prefixLast":
                    return QueryType.PREFIX_LAST;
                case "prefixNone":
                    return QueryType.PREFIX_NONE;
                case "prefixAll":
                    return QueryType.PREFIX_ALL;
            }
        }
        return null;
    }

    private static final String KEY_REMOVE_STOP_WORDS = "removeStopWords";

    /**
     * Enable the removal of stop words. Defaults to false.
     */
    public @NonNull Query setRemoveStopWords(Boolean removeStopWords) {
        return set(KEY_REMOVE_STOP_WORDS, removeStopWords);
    }

    public Boolean getRemoveStopWords() {
        return parseBoolean(get(KEY_REMOVE_STOP_WORDS));
    }

    private static final String KEY_REMOVE_WORDS_IF_NO_RESULT = "removeWordsIfNoResult";

    /**
     * Select the strategy to adopt when a query does not return any result.
     */
    public @NonNull Query setRemoveWordsIfNoResult(RemoveWordsType type) {
        if (type == null) {
            set(KEY_REMOVE_WORDS_IF_NO_RESULT, null);
        } else {
            switch (type) {
                case REMOVE_LAST_WORDS:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "lastWords");
                    break;
                case REMOVE_FIRST_WORDS:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "firstWords");
                    break;
                case REMOVE_ALLOPTIONAL:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "allOptional");
                    break;
                case REMOVE_NONE:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "none");
                    break;
            }
        }
        return this;
    }

    public RemoveWordsType getRemoveWordsIfNoResult()
    {
        String value = get(KEY_REMOVE_WORDS_IF_NO_RESULT);
        if (value != null) {
            switch (value) {
                case "lastWords":
                    return RemoveWordsType.REMOVE_LAST_WORDS;
                case "firstWords":
                    return RemoveWordsType.REMOVE_FIRST_WORDS;
                case "allOptional":
                    return RemoveWordsType.REMOVE_ALLOPTIONAL;
                case "none":
                    return RemoveWordsType.REMOVE_NONE;
            }
        }
        return null;
    }

    private static final String KEY_REPLACE_SYNONYMS_IN_HIGHLIGHT = "replaceSynonymsInHighlight";

    /**
     * @param enabled
     *            If set to false, words matched via synonyms expansion will not be
     *            replaced by the matched synonym in highlight result. Default
     *            to true.
     */
    public @NonNull Query setReplaceSynonymsInHighlight(Boolean enabled) {
        return set(KEY_REPLACE_SYNONYMS_IN_HIGHLIGHT, enabled);
    }

    public Boolean getReplaceSynonymsInHighlight() {
        return parseBoolean(get(KEY_REPLACE_SYNONYMS_IN_HIGHLIGHT));
    }

    private static final String KEY_RESTRICT_SEARCHABLE_ATTRIBUTES = "restrictSearchableAttributes";

    /**
     * List of object attributes you want to use for textual search (must be a
     * subset of the attributesToIndex index setting). Attributes are separated
     * with a comma (for example @"name,address"). You can also use a JSON
     * string array encoding (for example
     * encodeURIComponent("[\"name\",\"address\"]")). By default, all attributes
     * specified in attributesToIndex settings are used to search.
     */
    public @NonNull Query setRestrictSearchableAttributes(String... attributes) {
        return set(KEY_RESTRICT_SEARCHABLE_ATTRIBUTES, buildJSONArray(attributes));
    }

    public String[] getRestrictSearchableAttributes()
    {
        return parseArray(get(KEY_RESTRICT_SEARCHABLE_ATTRIBUTES));
    }

    private static final String KEY_SNIPPET_ELLIPSIS_TEXT = "snippetEllipsisText";

    /**
     * Specify the string that is used as an ellipsis indicator when a snippet
     * is truncated (defaults to the empty string).
     */
    public @NonNull Query setSnippetEllipsisText(String snippetEllipsisText) {
        return set(KEY_SNIPPET_ELLIPSIS_TEXT, snippetEllipsisText);
    }

    public String getSnippetEllipsisText() {
        return get(KEY_SNIPPET_ELLIPSIS_TEXT);
    }

    private static final String KEY_SYNONYMS = "synonyms";

    /**
     * @param enabled
     *            If set to false, this query will not use synonyms defined in
     *            configuration. Default to true.
     */
    public @NonNull Query setSynonyms(Boolean enabled) {
        return set(KEY_SYNONYMS, enabled);
    }

    public Boolean getSynonyms() {
        return parseBoolean(get(KEY_SYNONYMS));
    }

    private static final String KEY_TAG_FILTERS = "tagFilters";

    public @NonNull Query setTagFilters(JSONArray tagFilters) {
        return set(KEY_TAG_FILTERS, tagFilters);
    }

    public JSONArray getTagFilters() {
        try {
            String value = get(KEY_TAG_FILTERS);
            if (value != null) {
                return new JSONArray(value);
            }
        }
        catch (JSONException e) {
            // Will return null
        }
        return null;
    }

    private static final String KEY_TYPO_TOLERANCE = "typoTolerance";

    public @NonNull Query setTypoTolerance(TypoTolerance type) {
        if (type == null) {
            set(KEY_TYPO_TOLERANCE, null);
        } else {
            switch (type) {
                case TYPO_FALSE:
                    set(KEY_TYPO_TOLERANCE, "false");
                    break;
                case TYPO_MIN:
                    set(KEY_TYPO_TOLERANCE, "min");
                    break;
                case TYPO_STRICT:
                    set(KEY_TYPO_TOLERANCE, "strict");
                    break;
                case TYPO_TRUE:
                    set(KEY_TYPO_TOLERANCE, "true");
                    break;
            }
        }
        return this;
    }

    public TypoTolerance getTypoTolerance()
    {
        String value = get(KEY_TYPO_TOLERANCE);
        if (value != null) {
            switch (value) {
                case "false":
                    return TypoTolerance.TYPO_FALSE;
                case "min":
                    return TypoTolerance.TYPO_MIN;
                case "strict":
                    return TypoTolerance.TYPO_STRICT;
                case "true":
                    return TypoTolerance.TYPO_TRUE;
            }
        }
        return null;
    }

    // TODO: tag filters
    // TODO: facet filters
    // TODO: numeric filters

    // ----------------------------------------------------------------------
    // Parsing/serialization
    // ----------------------------------------------------------------------

    /**
     * Build the URL query parameter string representing this object.
     * @return A string suitable for use inside the query part of a URL (i.e. after the question mark).
     */
    protected @NonNull String build() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(urlEncode(entry.getKey()));
                String value = entry.getValue();
                if (value != null) {
                    stringBuilder.append('=');
                    stringBuilder.append(urlEncode(value));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // should never happen: UTF-8 is always supported
        }
        return stringBuilder.toString();
    }

    static private String urlEncode(String value) throws UnsupportedEncodingException {
        // NOTE: We prefer to have space encoded as `%20` instead of `+`, so we patch `URLEncoder`'s behaviour.
        // This works because `+` itself is percent-escaped (into `%2B`).
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    /**
     * Parse a query object from a URL query parameter string.
     * @param queryParameters URL query parameter string.
     * @return The parsed query object.
     */
    protected static @NonNull Query parse(@NonNull String queryParameters) {
        try {
            Query query = new Query();
            String[] parameters = queryParameters.split("&");
            for (String parameter : parameters) {
                String[] components = parameter.split("=");
                if (components.length < 1 || components.length > 2)
                    continue; // ignore invalid values
                String name = URLDecoder.decode(components[0], "UTF-8");
                String value = components.length >= 2 ? URLDecoder.decode(components[1], "UTF-8") : null;
                query.set(name, value);
            } // for each parameter
            return query;
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF-8 is one of the default encodings.
            throw new RuntimeException(e);
        }
    }

    private static Boolean parseBoolean(String value) {
        if (value == null) {
            return null;
        }
        if (value.trim().toLowerCase().equals("true")) {
            return true;
        }
        Integer intValue = parseInt(value);
        return intValue != null && intValue != 0;
    }

    private static Integer parseInt(String value)  {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String buildJSONArray(String[] values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array.toString();
    }

    private static String[] parseArray(String string) {
        if (string == null) {
            return null;
        }
        // First try to parse JSON notation.
        try {
            JSONArray array = new JSONArray(string);
            String[] result = new String[array.length()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = array.optString(i);
            }
            return result;
        }
        // Otherwise parse as a comma-separated list.
        catch (JSONException e) {
            return string.split(",");
        }
    }

    private static String buildCommaArray(String[] values) {
        return TextUtils.join(",", values);
    }

    private static String[] parseCommaArray(String string) {
        return string == null ? null : string.split(",");
    }

    // ----------------------------------------------------------------------
    // Low-level (untyped) accessors
    // ----------------------------------------------------------------------

    /**
     * Set a parameter in an untyped fashion.
     * This low-level accessor is intended to access parameters that this client does not yet support.
     * @param name The parameter's name.
     * @param value The parameter's value, or null to remove it.
     *              It will first be converted to a String by the `toString()` method.
     */
    public @NonNull Query set(@NonNull String name, Object value) {
        if (value == null) {
            parameters.remove(name);
        } else {
            parameters.put(name, value.toString());
        }
        return this;
    }

    /**
     * Get a parameter in an untyped fashion.
     * @param name The parameter's name.
     * @return The parameter's value, or null if a parameter with the specified name does not exist.
     */
    public String get(@NonNull String name) {
        return parameters.get(name);
    }
}