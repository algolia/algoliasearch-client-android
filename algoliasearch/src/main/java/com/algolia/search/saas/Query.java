package com.algolia.search.saas;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

/**
 * Describes all parameters of a search query.
 * <p>
 * There are two ways to access parameters:
 * <p>
 * 1. Using the high-level, typed properties for individual parameters (recommended).
 * 2. Using the low-level, untyped getter (`get()`) and setter (`set()`) or the subscript operator.
 * Use this approach if the parameter you wish to set is not supported by this class.
 */
@SuppressWarnings("WeakerAccess") // Enums & Methods are voluntarily public
public class Query extends AbstractQuery {
    public enum QueryType {
        /** All query words are interpreted as prefixes. */
        PREFIX_ALL,
        /** Only the last word is interpreted as a prefix. */
        PREFIX_LAST,
        /** No query word is interpreted as a prefix. This option is not recommended. */
        PREFIX_NONE
    }

    public enum RemoveWordsIfNoResults {
        LAST_WORDS,
        FIRST_WORDS,
        NONE,
        ALL_OPTIONAL
    }

    public enum TypoTolerance {
        TRUE,
        FALSE,
        MIN,
        STRICT
    }

    public enum ExactOnSingleWordQuery {
        NONE,
        ATTRIBUTE,
        WORD
    }

    public enum AlternativesAsExact {
        IGNORE_PLURALS,
        SINGLE_WORD_SYNONYM,
        MULTI_WORDS_SYNONYM
    }

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
     *
     * @param query Query text.
     */
    public Query(String query) {
        setQuery(query);
    }

    /**
     * Clone an existing query.
     *
     * @param other The query to be cloned.
     */
    public Query(@NonNull Query other) {
        super(other);
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
     * @param enabled If set to false, disable typo-tolerance on numeric tokens.
     *                Defaults to true.
     */
    public @NonNull Query setAllowTyposOnNumericTokens(Boolean enabled) {
        return set(KEY_ALLOW_TYPOS_ON_NUMERIC_TOKENS, enabled);
    }

    public Boolean getAllowTyposOnNumericTokens() {
        return parseBoolean(get(KEY_ALLOW_TYPOS_ON_NUMERIC_TOKENS));
    }

    private static final String KEY_ANALYTICS = "analytics";

    /**
     * @param enabled If set to false, this query will not be taken into account in
     *                analytics feature. Defaults to true.
     */
    public @NonNull Query setAnalytics(Boolean enabled) {
        return set(KEY_ANALYTICS, enabled);
    }

    public Boolean getAnalytics() {
        return parseBoolean(get(KEY_ANALYTICS));
    }

    private static final String KEY_ANALYTICS_TAGS = "analyticsTags";

    /**
     * @param tags Set the analytics tags identifying the query
     */
    public @NonNull Query setAnalyticsTags(String... tags) {
        return set(KEY_ANALYTICS_TAGS, buildJSONArray(tags));
    }

    public String[] getAnalyticsTags() {
        return parseArray(get(KEY_ANALYTICS_TAGS));
    }

    private static final String KEY_AROUND_LAT_LNG = "aroundLatLng";

    /**
     * Search for entries around a given latitude/longitude.
     */
    public @NonNull Query setAroundLatLng(@Nullable LatLng location) {
        if (location == null) {
            return set(KEY_AROUND_LAT_LNG, null);
        } else {
            return set(KEY_AROUND_LAT_LNG, location.lat + "," + location.lng);
        }
    }

    public @Nullable LatLng getAroundLatLng() {
        return LatLng.parse(get(KEY_AROUND_LAT_LNG));
    }

    private static final String KEY_AROUND_LAT_LNG_VIA_IP = "aroundLatLngViaIP";

    /**
     * Search for entries around the latitude/longitude of user (using IP
     * geolocation)
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
    public static final int RADIUS_ALL = Integer.MAX_VALUE;

    /**
     * Change the radius for around latitude/longitude queries.
     *
     * @param radius the radius to set, or Query.RADIUS_ALL to disable stopping at a specific radius.
     */
    public @NonNull Query setAroundRadius(Integer radius) {
        if (radius == Query.RADIUS_ALL) {
            return set(KEY_AROUND_RADIUS, "all");
        }
        return set(KEY_AROUND_RADIUS, radius);
    }

    /**
     * Get the current radius for around latitude/longitude queries.
     *
     * @return Query.RADIUS_ALL if set to 'all'.
     */
    public Integer getAroundRadius() {
        final String value = get(KEY_AROUND_RADIUS);
        if (value != null && value.equals("all")) {
            return Query.RADIUS_ALL;
        }
        return parseInt(value);
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
     * List of attributes on which you want to disable typo tolerance (must be a subset of the `searchableAttributes` index setting).
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
     * @param nbHitsToKeep Specify the maximum number of hits to keep for each distinct
     *                     value
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

    /**
     * Set the <b>deprecated</b> {@code facetFilters} parameter.
     *
     * @deprecated Use {@link Query#setFilters(String)} instead.
     */
    public @NonNull Query setFacetFilters(JSONArray filters) {
        return set(KEY_FACET_FILTERS, filters);
    }

    /**
     * Get the value of <b>deprecated</b> {@code facetFilters} parameter.
     *
     * @deprecated Use {@link Query#getFilters()} instead.
     */
    public @Nullable JSONArray getFacetFilters() {
        try {
            String value = get(KEY_FACET_FILTERS);
            if (value != null) {
                return new JSONArray(value);
            }
        } catch (JSONException e) {
            // Will return null
        }
        return null;
    }

    private static final String KEY_FILTERS = "filters";

    /**
     * Filter the query with numeric, facet or/and tag filters.
     * <p>
     * The syntax is a SQL like syntax, you can use the OR and AND keywords. The syntax for the underlying numeric, facet and tag filters is the same than in the other filters:
     * {@code available=1 AND (category:Book OR NOT category:Ebook) AND _tags:public date: 1441745506 TO 1441755506 AND inStock > 0 AND author:"John Doe"}
     *
     * @param filters a string following the given syntax.
     * @return the {@link Query} for chaining.
     */
    public @NonNull Query setFilters(String filters) {
        return set(KEY_FILTERS, filters);
    }

    /**
     * Get the numeric, facet or/and tag filters for this Query.
     *
     * @return a String with this query's filters.
     */
    public @Nullable String getFilters() {
        return get(KEY_FILTERS);
    }

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

    public @Nullable String getHighlightPostTag() {
        return get(KEY_HIGHLIGHT_POST_TAG);
    }

    private static final String KEY_HIGHLIGHT_PRE_TAG = "highlightPreTag";

    public @NonNull Query setHighlightPreTag(String tag) {
        return set(KEY_HIGHLIGHT_PRE_TAG, tag);
    }

    public @Nullable String getHighlightPreTag() {
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
     * A value of the {@code ignorePlurals} setting.
     * Can represent either a boolean or a list of language codes, see https://www.algolia.com/doc/faq/searching/how-does-ignoreplurals-work.
     */
    public static final class IgnorePlurals {
        /** Whether plurals are ignored. */
        public final boolean enabled;

        /** A list containing every active language's code. When {@code null}, all supported languages are be used. */
        public @Nullable final List<String> languageCodes;

        /**
         * Construct an IgnorePlurals object for a boolean value.
         *
         * @param b if {@code true}, the engine will ignore plurals in all supported languages.
         */
        public IgnorePlurals(boolean b) {
            this.enabled = b;
            languageCodes = null;
        }

        /**
         * Construct an IgnorePlurals object for a {@link Collection} of language codes.
         *
         * @param codes a list of language codes to ignore plurals from. if {@code null},
         *              the engine will ignore plurals in all supported languages.
         */
        public IgnorePlurals(@Nullable Collection<String> codes) {
            this.enabled = !isEmptyCollection(codes);
            languageCodes = codes != null ? new ArrayList<>(codes) : null;
        }

        /**
         * Construct an IgnorePlurals object for some language codes.
         *
         * @param codes one or several language codes to ignore plurals from.
         *              if {@code null}, the engine will ignore plurals in all supported languages.
         */
        public IgnorePlurals(@Nullable String... codes) {
            this(codes == null ? null : Arrays.asList(codes));
        }

        private boolean isEmptyCollection(@Nullable Collection<String> codesList) {
            return codesList == null || codesList.size() == 0;
        }

        @Override
        public String toString() {
            if (!enabled) {
                return "false";
            } else {
                if (isEmptyCollection(languageCodes)) {  // enabled without specific language
                    return "true";
                } else {
                    return TextUtils.join(",", languageCodes);
                }
            }
        }

        static @NonNull IgnorePlurals parse(@Nullable String s) {
            if (s == null || s.length() == 0 || s.equals("null")) {
                return new IgnorePlurals(false);
            } else if ("true".equals(s) || "false".equals(s)) {
                return new IgnorePlurals(parseBoolean(s));
            } else {
                ArrayList<String> codesList = new ArrayList<>();
                //ignorePlurals=["en","fi"]
                try {
                    JSONArray codesArray = new JSONArray(s);
                    for (int i = 0; i < codesArray.length(); i++) {
                        codesList.add(codesArray.getJSONObject(i).toString());
                    }
                    return new IgnorePlurals(codesList);
                } catch (JSONException e) {
                    // s was not a JSONArray of strings. Maybe it is a comma-separated list?
                    final String[] split = TextUtils.split(s, ",");
                    if (split != null && split.length != 0) {
                        Collections.addAll(codesList, split);
                        return new IgnorePlurals(codesList);
                    } else {
                        final String msg = "Error while parsing `" + s + "`: invalid ignorePlurals value.";
                        throw new RuntimeException(msg);
                    }
                }
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IgnorePlurals that = (IgnorePlurals) o;

            if (enabled != that.enabled) {
                return false;
            }
            return languageCodes != null ? languageCodes.equals(that.languageCodes) : that.languageCodes == null;

        }

        @Override
        public int hashCode() {
            int result = (enabled ? 1 : 0);
            result = 31 * result + (languageCodes != null ? languageCodes.hashCode() : 0);
            return result;
        }
    }

    /**
     * If set to true, plural won't be considered as a typo (for example
     * car/cars will be considered as equals). Defaults to false.
     */
    public @NonNull Query setIgnorePlurals(boolean enabled) {
        return set(KEY_IGNORE_PLURALS, enabled);
    }

    /**
     * A list of language codes for which plural won't be considered as a typo (for example
     * car/cars will be considered as equals). If empty or null, this disables the feature.
     */
    public @NonNull Query setIgnorePlurals(@Nullable Collection<String> languageISOCodes) {
        return set(KEY_IGNORE_PLURALS, new IgnorePlurals(languageISOCodes));
    }

    /**
     * One or several language codes for which plural won't be considered as a typo (for example
     * car/cars will be considered as equals). If empty or null, this disables the feature.
     */
    public @NonNull Query setIgnorePlurals(@Nullable String... languageISOCodes) {
        return set(KEY_IGNORE_PLURALS, new IgnorePlurals(languageISOCodes));
    }

    public @NonNull IgnorePlurals getIgnorePlurals() {
        return IgnorePlurals.parse(get(KEY_IGNORE_PLURALS));
    }

    /**
     * A rectangle in geo coordinates.
     * Used in geo-search.
     */
    public static final class GeoRect {
        @NonNull
        public final LatLng p1;
        @NonNull
        public final LatLng p2;

        public GeoRect(@NonNull LatLng p1, @NonNull LatLng p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other != null && other instanceof GeoRect
                    && this.p1.equals(((GeoRect) other).p1)
                    && this.p2.equals(((GeoRect) other).p2);
        }

        @Override
        public int hashCode() {
            return p1.hashCode() ^ p2.hashCode();
        }
    }

    private static final String KEY_INSIDE_BOUNDING_BOX = "insideBoundingBox";

    /**
     * Search for entries inside a given area defined by the two extreme points
     * of a rectangle. At indexing, geoloc of an object should be set with
     * _geoloc attribute containing lat and lng attributes (for example
     * {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     * <p>
     * You can use several bounding boxes (OR) by calling this method several times.
     */
    public @NonNull Query setInsideBoundingBox(@Nullable GeoRect... boxes) {
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

    public @Nullable GeoRect[] getInsideBoundingBox() {
        try {
            String value = get(KEY_INSIDE_BOUNDING_BOX);
            if (value != null) {
                String[] fields = value.split(",");
                if (fields.length % 4 == 0) {
                    GeoRect[] result = new GeoRect[fields.length / 4];
                    for (int i = 0; i < result.length; ++i) {
                        result[i] = new GeoRect(
                                new LatLng(
                                        Double.parseDouble(fields[4 * i]),
                                        Double.parseDouble(fields[4 * i + 1])
                                ), new LatLng(
                                Double.parseDouble(fields[4 * i + 2]),
                                Double.parseDouble(fields[4 * i + 3])
                        )
                        );
                    }
                    return result;
                }
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String KEY_INSIDE_POLYGON = "insidePolygon";

    /**
     * Add a point to the polygon of geo-search (requires a minimum of three points to define a valid polygon)
     * At indexing, you should specify geoloc of an object with the _geoloc attribute (in the form "_geoloc":{"lat":48.853409, "lng":2.348800} or
     * "_geoloc":[{"lat":48.853409, "lng":2.348800},{"lat":48.547456, "lng":2.972075}] if you have several geo-locations in your record).
     */
    public @NonNull Query setInsidePolygon(@Nullable LatLng... points) {
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

    public @Nullable LatLng[] getInsidePolygon() {
        try {
            String value = get(KEY_INSIDE_POLYGON);
            if (value != null) {
                String[] fields = value.split(",");
                if (fields.length % 2 == 0 && fields.length / 2 >= 3) {
                    LatLng[] result = new LatLng[fields.length / 2];
                    for (int i = 0; i < result.length; ++i) {
                        result[i] = new LatLng(
                                Double.parseDouble(fields[2 * i]),
                                Double.parseDouble(fields[2 * i + 1])
                        );
                    }
                    return result;
                }
            }
            return null;
        } catch (NumberFormatException e) {
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
    public @NonNull Query setMinWordSizefor1Typo(Integer nbChars) {
        return set(KEY_MIN_WORD_SIZE_FOR_1_TYPO, nbChars);
    }

    public Integer getMinWordSizefor1Typo() {
        return parseInt(get(KEY_MIN_WORD_SIZE_FOR_1_TYPO));
    }

    private static final String KEY_MIN_WORD_SIZE_FOR_2_TYPOS = "minWordSizefor2Typos";

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public @NonNull Query setMinWordSizefor2Typos(Integer nbChars) {
        return set(KEY_MIN_WORD_SIZE_FOR_2_TYPOS, nbChars);
    }

    public Integer getMinWordSizefor2Typos() {
        return parseInt(get(KEY_MIN_WORD_SIZE_FOR_2_TYPOS));
    }

    private static final String KEY_NUMERIC_FILTERS = "numericFilters";

    /**
     * Set the <b>deprecated</b> {@code numericFilters} parameter.
     *
     * @deprecated Use {@link Query#setFilters(String)} instead.
     */
    public @NonNull Query setNumericFilters(JSONArray filters) {
        return set(KEY_NUMERIC_FILTERS, filters);
    }

    /**
     * Get the value of <b>deprecated</b> {@code facetFilters} parameter.
     *
     * @deprecated Use {@link Query#getFilters()} instead.
     */
    public @Nullable JSONArray getNumericFilters() {
        try {
            String value = get(KEY_NUMERIC_FILTERS);
            if (value != null) {
                return new JSONArray(value);
            }
        } catch (JSONException e) {
            // Will return null
        }
        return null;
    }

    private static final String KEY_OPTIONAL_WORDS = "optionalWords";

    /**
     * Set the list of words that should be considered as optional when found in
     * the query.
     *
     * @param words The list of optional words, comma separated.
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

    public @Nullable String getQuery() {
        return get(KEY_QUERY);
    }

    private static final String KEY_QUERY_TYPE = "queryType";

    /**
     * Select how the query words are interpreted:
     */
    public @NonNull Query setQueryType(@Nullable QueryType type) {
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

    public @Nullable QueryType getQueryType() {
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
     * Enable the removal of stop words, disabled by default.
     * In most use-cases, we don’t recommend enabling this option.
     *
     * @param removeStopWords Boolean: enable or disable all 41 supported languages,
     *                        String: comma separated list of languages you have in your record (using language iso code).
     */
    public @NonNull Query setRemoveStopWords(Object removeStopWords) throws AlgoliaException {
        if (removeStopWords instanceof Boolean || removeStopWords instanceof String) {
            return set(KEY_REMOVE_STOP_WORDS, removeStopWords);
        }
        throw new AlgoliaException("removeStopWords should be a Boolean or a String.");
    }

    public @Nullable Object getRemoveStopWords() {
        final String value = get(KEY_REMOVE_STOP_WORDS);
        if (value == null) {
            return null;
        }
        final String[] commaArray = parseCommaArray(value);
        if (commaArray.length == 1 && (commaArray[0].equals("false") || commaArray[0].equals("true"))) {
            return parseBoolean(value);
        }
        return commaArray;
    }

    private static final String KEY_REMOVE_WORDS_IF_NO_RESULT = "removeWordsIfNoResults";

    /**
     * Select the strategy to adopt when a query does not return any result.
     */
    public @NonNull Query setRemoveWordsIfNoResults(@Nullable RemoveWordsIfNoResults type) {
        if (type == null) {
            set(KEY_REMOVE_WORDS_IF_NO_RESULT, null);
        } else {
            switch (type) {
                case LAST_WORDS:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "lastWords");
                    break;
                case FIRST_WORDS:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "firstWords");
                    break;
                case ALL_OPTIONAL:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "allOptional");
                    break;
                case NONE:
                    set(KEY_REMOVE_WORDS_IF_NO_RESULT, "none");
                    break;
            }
        }
        return this;
    }

    public @Nullable RemoveWordsIfNoResults getRemoveWordsIfNoResults() {
        String value = get(KEY_REMOVE_WORDS_IF_NO_RESULT);
        if (value != null) {
            switch (value) {
                case "lastWords":
                    return RemoveWordsIfNoResults.LAST_WORDS;
                case "firstWords":
                    return RemoveWordsIfNoResults.FIRST_WORDS;
                case "allOptional":
                    return RemoveWordsIfNoResults.ALL_OPTIONAL;
                case "none":
                    return RemoveWordsIfNoResults.NONE;
            }
        }
        return null;
    }

    private static final String KEY_REPLACE_SYNONYMS_IN_HIGHLIGHT = "replaceSynonymsInHighlight";

    /**
     * @param enabled If set to false, words matched via synonyms expansion will not be
     *                replaced by the matched synonym in highlight result. Default
     *                to true.
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
     * subset of the `searchableAttributes` index setting). Attributes are separated
     * with a comma (for example @"name,address"). You can also use a JSON
     * string array encoding (for example
     * encodeURIComponent("[\"name\",\"address\"]")). By default, all attributes
     * specified in `searchableAttributes` settings are used to search.
     */
    public @NonNull Query setRestrictSearchableAttributes(String... attributes) {
        return set(KEY_RESTRICT_SEARCHABLE_ATTRIBUTES, buildJSONArray(attributes));
    }

    public String[] getRestrictSearchableAttributes() {
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

    public @Nullable String getSnippetEllipsisText() {
        return get(KEY_SNIPPET_ELLIPSIS_TEXT);
    }

    private static final String KEY_SYNONYMS = "synonyms";

    /**
     * @param enabled If set to false, this query will not use synonyms defined in
     *                configuration. Defaults to true.
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

    public @Nullable JSONArray getTagFilters() {
        try {
            String value = get(KEY_TAG_FILTERS);
            if (value != null) {
                return new JSONArray(value);
            }
        } catch (JSONException e) {
            // Will return null
        }
        return null;
    }

    private static final String KEY_TYPO_TOLERANCE = "typoTolerance";

    public @NonNull Query setTypoTolerance(@Nullable TypoTolerance type) {
        if (type == null) {
            set(KEY_TYPO_TOLERANCE, null);
        } else {
            switch (type) {
                case FALSE:
                    set(KEY_TYPO_TOLERANCE, "false");
                    break;
                case MIN:
                    set(KEY_TYPO_TOLERANCE, "min");
                    break;
                case STRICT:
                    set(KEY_TYPO_TOLERANCE, "strict");
                    break;
                case TRUE:
                    set(KEY_TYPO_TOLERANCE, "true");
                    break;
            }
        }
        return this;
    }

    public @Nullable TypoTolerance getTypoTolerance() {
        String value = get(KEY_TYPO_TOLERANCE);
        if (value != null) {
            switch (value) {
                case "false":
                    return TypoTolerance.FALSE;
                case "min":
                    return TypoTolerance.MIN;
                case "strict":
                    return TypoTolerance.STRICT;
                case "true":
                    return TypoTolerance.TRUE;
            }
        }
        return null;
    }

    private static final String KEY_EXACT_ON_SINGLE_WORD_QUERY = "exactOnSingleWordQuery";

    public @NonNull Query setExactOnSingleWordQuery(@Nullable ExactOnSingleWordQuery type) {
        if (type == null) {
            set(KEY_EXACT_ON_SINGLE_WORD_QUERY, null);
        } else {
            switch (type) {
                case NONE:
                    set(KEY_EXACT_ON_SINGLE_WORD_QUERY, "none");
                    break;
                case WORD:
                    set(KEY_EXACT_ON_SINGLE_WORD_QUERY, "word");
                    break;
                case ATTRIBUTE:
                    set(KEY_EXACT_ON_SINGLE_WORD_QUERY, "attribute");
                    break;
            }
        }
        return this;
    }

    public @Nullable ExactOnSingleWordQuery getExactOnSingleWordQuery() {
        String value = get(KEY_EXACT_ON_SINGLE_WORD_QUERY);
        if (value != null) {
            switch (value) {
                case "none":
                    return ExactOnSingleWordQuery.NONE;
                case "word":
                    return ExactOnSingleWordQuery.WORD;
                case "attribute":
                    return ExactOnSingleWordQuery.ATTRIBUTE;
            }
        }
        return null;
    }

    private static final String KEY_ALTERNATIVES_AS_EXACT = "alternativesAsExact";

    public @NonNull Query setAlternativesAsExact(@Nullable AlternativesAsExact[] types) {
        if (types == null) {
            set(KEY_ALTERNATIVES_AS_EXACT, null);
        } else {
            List<String> stringList = new ArrayList<>(types.length);
            for (AlternativesAsExact type : types) {
                switch (type) {
                    case IGNORE_PLURALS:
                        stringList.add("ignorePlurals");
                        break;
                    case MULTI_WORDS_SYNONYM:
                        stringList.add("multiWordsSynonym");
                        break;
                    case SINGLE_WORD_SYNONYM:
                        stringList.add("singleWordSynonym");
                        break;
                }
            }
            String alternatives = TextUtils.join(",", stringList);
            set(KEY_ALTERNATIVES_AS_EXACT, alternatives);
        }
        return this;
    }

    public @Nullable AlternativesAsExact[] getAlternativesAsExact() {
        String alternativesStr = get(KEY_ALTERNATIVES_AS_EXACT);
        if (alternativesStr == null) {
            return null;
        }

        String[] stringList = TextUtils.split(alternativesStr, ",");
        AlternativesAsExact[] alternatives = new AlternativesAsExact[stringList.length];

        for (int i = 0, stringListLength = stringList.length; i < stringListLength; i++) {
            String alternative = stringList[i];
            switch (alternative) {
                case "ignorePlurals":
                    alternatives[i] = AlternativesAsExact.IGNORE_PLURALS;
                    break;
                case "multiWordsSynonym":
                    alternatives[i] = AlternativesAsExact.MULTI_WORDS_SYNONYM;
                    break;
                case "singleWordSynonym":
                    alternatives[i] = AlternativesAsExact.SINGLE_WORD_SYNONYM;
                    break;
            }
        }
        return alternatives;
    }

    private static final String KEY_RESPONSE_FIELDS = "responseFields";

    /**
     * Choose which fields the response will contain. Applies to search and browse queries.
     * <p>
     * By default, all fields are returned. If this parameter is specified, only the fields explicitly listed will be returned, unless * is used, in which case all fields are returned. Specifying an empty list or unknown field names is an error.
     */
    public @NonNull Query setResponseFields(String... attributes) {
        return set(KEY_RESPONSE_FIELDS, buildJSONArray(attributes));
    }

    /**
     * Get the fields the response will contain. If unspecified, all fields are returned.
     */
    public String[] getResponseFields() {
        return parseArray(get(KEY_RESPONSE_FIELDS));
    }
    // ----------------------------------------------------------------------
    // Parsing/serialization
    // ----------------------------------------------------------------------

    /**
     * Parse a query object from a URL query parameter string.
     *
     * @param queryParameters URL query parameter string.
     * @return The parsed query object.
     */
    protected static @NonNull Query parse(@NonNull String queryParameters) {
        Query query = new Query();
        query.parseFrom(queryParameters);
        return query;
    }

    // ----------------------------------------------------------------------
    // Low-level (untyped) accessors
    // ----------------------------------------------------------------------

    /**
     * Set a parameter in an untyped fashion.
     * This low-level accessor is intended to access parameters that this client does not yet support.
     *
     * @param name  The parameter's name.
     * @param value The parameter's value, or null to remove it.
     *              It will first be converted to a String by the `toString()` method.
     * @return This instance (used to chain calls).
     */
    @Override
    public @NonNull Query set(@NonNull String name, @Nullable Object value) {
        return (Query) super.set(name, value);
    }
}