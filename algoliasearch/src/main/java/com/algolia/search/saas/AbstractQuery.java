/*
 * Copyright (c) 2016 Algolia
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;


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

/**
 * An abstract search query.
 */
public abstract class AbstractQuery {

    // ----------------------------------------------------------------------
    // Types
    // ----------------------------------------------------------------------

    /**
     * A pair of (latitude, longitude).
     * Used in geo-search.
     */
    public static final class LatLng {
        public final double lat;
        public final double lng;

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        public boolean equals(Object other) {
            return other != null && other instanceof LatLng
                    && this.lat == ((LatLng)other).lat && this.lng == ((LatLng)other).lng;
        }

        @Override
        public int hashCode() {
            return (int)Math.round(lat * lng % Integer.MAX_VALUE);
        }

        /**
         * Parse a `LatLng` from its string representation.
         *
         * @param value A string representation of a (latitude, longitude) pair, in the format `12.345,67.890`
         *              (number of digits may vary).
         * @return A `LatLng` instance describing the given geolocation, or `null` if `value` is `null` or does not
         *         represent a valid geolocation.
         */
        @Nullable public static LatLng parse(String value) {
            if (value == null) {
                return null;
            }
            String[] components = value.split(",");
            if (components.length != 2) {
                return null;
            }
            try {
                return new LatLng(Double.valueOf(components[0]), Double.valueOf(components[1]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /** Query parameters, as an untyped key-value array. */
    // NOTE: Using a tree map to have parameters sorted by key on output.
    private Map<String, String> parameters = new TreeMap<>();

    // ----------------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------------

    /**
     * Construct an empty query.
     */
    protected AbstractQuery() {
    }

    /**
     * Clone an existing query.
     * @param other The query to be cloned.
     */
    protected AbstractQuery(@NonNull AbstractQuery other) {
        parameters = new TreeMap<>(other.parameters);
    }

    // ----------------------------------------------------------------------
    // Equality
    // ----------------------------------------------------------------------

    @Override
    public boolean equals(Object other) {
        return other != null && other instanceof AbstractQuery && this.parameters.equals(((AbstractQuery)other).parameters);
    }

    @Override
    public int hashCode() {
        return parameters.hashCode();
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
    // Parsing/serialization
    // ----------------------------------------------------------------------

    /**
     * Build the URL query parameter string representing this object.
     * @return A string suitable for use inside the query part of a URL (i.e. after the question mark).
     */
    public @NonNull String build() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                if (stringBuilder.length() > 0)
                    stringBuilder.append('&');
                stringBuilder.append(urlEncode(key));
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
     * Parse a URL query parameter string and store the resulting parameters into this query.
     * @param queryParameters URL query parameter string.
     */
    protected void parseFrom(@NonNull String queryParameters) {
        try {
            String[] parameters = queryParameters.split("&");
            for (String parameter : parameters) {
                String[] components = parameter.split("=");
                if (components.length < 1 || components.length > 2)
                    continue; // ignore invalid values
                String name = URLDecoder.decode(components[0], "UTF-8");
                String value = components.length >= 2 ? URLDecoder.decode(components[1], "UTF-8") : null;
                set(name, value);
            } // for each parameter
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF-8 is one of the default encodings.
            throw new RuntimeException(e);
        }
    }

    protected static Boolean parseBoolean(String value) {
        if (value == null) {
            return null;
        }
        if (value.trim().toLowerCase().equals("true")) {
            return true;
        }
        Integer intValue = parseInt(value);
        return intValue != null && intValue != 0;
    }

    protected static Integer parseInt(String value)  {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static String buildJSONArray(String[] values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array.toString();
    }

    protected static String[] parseArray(String string) {
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

    protected static String buildCommaArray(String[] values) {
        return TextUtils.join(",", values);
    }

    protected static String[] parseCommaArray(String string) {
        return string == null ? null : string.split(",");
    }

    /**
     * @deprecated Please use {@link LatLng#parse(String)} instead.
     */
    @Nullable public static LatLng parseLatLng(String value) {
        return LatLng.parse(value);
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
     * @return This instance (used to chain calls).
     */
    public @NonNull AbstractQuery set(@NonNull String name, @Nullable Object value) {
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
    public @Nullable String get(@NonNull String name) {
        return parameters.get(name);
    }
}