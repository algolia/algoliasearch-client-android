package com.algolia.search.saas.places;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.algolia.search.saas.AbstractQuery;
import com.algolia.search.saas.CompletionHandler;

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
 * Search parameters for Algolia Places.
 *
 * @see PlacesClient#searchAsync(PlacesQuery, CompletionHandler)
 */
public class PlacesQuery extends AbstractQuery {
    // ----------------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------------

    /**
     * Construct an empty query.
     */
    public PlacesQuery() {
    }

    /**
     * Construct a query with the specified full text query.
     *
     * @param query Full text query.
     */
    public PlacesQuery(CharSequence query) {
        setQuery(query);
    }

    /**
     * Clone an existing query.
     *
     * @param other The query to be cloned.
     */
    public PlacesQuery(@NonNull PlacesQuery other) {
        super(other);
    }

    // ----------------------------------------------------------------------
    // High-level (typed) accessors
    // ----------------------------------------------------------------------

    private static final String KEY_QUERY = "query";

    /**
     * Set the full text query.
     */
    public @NonNull
    PlacesQuery setQuery(CharSequence query) {
        return set(KEY_QUERY, query);
    }

    public String getQuery() {
        return get(KEY_QUERY);
    }

    private static final String KEY_AROUND_LAT_LNG = "aroundLatLng";

    /**
     * Force to *first* search around a specific latitude/longitude.
     * The default is to search around the location of the user determined via his IP address (geoip).
     *
     * @param location The location to start the search at, or `null` to use the default.
     */
    public @NonNull
    PlacesQuery setAroundLatLng(LatLng location) {
        if (location == null) {
            return set(KEY_AROUND_LAT_LNG, null);
        } else {
            return set(KEY_AROUND_LAT_LNG, location.lat + "," + location.lng);
        }
    }

    public LatLng getAroundLatLng() {
        return LatLng.parse(get(KEY_AROUND_LAT_LNG));
    }

    private static final String KEY_AROUND_LAT_LNG_VIA_IP = "aroundLatLngViaIP";

    /**
     * Search *first* around the geolocation of the user found via his IP address.
     * Default: `true`.
     *
     * @param enabled Whether to use IP address to determine geolocation, or `null` to use the default.
     */
    public @NonNull
    PlacesQuery setAroundLatLngViaIP(Boolean enabled) {
        return set(KEY_AROUND_LAT_LNG_VIA_IP, enabled);
    }

    public Boolean getAroundLatLngViaIP() {
        return parseBoolean(get(KEY_AROUND_LAT_LNG_VIA_IP));
    }

    private static final String KEY_AROUND_RADIUS = "aroundRadius";
    public static final int RADIUS_ALL = Integer.MAX_VALUE;

    /**
     * Change the radius for around latitude/longitude queries.
     *
     * @param radius The radius to set, or {@link #RADIUS_ALL} to disable stopping at a specific radius, or `null` to
     *               use the default.
     */
    public @NonNull
    PlacesQuery setAroundRadius(Integer radius) {
        if (radius == PlacesQuery.RADIUS_ALL) {
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
            return PlacesQuery.RADIUS_ALL;
        }
        return parseInt(value);
    }

    private static final String KEY_HIGHLIGHT_POST_TAG = "highlightPostTag";

    public @NonNull
    PlacesQuery setHighlightPostTag(String tag) {
        return set(KEY_HIGHLIGHT_POST_TAG, tag);
    }

    public String getHighlightPostTag() {
        return get(KEY_HIGHLIGHT_POST_TAG);
    }

    private static final String KEY_HIGHLIGHT_PRE_TAG = "highlightPreTag";

    public @NonNull
    PlacesQuery setHighlightPreTag(String tag) {
        return set(KEY_HIGHLIGHT_PRE_TAG, tag);
    }

    public String getHighlightPreTag() {
        return get(KEY_HIGHLIGHT_PRE_TAG);
    }

    private static final String KEY_HITS_PER_PAGE = "hitsPerPage";

    /**
     * Set how many results you want to retrieve per search. Default: 20.
     */
    public @NonNull
    PlacesQuery setHitsPerPage(Integer nbHitsPerPage) {
        return set(KEY_HITS_PER_PAGE, nbHitsPerPage);
    }

    public Integer getHitsPerPage() {
        return parseInt(get(KEY_HITS_PER_PAGE));
    }

    /**
     * Types of places that can be searched for.
     */
    public static enum Type {
        /** City. */
        CITY,
        /** Country. */
        COUNTRY,
        /** Address. */
        ADDRESS,
        /** Bus stop. */
        BUS_STOP,
        /** Train station. */
        TRAIN_STATION,
        /** Town hall. */
        TOWN_HALL,
        /** Airport. */
        AIRPORT
    }

    private static final String KEY_TYPE = "type";

    /**
     * Set the type of place to search for.
     *
     * @param type Type of place to search for.
     */
    public @NonNull PlacesQuery setType(Type type) {
        if (type == null) {
            set(KEY_TYPE, null);
        } else {
            switch (type) {
                case CITY:
                    set(KEY_TYPE, "city");
                    break;
                case COUNTRY:
                    set(KEY_TYPE, "country");
                    break;
                case ADDRESS:
                    set(KEY_TYPE, "address");
                    break;
                case BUS_STOP:
                    set(KEY_TYPE, "busStop");
                    break;
                case TRAIN_STATION:
                    set(KEY_TYPE, "trainStation");
                    break;
                case TOWN_HALL:
                    set(KEY_TYPE, "townhall");
                    break;
                case AIRPORT:
                    set(KEY_TYPE, "airport");
                    break;
            }
        }
        return this;
    }

    public Type getType() {
        String value = get(KEY_TYPE);
        if (value != null) {
            switch (value) {
                case "city":
                    return Type.CITY;
                case "country":
                    return Type.COUNTRY;
                case "address":
                    return Type.ADDRESS;
                case "busStop":
                    return Type.BUS_STOP;
                case "trainStation":
                    return Type.TRAIN_STATION;
                case "townhall":
                    return Type.TOWN_HALL;
                case "airport":
                    return Type.AIRPORT;
            }
        }
        return null;
    }

    private static final String KEY_LANGUAGE = "language";

    /**
     * Restrict the search results to a single language.
     * You can pass two letters country codes (<a href="https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">ISO 639-1</a>).
     *
     * @param language The language used to return the results, or `null` to use all available languages.
     * @return This query.
     */
    public @NonNull PlacesQuery setLanguage(String language) {
        return set(KEY_LANGUAGE, language);
    }

    public String getLanguage() {
        return get(KEY_LANGUAGE);
    }

    private static final String KEY_COUNTRIES = "countries";

    /**
     * Restrict the search results to a specific list of countries.
     * You can pass two letters country codes (<a href="https://en.wikipedia.org/wiki/ISO_3166-1#Officially_assigned_code_elements">ISO 3166-1</a>).
     * <p>
     * Default: Search on the whole planet.
     *
     * @param countries The countries to restrict the search to, or `null` to search on the whole planet.
     * @return This query.
     */
    public @NonNull PlacesQuery setCountries(String... countries) {
        return set(KEY_COUNTRIES, buildJSONArray(countries));
    }

    public String[] getCountries() {
        return parseArray(get(KEY_COUNTRIES));
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
    protected static @NonNull
    PlacesQuery parse(@NonNull String queryParameters) {
        PlacesQuery query = new PlacesQuery();
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
    public @NonNull
    PlacesQuery set(@NonNull String name, @Nullable Object value) {
        return (PlacesQuery) super.set(name, value);
    }
}
