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
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getHitsPerPage());
        query1.setHitsPerPage(50);
        assertEquals(Integer.valueOf(50), query1.getHitsPerPage());
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getHitsPerPage(), query2.getHitsPerPage());
    }

    @Test
    public void query() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getQuery());
        query1.setQuery("San Francisco");
        assertEquals("San Francisco", query1.getQuery());
        assertEquals("San Francisco", query1.get("query"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getQuery(), query2.getQuery());
    }

    @Test
    public void highlightPreTag() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getHighlightPreTag());
        query1.setHighlightPreTag("<PRE[");
        assertEquals("<PRE[", query1.getHighlightPreTag());
        assertEquals("<PRE[", query1.get("highlightPreTag"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getHighlightPreTag(), query2.getHighlightPreTag());
    }

    @Test
    public void highlightPostTag() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getHighlightPostTag());
        query1.setHighlightPostTag("]POST>");
        assertEquals("]POST>", query1.getHighlightPostTag());
        assertEquals("]POST>", query1.get("highlightPostTag"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getHighlightPostTag(), query2.getHighlightPostTag());
    }

    @Test
    public void aroundRadius() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getAroundRadius());
        query1.setAroundRadius(987);
        assertEquals(Integer.valueOf(987), query1.getAroundRadius());
        assertEquals("987", query1.get("aroundRadius"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getAroundRadius(), query2.getAroundRadius());
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
        PlacesQuery query2 = PlacesQuery.parse(query.build());
        assertEquals(query2.getAroundRadius(), query.getAroundRadius());
    }

    @Test
    public void aroundLatLngViaIP() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getAroundLatLngViaIP());
        query1.setAroundLatLngViaIP(true);
        assertEquals(Boolean.TRUE, query1.getAroundLatLngViaIP());
        assertEquals("true", query1.get("aroundLatLngViaIP"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getAroundLatLngViaIP(), query2.getAroundLatLngViaIP());
    }

    @Test
    public void aroundLatLng() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getAroundLatLng());
        query1.setAroundLatLng(new PlacesQuery.LatLng(89.76, -123.45));
        assertEquals(new PlacesQuery.LatLng(89.76, -123.45), query1.getAroundLatLng());
        assertEquals("89.76,-123.45", query1.get("aroundLatLng"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getAroundLatLng(), query2.getAroundLatLng());
    }

    @Test
    public void language() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getQuery());
        query1.setLanguage("en");
        assertEquals("en", query1.getLanguage());
        assertEquals("en", query1.get("language"));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getQuery(), query2.getQuery());
    }

    @Test
    public void countries() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getCountries());
        query1.setCountries("de", "fr", "us");
        assertArrayEquals(query1.getCountries(), new String[]{ "de", "fr", "us" });
        assertEquals(query1.get("countries"), "[\"de\",\"fr\",\"us\"]");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertArrayEquals(query2.getCountries(), query1.getCountries());
    }

    @Test
    public void type() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getType());
        PlacesQuery query2;

        query1.setType(PlacesQuery.Type.CITY);
        assertEquals(PlacesQuery.Type.CITY, query1.getType());
        assertEquals("city", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.COUNTRY);
        assertEquals(PlacesQuery.Type.COUNTRY, query1.getType());
        assertEquals("country", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.ADDRESS);
        assertEquals(PlacesQuery.Type.ADDRESS, query1.getType());
        assertEquals("address", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.BUS_STOP);
        assertEquals(PlacesQuery.Type.BUS_STOP, query1.getType());
        assertEquals("busStop", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.TRAIN_STATION);
        assertEquals(PlacesQuery.Type.TRAIN_STATION, query1.getType());
        assertEquals("trainStation", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.TOWN_HALL);
        assertEquals(PlacesQuery.Type.TOWN_HALL, query1.getType());
        assertEquals("townhall", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.setType(PlacesQuery.Type.AIRPORT);
        assertEquals(PlacesQuery.Type.AIRPORT, query1.getType());
        assertEquals("airport", query1.get("type"));
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query1.getType(), query2.getType());

        query1.set("type", "invalid");
        assertNull(query1.getType());
    }
}
