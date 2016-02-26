package com.algolia.search.saas;

import android.support.annotation.NonNull;
import android.util.Pair;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        // / all query words are interpreted as prefixes.
        PREFIX_ALL,
        // / only the last word is interpreted as a prefix (default behavior).
        PREFIX_LAST,
        // / no query word is interpreted as a prefix. This option is not
        // recommended.
        PREFIX_NONE,
        // The parameter isn't set
        PREFIX_NOTSET
    }

    public enum RemoveWordsType {
        // / when a query does not return any result, the final word will be
        // removed until there is results. This option is particulary useful on
        // e-commerce websites
        REMOVE_LAST_WORDS,
        // / when a query does not return any result, the first word will be
        // removed until there is results. This option is useful on adress
        // search.
        REMOVE_FIRST_WORDS,
        // / No specific processing is done when a query does not return any
        // result.
        REMOVE_NONE,
        // / When a query does not return any result, a second trial will be
        // made with all words as optional (which is equivalent to transforming
        // the AND operand between query terms in a OR operand)
        REMOVE_ALLOPTIONAL,
        // The parameter isn't set
        REMOVE_NOTSET
    }

    public enum TypoTolerance {
        // / the typotolerance is enabled and all typos are retrieved. (Default
        // behavior)
        TYPO_TRUE,
        // / the typotolerance is disabled.
        TYPO_FALSE,
        // / only keep results with the minimum number of typos.
        TYPO_MIN,
        // / the typotolerance with a distance=2 is disabled if the results
        // contain hits without typo.
        TYPO_STRICT,
        // The parameter isn't set
        TYPO_NOTSET
    }

    /** Extra query parameters, as un untyped key-value array. */
    // NOTE: Using a tree map to have parameters sorted by key on output.
    private Map<String, String> parameters = new TreeMap<>();

    protected List<String> attributes;
    protected List<String> attributesToHighlight;
    protected List<String> attributesToSnippet;
    protected List<String> disableTypoToleranceOn;
    protected Integer minWordSizeForApprox1;
    protected Integer minWordSizeForApprox2;
    protected Boolean getRankingInfo;
    protected Boolean ignorePlural;
    protected Integer distinct;
    protected Boolean advancedSyntax;
    protected Boolean removeStopWords;
    protected Integer page;
    protected Integer hitsPerPage;
    protected String restrictSearchableAttributes;
    protected String tags;
    protected String highlightPreTag;
    protected String highlightPostTag;
    protected String snippetEllipsisText;
    protected Integer minProximity;
    protected String numerics;
    protected String insideBoundingBox;
    protected String insidePolygon;
    protected String aroundLatLong;
    protected Boolean aroundLatLongViaIP;
    protected String query;
    protected String similarQuery;
    protected QueryType queryType;
    protected String optionalWords;
    protected String facets;
    protected String filters;
    protected String facetFilters;
    protected Integer maxNumberOfFacets;
    protected Boolean analytics;
    protected Boolean synonyms;
    protected Boolean replaceSynonyms;
    protected Boolean allowTyposOnNumericTokens;
    protected RemoveWordsType removeWordsIfNoResult;
    protected TypoTolerance typoTolerance;
    protected String analyticsTags;
    protected int aroundPrecision;
    protected int aroundRadius;

    public Query(String query) {
        minWordSizeForApprox1 = null;
        minWordSizeForApprox2 = null;
        getRankingInfo = null;
        ignorePlural = null;
        distinct = null;
        page = null;
        minProximity = null;
        hitsPerPage = null;
        this.query = query;
	similarQuery = null;
        queryType = QueryType.PREFIX_NOTSET;
        maxNumberOfFacets = null;
        advancedSyntax = null;
	removeStopWords = null;
        analytics = synonyms = replaceSynonyms = allowTyposOnNumericTokens = null;
        analyticsTags = null;
        typoTolerance = TypoTolerance.TYPO_NOTSET;
        removeWordsIfNoResult = RemoveWordsType.REMOVE_NOTSET;
        aroundPrecision = aroundRadius = 0;
    }

    public Query() {
        this((String) null);
    }

    public Query(Query other) {
        if (other.attributesToHighlight != null) {
            attributesToHighlight = new ArrayList<String>(other.attributesToHighlight);
        }
        if (other.attributes != null) {
            attributes = new ArrayList<String>(other.attributes);
        }
        if (other.attributesToSnippet != null) {
            attributesToSnippet = new ArrayList<String>(other.attributesToSnippet);
        }
        if (other.disableTypoToleranceOn != null) {
            disableTypoToleranceOn = new ArrayList<String>(other.disableTypoToleranceOn);
        }
        minWordSizeForApprox1 = other.minWordSizeForApprox1;
        minWordSizeForApprox2 = other.minWordSizeForApprox2;
        getRankingInfo = other.getRankingInfo;
        ignorePlural = other.ignorePlural;
        minProximity = other.minProximity;
        highlightPreTag = other.highlightPreTag;
        highlightPostTag = other.highlightPostTag;
        snippetEllipsisText = other.snippetEllipsisText;
        distinct = other.distinct;
        advancedSyntax = other.advancedSyntax;
	removeStopWords = other.removeStopWords;
        page = other.page;
        hitsPerPage = other.hitsPerPage;
        restrictSearchableAttributes = other.restrictSearchableAttributes;
        tags = other.tags;
        numerics = other.numerics;
        insideBoundingBox = other.insideBoundingBox;
        aroundLatLong = other.aroundLatLong;
        aroundLatLongViaIP = other.aroundLatLongViaIP;
        query = other.query;
	similarQuery = other.similarQuery;
        queryType = other.queryType;
        optionalWords = other.optionalWords;
        facets = other.facets;
        filters = other.filters;
        facetFilters = other.facetFilters;
        maxNumberOfFacets = other.maxNumberOfFacets;
        analytics = other.analytics;
        analyticsTags = other.analyticsTags;
        synonyms = other.synonyms;
        replaceSynonyms = other.replaceSynonyms;
        typoTolerance = other.typoTolerance;
        allowTyposOnNumericTokens = other.allowTyposOnNumericTokens;
        removeWordsIfNoResult = other.removeWordsIfNoResult;
        aroundPrecision = other.aroundPrecision;
        aroundRadius = other.aroundRadius;
        insidePolygon = other.insidePolygon;
        parameters = new TreeMap<>(other.parameters);
    }

    @Override public String toString() {
        return String.format("%s{%s}", this.getClass().getSimpleName(), this.build());
    }

    /**
     * Select the strategy to adopt when a query does not return any result.
     */
    public Query removeWordsIfNoResult(RemoveWordsType type) {
        this.removeWordsIfNoResult = type;
        return this;
    }

    /**
     * List of object attributes you want to use for textual search (must be a
     * subset of the attributesToIndex index setting). Attributes are separated
     * with a comma (for example @"name,address"). You can also use a JSON
     * string array encoding (for example
     * encodeURIComponent("[\"name\",\"address\"]")). By default, all attributes
     * specified in attributesToIndex settings are used to search.
     */
    public Query restrictSearchableAttributes(String attributes) {
        this.restrictSearchableAttributes = attributes;
        return this;
    }

    /**
     * Select how the query words are interpreted:
     */
    public Query setQueryType(QueryType type) {
        this.queryType = type;
        return this;
    }

    /**
     * Set the full text query
     */
    public Query setQueryString(String query) {
        this.query = query;
        return this;
    }

    /**
     * Set the full text similar query
     */
    public Query setSimilarQueryString(String query) {
        this.similarQuery = query;
        return this;
    }

    /**
     * Specify the list of attribute names to retrieve. By default all
     * attributes are retrieved.
     */
    public Query setAttributesToRetrieve(List<String> attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * Specify the list of attribute names to highlight. By default indexed
     * attributes are highlighted.
     */
    public Query setAttributesToHighlight(List<String> attributes) {
        this.attributesToHighlight = attributes;
        return this;
    }

    /**
     * Specify the list of attribute names to Snippet alongside the number of
     * words to return (syntax is 'attributeName:nbWords'). By default no
     * snippet is computed.
     */
    public Query setAttributesToSnippet(List<String> attributes) {
        this.attributesToSnippet = attributes;
        return this;
    }

    /**
     *
     * @param distinct
     *            If set to true, enable the distinct feature (disabled by default)
     *            if the attributeForDistinct index setting is set. This feature
     *            is similar to the SQL "distinct" keyword: when enabled in a
     *            query with the distinct=1 parameter, all hits containing a
     *            duplicate value for the attributeForDistinct attribute are
     *            removed from results. For example, if the chosen attribute is
     *            show_name and several hits have the same value for show_name,
     *            then only the best one is kept and others are removed.
     */
    public Query enableDistinct(boolean distinct) {
        this.distinct = distinct ? 1 : 0;
        return this;
    }

    /**
     * This feature is similar to the distinct just before but instead of
     * keeping the best value per value of attributeForDistinct, it allows to
     * keep N values.
     *
     * @param nbHitsToKeep
     *            Specify the maximum number of hits to keep for each distinct
     *            value
     */
    public Query enableDistinct(int nbHitsToKeep) {
        this.distinct = nbHitsToKeep;
        return this;
    }

    /**
     * @param enabled
     *            If set to false, this query will not be taken into account in
     *            analytics feature. Default to true.
     */
    public Query enableAnalytics(boolean enabled) {
        this.analytics = enabled;
        return this;
    }

    /**
     * @param analyticsTags
     *            Set the analytics tags identifying the query
     */
    public Query setAnalyticsTags(String analyticsTags) {
        this.analyticsTags = analyticsTags;
        return this;
    }

    /**
     * @param enabled
     *            If set to false, this query will not use synonyms defined in
     *            configuration. Default to true.
     */
    public Query enableSynonyms(boolean enabled) {
        this.synonyms = enabled;
        return this;
    }

    /**
     * @param enabled
     *            If set to false, words matched via synonyms expansion will not be
     *            replaced by the matched synonym in highlight result. Default
     *            to true.
     */
    public Query enableReplaceSynonymsInHighlight(boolean enabled) {
        this.replaceSynonyms = enabled;
        return this;
    }

    /**
     * @param enabled
     *            If set to false, disable typo-tolerance. Default to true.
     */
    public Query enableTypoTolerance(boolean enabled) {
        if (enabled) {
            this.typoTolerance = TypoTolerance.TYPO_TRUE;
        } else {
            this.typoTolerance = TypoTolerance.TYPO_FALSE;
        }
        return this;
    }

    /**
     * List of attributes on which you want to disable typo tolerance (must be a subset of the attributesToIndex index setting).
     */
    public Query disableTypoToleranceOnAttributes(List<String> attributes) {
        this.disableTypoToleranceOn = attributes;
        return this;
    }

    /**
     * @param typoTolerance
     *            This option allow to control the number of typo in the results set.
     */
    public Query setTypoTolerance(TypoTolerance typoTolerance) {
        this.typoTolerance = typoTolerance;
        return this;
    }

    /**
     * Specify the minimum number of characters in a query word to accept one
     * typo in this word. Defaults to 3.
     */
    public Query setMinWordSizeToAllowOneTypo(int nbChars) {
        minWordSizeForApprox1 = nbChars;
        return this;
    }

    /*
     * Configure the precision of the proximity ranking criterion. By default,
     * the minimum (and best) proximity value distance between 2 matching words
     * is 1. Setting it to 2 (or 3) would allow 1 (or 2) words to be found
     * between the matching words without degrading the proximity ranking value.
     *
     * Considering the query "javascript framework", if you set minProximity=2
     * the records "JavaScript framework" and "JavaScript charting framework"
     * will get the same proximity score, even if the second one contains a word
     * between the 2 matching words. Default to 1.
     */
    public Query setMinProximity(int value) {
        this.minProximity = value;
        return this;
    }

    /*
     * Specify the string that is inserted before/after the highlighted parts in
     * the query result (default to "<em>" / "</em>").
     */
    public Query setHighlightingTags(String preTag, String postTag) {
        this.highlightPreTag = preTag;
        this.highlightPostTag = postTag;
        return this;
    }

    /**
     * Specify the string that is used as an ellipsis indicator when a snippet
     * is truncated (defaults to the empty string).
     */
    public Query setSnippetEllipsisText(String snippetEllipsisText) {
        this.snippetEllipsisText = snippetEllipsisText;
        return this;
    }

    /**
     * Specify the minimum number of characters in a query word to accept two
     * typos in this word. Defaults to 7.
     */
    public Query setMinWordSizeToAllowTwoTypos(int nbChars) {
        minWordSizeForApprox2 = nbChars;
        return this;
    }

    /**
     * @param enabled
     *            If set to false, disable typo-tolerance on numeric tokens.
     *            Default to true.
     */
    public Query enableTyposOnNumericTokens(boolean enabled) {
        this.allowTyposOnNumericTokens = enabled;
        return this;
    }

    /**
     * if set, the result hits will contain ranking information in _rankingInfo
     * attribute.
     */
    public Query getRankingInfo(boolean enabled) {
        getRankingInfo = enabled;
        return this;
    }

    /**
     * If set to true, plural won't be considered as a typo (for example
     * car/cars will be considered as equals). Default to false.
     */
    public Query ignorePlural(boolean enabled) {
        ignorePlural = enabled;
        return this;
    }

    /**
     * Set the page to retrieve (zero base). Defaults to 0.
     */
    public Query setPage(int page) {
        this.page = page;
        return this;
    }

    /**
     * Set the number of hits per page. Defaults to 10.
     */
    public Query setHitsPerPage(int nbHitsPerPage) {
        this.hitsPerPage = nbHitsPerPage;
        return this;
    }

    /**
     * Change the radius or around latitude/longitude query
     */
    public Query setAroundRadius(int radius) {
        aroundRadius = radius;
        return this;
    }

    /**
     * Change the precision or around latitude/longitude query
     */
    public Query setAroundPrecision(int precision) {
        aroundPrecision = precision;
        return this;
    }

    /**
     * Set the number of hits per page. Defaults to 10.
     *
     * @deprecated Use {@code setHitsPerPage}
     */
    @Deprecated
    public Query setNbHitsPerPage(int nbHitsPerPage) {
        return setHitsPerPage(nbHitsPerPage);
    }


    /**
     * Search for entries around a given latitude/longitude.
     *
     */
    public Query aroundLatitudeLongitude(float latitude, float longitude) {
        aroundLatLong = "aroundLatLng=" + latitude + "," + longitude;
        return this;
    }

    /**
     * Search for entries around a given latitude/longitude.
     *
     * @param radius
     *            set the maximum distance in meters. Note: at indexing, geoloc
     *            of an object should be set with _geoloc attribute containing
     *            lat and lng attributes (for example
     *            {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     */
    @Deprecated
    public Query aroundLatitudeLongitude(float latitude, float longitude, int radius) {
        aroundLatLong = "aroundLatLng=" + latitude + "," + longitude + "&aroundRadius=" + radius;
        return this;
    }

    /**
     * Search for entries around a given latitude/longitude.
     *
     * @param radius
     *            set the maximum distance in meters.
     * @param precision
     *            set the precision for ranking (for example if you set
     *            precision=100, two objects that are distant of less than 100m
     *            will be considered as identical for "geo" ranking parameter).
     *            Note: at indexing, geoloc of an object should be set with
     *            _geoloc attribute containing lat and lng attributes (for
     *            example {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     */
    @Deprecated
    public Query aroundLatitudeLongitude(float latitude, float longitude, int radius, int precision) {
        aroundLatLong = "aroundLatLng=" + latitude + "," + longitude + "&aroundRadius=" + radius + "&aroundPrecision=" + precision;
        return this;
    }

    /**
     * Search for entries around the latitude/longitude of user (using IP
     * geolocation)
     *
     */
    public Query aroundLatitudeLongitudeViaIP(boolean enabled) {
        aroundLatLongViaIP = enabled;
        return this;
    }

    /**
     * Search for entries around the latitude/longitude of user (using IP
     * geolocation)
     *
     * @param radius
     *            set the maximum distance in meters. Note: at indexing, geoloc
     *            of an object should be set with _geoloc attribute containing
     *            lat and lng attributes (for example
     *            {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     */
    @Deprecated
    public Query aroundLatitudeLongitudeViaIP(boolean enabled, int radius) {
        aroundLatLong = "aroundRadius=" + radius;
        aroundLatLongViaIP = enabled;
        return this;
    }

    /**
     * Search for entries around the latitude/longitude of user (using IP
     * geolocation)
     *
     * @param radius
     *            set the maximum distance in meters.
     * @param precision
     *            set the precision for ranking (for example if you set
     *            precision=100, two objects that are distant of less than 100m
     *            will be considered as identical for "geo" ranking parameter).
     *            Note: at indexing, geoloc of an object should be set with
     *            _geoloc attribute containing lat and lng attributes (for
     *            example {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     */
    @Deprecated
    public Query aroundLatitudeLongitudeViaIP(boolean enabled, int radius, int precision) {
        aroundLatLong = "aroundRadius=" + radius + "&aroundPrecision=" + precision;
        aroundLatLongViaIP = enabled;
        return this;
    }

    /**
     * Search for entries inside a given area defined by the two extreme points
     * of a rectangle. At indexing, geoloc of an object should be set with
     * _geoloc attribute containing lat and lng attributes (for example
     * {"_geoloc":{"lat":48.853409, "lng":2.348800}})
     *
     * You can use several bounding boxes (OR) by calling this method several times.
     */
    public Query insideBoundingBox(float latitudeP1, float longitudeP1, float latitudeP2, float longitudeP2) {
        if (insideBoundingBox == null) {
            insideBoundingBox = "insideBoundingBox=" + latitudeP1 + "," + longitudeP1 + "," + latitudeP2 + "," + longitudeP2;
        } else {
            insideBoundingBox += "," + latitudeP1 + "," + longitudeP1 + "," + latitudeP2 + "," + longitudeP2;
        }
        return this;
    }

    /**
     * Add a point to the polygon of geo-search (requires a minimum of three points to define a valid polygon)
     * At indexing, you should specify geoloc of an object with the _geoloc attribute (in the form "_geoloc":{"lat":48.853409, "lng":2.348800} or
     * "_geoloc":[{"lat":48.853409, "lng":2.348800},{"lat":48.547456, "lng":2.972075}] if you have several geo-locations in your record).
     */
    public Query insidePolygon(List<Pair<Float, Float>> points) {
        insidePolygon = "insidePolygon=";
        boolean first = true;
        for (Pair<Float, Float> p : points) {
            if (!first) {
                insidePolygon += ",";
            }
            insidePolygon += p.first + "," + p.second;
            first = true;
        }
        return this;
    }

    /**
     * Set the list of words that should be considered as optional when found in
     * the query.
     *
     * @param words
     *            The list of optional words, comma separated.
     */
    public Query setOptionalWords(String words) {
        this.optionalWords = words;
        return this;
    }

    /**
     * Set the list of words that should be considered as optional when found in
     * the query.
     *
     * @param words
     *            The list of optional words.
     */
    public Query setOptionalWords(List<String> words) {
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(word);
            builder.append(",");
        }
        this.optionalWords = builder.toString();
        return this;
    }

    /**
	 * Filter the query with numeric, facet or/and tag filters.
	 * The syntax is a SQL like syntax, you can use the OR and AND keywords.
	 * The syntax for the underlying numeric, facet and tag filters is the same than in the other filters:
	 * available=1 AND (category:Book OR NOT category:Ebook) AND public
     * date: 1441745506 TO 1441755506 AND inStock &gt; 0 AND author:"John Doe"
     * The list of keywords is:
     * OR: create a disjunctive filter between two filters.
     * AND: create a conjunctive filter between two filters.
     * TO: used to specify a range for a numeric filter.
     * NOT: used to negate a filter. The syntax with the ‘-‘ isn’t allowed.
	 */
	public Query setFilters(String filters) {
		this.filters = filters;
        return this;
	}

    /**
     * Filter the query by a list of facets. Each filter is encoded as
     * `attributeName:value`.
     */
    public Query setFacetFilters(List<String> facets) {
        JSONArray obj = new JSONArray();
        for (String facet : facets) {
            obj.put(facet);
        }
        this.facetFilters = obj.toString();
        return this;
    }

    /**
     * Filter the query by a list of facets. Filters are separated by commas and
     * each facet is encoded as `attributeName:value`. To OR facets, you must
     * add parentheses. For example:
     * `(category:Book,category:Movie),author:John%20Doe`. You can also use a
     * JSON string array encoding, for example
     * `[[\"category:Book\",\"category:Movie\"],\"author:John Doe\"]`.
     */
    public Query setFacetFilters(String facetFilters) {
        this.facetFilters = facetFilters;
        return this;
    }

    /**
     * List of object attributes that you want to use for faceting.
     * Only attributes that have been added in **attributesForFaceting** index
     * setting can be used in this parameter. You can also use `*` to perform
     * faceting on all attributes specified in **attributesForFaceting**.
     */
    public Query setFacets(List<String> facets) {
        JSONArray obj = new JSONArray();
        for (String facet : facets) {
            obj.put(facet);
        }
        this.facets = obj.toString();
        return this;
    }

    /**
     * Limit the number of facet values returned for each facet.
     */
    public Query setMaxNumberOfFacets(int n) {
        this.maxNumberOfFacets = n;
        return this;
    }

    /**
     * Filter the query by a set of tags. You can AND tags by separating them by
     * commas. To OR tags, you must add parentheses. For example
     * tag1,(tag2,tag3) means tag1 AND (tag2 OR tag3). At indexing, tags should
     * be added in the _tags attribute of objects (for example
     * {"_tags":["tag1","tag2"]} )
     */
    public Query setTagFilters(String tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Add a list of numeric filters separated by a comma. The syntax of one
     * filter is `attributeName` followed by `operand` followed by `value.
     * Supported operands are `&lt;`, `&lt;=`, `=`, `&gt;` and `&lt;=`. You can have
     * multiple conditions on one attribute like for example
     * `numerics=price&gt;100,price&lt;1000`.
     */
    public Query setNumericFilters(String numerics) {
        this.numerics = numerics;
        return this;
    }

    /**
     * Add a list of numeric filters separated by a comma. The syntax of one
     * filter is `attributeName` followed by `operand` followed by `value.
     * Supported operands are `&lt;`, `&lt;=`, `=`, `&gt;` and `&lt;=`. You can have
     * multiple conditions on one attribute like for example
     * `numerics=price&gt;100,price&lt;1000`.
     */
    public Query setNumericFilters(List<String> numerics) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String n : numerics) {
            if (!first)
                builder.append(",");
            builder.append(n);
            first = false;
        }
        this.numerics = builder.toString();
        return this;
    }

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
    public Query enableAvancedSyntax(boolean advancedSyntax) {
        this.advancedSyntax = advancedSyntax;
        return this;
    }

    /**
     * Enable the removal of stop words. Defaults to false.
     */
    public Query enableRemoveStopWords(boolean removeStopWords) {
        this.removeStopWords = removeStopWords;
        return this;
    }

    /**
     * Build the URL query parameter string representing this object.
     * @return A string suitable for use inside the query part of a URL (i.e. after the question mark).
     */
    protected @NonNull String build() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Known parameters.
            if (attributes != null) {
                stringBuilder.append("attributesToRetrieve=");
                boolean first = true;
                for (String attr : this.attributes) {
                    if (!first)
                        stringBuilder.append(",");
                    stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
                    first = false;
                }
            }
            if (attributesToHighlight != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("attributesToHighlight=");
                boolean first = true;
                for (String attr : this.attributesToHighlight) {
                    if (!first)
                        stringBuilder.append(',');
                    stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
                    first = false;
                }
            }
            if (attributesToSnippet != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("attributesToSnippet=");
                boolean first = true;
                for (String attr : this.attributesToSnippet) {
                    if (!first)
                        stringBuilder.append(',');
                    stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
                    first = false;
                }
            }
            if (disableTypoToleranceOn != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("disableTypoToleranceOnAttributes=");
                boolean first = true;
                for (String attr : this.disableTypoToleranceOn) {
                    if (!first)
                        stringBuilder.append(',');
                    stringBuilder.append(URLEncoder.encode(attr, "UTF-8"));
                    first = false;
                }
            }
            if (typoTolerance != TypoTolerance.TYPO_NOTSET) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("typoTolerance=");
                switch (typoTolerance) {
                    case TYPO_FALSE:
                        stringBuilder.append("false");
                        break;
                    case TYPO_MIN:
                        stringBuilder.append("min");
                        break;
                    case TYPO_STRICT:
                        stringBuilder.append("strict");
                        break;
                    case TYPO_TRUE:
                        stringBuilder.append("true");
                        break;
                    case TYPO_NOTSET:
                        throw new IllegalStateException("code not reachable");
                }
            }
            if (allowTyposOnNumericTokens != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("allowTyposOnNumericTokens=").append(allowTyposOnNumericTokens ? '1' : '0');
            }
            if (minWordSizeForApprox1 != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("minWordSizefor1Typo=");
                stringBuilder.append(minWordSizeForApprox1);
            }
            if (minWordSizeForApprox2 != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("minWordSizefor2Typos=");
                stringBuilder.append(minWordSizeForApprox2);
            }
            switch (removeWordsIfNoResult) {
                case REMOVE_LAST_WORDS:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("removeWordsIfNoResult=LastWords");
                    break;
                case REMOVE_FIRST_WORDS:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("removeWordsIfNoResult=FirstWords");
                    break;
                case REMOVE_ALLOPTIONAL:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("removeWordsIfNoResult=allOptional");
                    break;
                case REMOVE_NONE:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("removeWordsIfNoResult=none");
                    break;
                case REMOVE_NOTSET:
                    // Nothing to do
                    break;
            }
            if (getRankingInfo != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("getRankingInfo=").append(getRankingInfo ? '1' : '0');
            }
            if (ignorePlural != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("ignorePlural=").append(ignorePlural ? '1' : '0');
            }
            if (analytics != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("analytics=").append(analytics ? '1' : '0');
            }
            if (analyticsTags != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("analyticsTags=");
                stringBuilder.append(URLEncoder.encode(analyticsTags, "UTF-8"));
            }
            if (synonyms != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("synonyms=").append(synonyms ? '1' : '0');
            }
            if (replaceSynonyms != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("replaceSynonymsInHighlight=").append(replaceSynonyms ? '1' : '0');
            }
            if (distinct != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("distinct=");
                stringBuilder.append(distinct);
            }
            if (advancedSyntax != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("advancedSyntax=").append(advancedSyntax ? '1' : '0');
            }
            if (removeStopWords != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("removeStopWords=").append(removeStopWords ? '1' : '0');
            }
            if (page != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("page=");
                stringBuilder.append(page);
            }
            if (minProximity != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("minProximity=");
                stringBuilder.append(minProximity);
            }
            if (highlightPreTag != null && highlightPostTag != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("highlightPreTag=");
                stringBuilder.append(URLEncoder.encode(highlightPreTag, "UTF-8"));
                stringBuilder.append("&highlightPostTag=");
                stringBuilder.append(URLEncoder.encode(highlightPostTag, "UTF-8"));
            }
            if (snippetEllipsisText != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("snippetEllipsisText=");
                stringBuilder.append(URLEncoder.encode(snippetEllipsisText, "UTF-8"));
            }
            if (hitsPerPage != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("hitsPerPage=");
                stringBuilder.append(hitsPerPage);
            }
            if (tags != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("tagFilters=");
                stringBuilder.append(URLEncoder.encode(tags, "UTF-8"));
            }
            if (numerics != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("numericFilters=");
                stringBuilder.append(URLEncoder.encode(numerics, "UTF-8"));
            }
            if (insideBoundingBox != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(insideBoundingBox);
            }
            if (aroundLatLong != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(aroundLatLong);
            }
            if (insidePolygon != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(insidePolygon);
            }
            if (aroundRadius > 0) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("aroundRadius=");
                stringBuilder.append(aroundRadius);
            }
            if (aroundPrecision > 0) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("aroundPrecision=");
                stringBuilder.append(aroundPrecision);
            }
            if (aroundLatLongViaIP != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("aroundLatLngViaIP=").append(aroundLatLongViaIP ? '1' : '0');
            }
            if (query != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("query=");
                stringBuilder.append(URLEncoder.encode(query, "UTF-8"));
            }
            if (similarQuery != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("similarQuery=");
                stringBuilder.append(URLEncoder.encode(similarQuery, "UTF-8"));
            }
            if (facets != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("facets=");
                stringBuilder.append(URLEncoder.encode(facets, "UTF-8"));
            }
            if (filters != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("filters=");
                stringBuilder.append(URLEncoder.encode(filters, "UTF-8"));
            }
            if (facetFilters != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("facetFilters=");
                stringBuilder.append(URLEncoder.encode(facetFilters, "UTF-8"));
            }
            if (maxNumberOfFacets != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("maxNumberOfFacets=");
                stringBuilder.append(maxNumberOfFacets);
            }
            if (optionalWords != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("optionalWords=");
                stringBuilder.append(URLEncoder.encode(optionalWords, "UTF-8"));
            }
            if (restrictSearchableAttributes != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append("restrictSearchableAttributes=");
                stringBuilder.append(URLEncoder.encode(restrictSearchableAttributes, "UTF-8"));
            }
            switch (queryType) {
                case PREFIX_ALL:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("queryType=prefixAll");
                    break;
                case PREFIX_LAST:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("queryType=prefixLast");
                    break;
                case PREFIX_NONE:
                    if (stringBuilder.length() > 0)
                        stringBuilder.append('&');
                    stringBuilder.append("queryType=prefixNone");
                    break;
            }
            // Unknown parameters.
            // NOTE: They may shadow previous values; this is wanted (see the `set()` method).
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                String value = entry.getValue();
                if (value != null) {
                    stringBuilder.append('=');
                    stringBuilder.append(URLEncoder.encode(value, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return stringBuilder.toString();
    }

    /**
     * @return the attributes
     */
    public List<String> getAttributes() {
        return attributes;
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

                // Known parameters: parse to typed field.
                if ((name.equals("attributesToRetrieve") || name.equals("attributes") /* legacy name */) && value != null) {
                    query.attributes = Arrays.asList(value.split(","));
                } else if (name.equals("attributesToHighlight") && value != null) {
                    query.attributesToHighlight = Arrays.asList(value.split(","));
                } else if (name.equals("attributesToSnippet") && value != null) {
                    query.attributesToSnippet = Arrays.asList(value.split(","));
                } else if (name.equals("disableTypoToleranceOnAttributes") && value != null) {
                    query.disableTypoToleranceOn = Arrays.asList(value.split(","));
                } else if (name.equals("typoTolerance") && value != null) {
                    if (value.equals("false")) {
                        query.typoTolerance = TypoTolerance.TYPO_FALSE;
                    } else if (value.equals("min")) {
                        query.typoTolerance = TypoTolerance.TYPO_MIN;
                    } else if (value.equals("strict")) {
                        query.typoTolerance = TypoTolerance.TYPO_STRICT;
                    } else if (value.equals("true")) {
                        query.typoTolerance = TypoTolerance.TYPO_TRUE;
                    }
                } else if (name.equals("allowTyposOnNumericTokens") && value != null) {
                    query.allowTyposOnNumericTokens = parseBoolean(value);
                } else if (name.equals("minWordSizefor1Typo") && value != null) {
                    query.minWordSizeForApprox1 = parseInt(value);
                } else if (name.equals("minWordSizefor2Typos") && value != null) {
                    query.minWordSizeForApprox2 = parseInt(value);
                } else if (name.equals("removeWordsIfNoResult") && value != null) {
                    if (value.equals("LastWords")) {
                        query.removeWordsIfNoResult = RemoveWordsType.REMOVE_LAST_WORDS;
                    } else if (value.equals("FirstWords")) {
                        query.removeWordsIfNoResult = RemoveWordsType.REMOVE_FIRST_WORDS;
                    } else if (value.equals("allOptional")) {
                        query.removeWordsIfNoResult = RemoveWordsType.REMOVE_ALLOPTIONAL;
                    } else if (value.equals("none")) {
                        query.removeWordsIfNoResult = RemoveWordsType.REMOVE_NONE;
                    }
                } else if (name.equals("getRankingInfo") && value != null) {
                    query.getRankingInfo = parseBoolean(value);
                } else if (name.equals("ignorePlural") && value != null) {
                    query.ignorePlural = parseBoolean(value);
                } else if (name.equals("analytics") && value != null) {
                    query.analytics = parseBoolean(value);
                } else if (name.equals("analyticsTags") && value != null) {
                    query.analyticsTags = value;
                } else if (name.equals("synonyms") && value != null) {
                    query.synonyms = parseBoolean(value);
                } else if (name.equals("replaceSynonymsInHighlight") && value != null) {
                    query.replaceSynonyms = parseBoolean(value);
                } else if (name.equals("distinct") && value != null) {
                    query.distinct = parseInt(value);
                } else if (name.equals("advancedSyntax") && value != null) {
                    query.advancedSyntax = parseBoolean(value);
                } else if (name.equals("removeStopWords") && value != null) {
                    query.removeStopWords = parseBoolean(value);
                } else if (name.equals("page") && value != null) {
                    query.page = parseInt(value);
                } else if (name.equals("minProximity") && value != null) {
                    query.minProximity = parseInt(value);
                } else if (name.equals("highlightPreTag") && value != null) {
                    query.highlightPreTag = value;
                } else if (name.equals("highlightPostTag") && value != null) {
                    query.highlightPostTag = value;
                } else if (name.equals("snippetEllipsisText") && value != null) {
                    query.snippetEllipsisText = value;
                } else if (name.equals("hitsPerPage") && value != null) {
                    query.hitsPerPage = parseInt(value);
                } else if (name.equals("tagFilters") && value != null) {
                    query.tags = value;
                } else if (name.equals("numericFilters") && value != null) {
                    query.numerics = value;
                } else if (name.equals("insideBoundingBox") && value != null) {
                    query.insideBoundingBox = value;
                } else if (name.equals("aroundLatLng") && value != null) {
                    query.aroundLatLong = value;
                } else if (name.equals("insidePolygon") && value != null) {
                    query.insidePolygon = value;
                } else if (name.equals("aroundRadius") && value != null) {
                    query.aroundRadius = parseInt(value);
                } else if (name.equals("aroundPrecision") && value != null) {
                    query.aroundPrecision = parseInt(value);
                } else if (name.equals("aroundLatLngViaIP") && value != null) {
                    query.aroundLatLongViaIP = parseBoolean(value);
                } else if (name.equals("query") && value != null) {
                    query.query = value;
                } else if (name.equals("similarQuery") && value != null) {
                    query.similarQuery = value;
                } else if (name.equals("facets") && value != null) {
                    query.facets = value;
                } else if (name.equals("filters") && value != null) {
                    query.filters = value;
                } else if (name.equals("facetFilters") && value != null) {
                    query.facetFilters = value;
                } else if (name.equals("maxNumberOfFacets") && value != null) {
                    query.maxNumberOfFacets = parseInt(value);
                } else if (name.equals("optionalWords") && value != null) {
                    query.optionalWords = value;
                } else if (name.equals("restrictSearchableAttributes") && value != null) {
                    query.restrictSearchableAttributes = value;
                } else if (name.equals("queryType") && value != null) {
                    if (value.equals("prefixAll")) {
                        query.queryType = QueryType.PREFIX_ALL;
                    } else if (value.equals("prefixLast")) {
                        query.queryType = QueryType.PREFIX_LAST;
                    } else if (value.equals("prefixNone")) {
                        query.queryType = QueryType.PREFIX_NONE;
                    }
                }
                // Unknown parameter: add it to the "extra" map.
                else {
                    if (value != null) {
                        query.parameters.put(name, value);
                    }
                }
            } // for each parameter
            return query;
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF-8 is one of the default encodings.
            throw new RuntimeException(e);
        }
    }

    private static boolean parseBoolean(String value) {
        return value.trim().toLowerCase().equals("true") || parseInt(value) != 0;
    }

    private static int parseInt(String value)  {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Set an extra (untyped) parameter.
     * This low-level accessor is intended to access parameters that this client does not yet support.
     * WARNING: Any parameter specified here will shadow any typed parameter with the same name.
     * @param name The parameter's name.
     * @param value The parameter's value, or null to remove it.
     *              It will first be converted to a String by the `toString()` method.
     */
    public void set(@NonNull String name, Object value) {
        if (value == null) {
            parameters.remove(name);
        } else {
            parameters.put(name, value.toString());
        }
    }

    /**
     * Get an extra (untyped) parameter.
     * @param name The parameter's name.
     * @return The parameter's value, or null if a parameter with the specified name does not exist.
     */
    public String get(@NonNull String name) {
        return parameters.get(name);
    }

    public List<String> getAttributesToHighlight() {
        return attributesToHighlight;
    }

    /**
     * @return the attributesToSnippet
     */
    public List<String> getAttributesToSnippet() {
        return attributesToSnippet;
    }

    /**
     * @return the minWordSizeForApprox1
     */
    public Integer getMinWordSizeForApprox1() {
        return minWordSizeForApprox1;
    }

    /**
     * @return the minWordSizeForApprox2
     */
    public Integer getMinWordSizeForApprox2() {
        return minWordSizeForApprox2;
    }

    /**
     * @return the getRankingInfo
     */
    public Boolean isGetRankingInfo() {
        return getRankingInfo;
    }

    /**
     * @return the ignorePlural
     */
    public Boolean isIgnorePlural() {
        return ignorePlural;
    }

    /**
     * @return the distinct
     */
    public Boolean isDistinct() {
        return distinct > 0;
    }

    /**
     * @return the distinct
     */
    public Integer getDistinct() {
        return distinct;
    }

    /**
     * @return the advancedSyntax
     */
    public Boolean isAdvancedSyntax() {
        return advancedSyntax;
    }

    /**
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * @return the hitsPerPage
     */
    public Integer getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * @return the restrictSearchableAttributes
     */
    public String getRestrictSearchableAttributes() {
        return restrictSearchableAttributes;
    }

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * @return the numerics
     */
    public String getNumerics() {
        return numerics;
    }

    /**
     * @return the insideBoundingBox
     */
    public String getInsideBoundingBox() {
        return insideBoundingBox;
    }

    /**
     * @return the aroundLatLong
     */
    public String getAroundLatLong() {
        return aroundLatLong;
    }

    /**
     * @return the aroundLatLongViaIP
     */
    public Boolean isAroundLatLongViaIP() {
        return aroundLatLongViaIP;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @return the queryType
     */
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * @return the optionalWords
     */
    public String getOptionalWords() {
        return optionalWords;
    }

    /**
     * @return the facets
     */
    public String getFacets() {
        return facets;
    }

    /**
     * @return the filters
     */
    public  String getFilters() { return filters; }

    /**
     * @return the facetFilters
     */
    public String getFacetFilters() {
        return facetFilters;
    }

    /**
     * @return the maxNumberOfFacets
     */
    public Integer getMaxNumberOfFacets() {
        return maxNumberOfFacets;
    }

    /**
     * @return the analytics
     */
    public Boolean isAnalytics() {
        return analytics;
    }

    /**
     * @return the analytics tags
     */
    public String getAnalyticsTags() {
        return analyticsTags;
    }

    /**
     * @return the synonyms
     */
    public Boolean isSynonyms() {
        return synonyms;
    }

    /**
     * @return the replaceSynonyms
     */
    public Boolean isReplaceSynonyms() {
        return replaceSynonyms;
    }

    /**
     * @return the allowTyposOnNumericTokens
     */
    public Boolean isAllowTyposOnNumericTokens() {
        return allowTyposOnNumericTokens;
    }

    /**
     * @return the removeWordsIfNoResult
     */
    public RemoveWordsType getRemoveWordsIfNoResult() {
        return removeWordsIfNoResult;
    }

    /**
     * @return the typoTolerance
     */
    public TypoTolerance getTypoTolerance() {
        return typoTolerance;
    }
}