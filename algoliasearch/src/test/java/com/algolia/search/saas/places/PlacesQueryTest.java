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

package com.algolia.search.saas.places;

import com.algolia.search.saas.RobolectricTestCase;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the `PlacesQuery` class.
 */
public class PlacesQueryTest extends RobolectricTestCase  {

    // ----------------------------------------------------------------------
    // High-level
    // ----------------------------------------------------------------------

    @Test
    public void hitsPerPage() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getHitsPerPage());
        query.setHitsPerPage(50);
        assertEquals(Integer.valueOf(50), query.getHitsPerPage());
        assertEquals(query.getHitsPerPage(), PlacesQuery.parse(query.build()).getHitsPerPage());
    }

    @Test
    public void query() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getQuery());
        query.setQuery("San Francisco");
        assertEquals("San Francisco", query.getQuery());
        assertEquals("San Francisco", query.get("query"));
        assertEquals(query.getQuery(), PlacesQuery.parse(query.build()).getQuery());
    }

    @Test
    public void highlightPreTag() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getHighlightPreTag());
        query.setHighlightPreTag("<PRE[");
        assertEquals("<PRE[", query.getHighlightPreTag());
        assertEquals("<PRE[", query.get("highlightPreTag"));
        assertEquals(query.getHighlightPreTag(), PlacesQuery.parse(query.build()).getHighlightPreTag());
    }

    @Test
    public void highlightPostTag() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getHighlightPostTag());
        query.setHighlightPostTag("]POST>");
        assertEquals("]POST>", query.getHighlightPostTag());
        assertEquals("]POST>", query.get("highlightPostTag"));
        assertEquals(query.getHighlightPostTag(), PlacesQuery.parse(query.build()).getHighlightPostTag());
    }

    @Test
    public void aroundRadius() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getAroundRadius());
        query.setAroundRadius(987);
        assertEquals(Integer.valueOf(987), query.getAroundRadius());
        assertEquals("987", query.get("aroundRadius"));
        assertEquals(query.getAroundRadius(), PlacesQuery.parse(query.build()).getAroundRadius());
    }

    @Test
    public void aroundRadius_all() {
        final Integer VALUE = 3;
        PlacesQuery query = new PlacesQuery();
        assertNull("A new query should have a null aroundRadius.", query.getAroundRadius());

        query.setAroundRadius(VALUE);
        assertEquals("After setting its aroundRadius to a given integer, we should return it from getAroundRadius.", VALUE, query.getAroundRadius());

        String queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=" + VALUE + "'.", queryStr.matches("aroundRadius=" + VALUE));

        query.setAroundRadius(PlacesQuery.RADIUS_ALL);
        assertEquals("After setting it to RADIUS_ALL, a query should have this aroundRadius value.", Integer.valueOf(PlacesQuery.RADIUS_ALL), query.getAroundRadius());

        queryStr = query.build();
        assertTrue("The built query should contain 'aroundRadius=all', not _" + queryStr + "_.", queryStr.matches("aroundRadius=all"));
        assertEquals(PlacesQuery.parse(query.build()).getAroundRadius(), query.getAroundRadius());
    }

    @Test
    public void aroundLatLngViaIP() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getAroundLatLngViaIP());
        query.setAroundLatLngViaIP(true);
        assertEquals(Boolean.TRUE, query.getAroundLatLngViaIP());
        assertEquals("true", query.get("aroundLatLngViaIP"));
        assertEquals(query.getAroundLatLngViaIP(), PlacesQuery.parse(query.build()).getAroundLatLngViaIP());
    }

    @Test
    public void aroundLatLng() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getAroundLatLng());
        query.setAroundLatLng(new PlacesQuery.LatLng(89.76, -123.45));
        assertEquals(new PlacesQuery.LatLng(89.76, -123.45), query.getAroundLatLng());
        assertEquals("89.76,-123.45", query.get("aroundLatLng"));
        assertEquals(query.getAroundLatLng(), PlacesQuery.parse(query.build()).getAroundLatLng());
    }

    @Test
    public void language() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getQuery());
        query.setLanguage("en");
        assertEquals("en", query.getLanguage());
        assertEquals("en", query.get("language"));
        assertEquals(query.getQuery(), PlacesQuery.parse(query.build()).getQuery());
    }

    @Test
    public void countries() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getCountries());
        query.setCountries("de", "fr", "us");
        assertArrayEquals(query.getCountries(), new String[]{ "de", "fr", "us" });
        assertEquals(query.get("countries"), "[\"de\",\"fr\",\"us\"]");
        assertArrayEquals(PlacesQuery.parse(query.build()).getCountries(), query.getCountries());
    }

    @Test
    public void type() {
        PlacesQuery query = new PlacesQuery();
        assertNull(query.getType());

        query.setType(PlacesQuery.Type.CITY);
        assertEquals(PlacesQuery.Type.CITY, query.getType());
        assertEquals("city", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.COUNTRY);
        assertEquals(PlacesQuery.Type.COUNTRY, query.getType());
        assertEquals("country", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.ADDRESS);
        assertEquals(PlacesQuery.Type.ADDRESS, query.getType());
        assertEquals("address", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.BUS_STOP);
        assertEquals(PlacesQuery.Type.BUS_STOP, query.getType());
        assertEquals("busStop", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.TRAIN_STATION);
        assertEquals(PlacesQuery.Type.TRAIN_STATION, query.getType());
        assertEquals("trainStation", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.TOWN_HALL);
        assertEquals(PlacesQuery.Type.TOWN_HALL, query.getType());
        assertEquals("townhall", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.setType(PlacesQuery.Type.AIRPORT);
        assertEquals(PlacesQuery.Type.AIRPORT, query.getType());
        assertEquals("airport", query.get("type"));
        assertEquals(query.getType(), PlacesQuery.parse(query.build()).getType());

        query.set("type", "invalid");
        assertNull(query.getType());
    }
}
