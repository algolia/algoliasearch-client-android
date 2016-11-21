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
        assertEquals(query1.getHitsPerPage(), Integer.valueOf(50));
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getHitsPerPage(), query1.getHitsPerPage());
    }

    @Test
    public void query() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getQuery());
        query1.setQuery("San Francisco");
        assertEquals(query1.getQuery(), "San Francisco");
        assertEquals(query1.get("query"), "San Francisco");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getQuery(), query1.getQuery());
    }

    @Test
    public void highlightPreTag() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getHighlightPreTag());
        query1.setHighlightPreTag("<PRE[");
        assertEquals(query1.getHighlightPreTag(), "<PRE[");
        assertEquals(query1.get("highlightPreTag"), "<PRE[");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getHighlightPreTag(), query1.getHighlightPreTag());
    }

    @Test
    public void highlightPostTag() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getHighlightPostTag());
        query1.setHighlightPostTag("]POST>");
        assertEquals(query1.getHighlightPostTag(), "]POST>");
        assertEquals(query1.get("highlightPostTag"), "]POST>");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getHighlightPostTag(), query1.getHighlightPostTag());
    }

    @Test
    public void aroundRadius() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getAroundRadius());
        query1.setAroundRadius(987);
        assertEquals(query1.getAroundRadius(), Integer.valueOf(987));
        assertEquals(query1.get("aroundRadius"), "987");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getAroundRadius(), query1.getAroundRadius());
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
        assertEquals(query1.getAroundLatLngViaIP(), Boolean.TRUE);
        assertEquals(query1.get("aroundLatLngViaIP"), "true");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getAroundLatLngViaIP(), query1.getAroundLatLngViaIP());
    }

    @Test
    public void aroundLatLng() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getAroundLatLng());
        query1.setAroundLatLng(new PlacesQuery.LatLng(89.76, -123.45));
        assertEquals(query1.getAroundLatLng(), new PlacesQuery.LatLng(89.76, -123.45));
        assertEquals(query1.get("aroundLatLng"), "89.76,-123.45");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getAroundLatLng(), query1.getAroundLatLng());
    }

    @Test
    public void language() {
        PlacesQuery query1 = new PlacesQuery();
        assertNull(query1.getQuery());
        query1.setLanguage("en");
        assertEquals(query1.getLanguage(), "en");
        assertEquals(query1.get("language"), "en");
        PlacesQuery query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getQuery(), query1.getQuery());
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
        assertEquals(query1.getType(), PlacesQuery.Type.CITY);
        assertEquals(query1.get("type"), "city");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.COUNTRY);
        assertEquals(query1.getType(), PlacesQuery.Type.COUNTRY);
        assertEquals(query1.get("type"), "country");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.ADDRESS);
        assertEquals(query1.getType(), PlacesQuery.Type.ADDRESS);
        assertEquals(query1.get("type"), "address");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.BUS_STOP);
        assertEquals(query1.getType(), PlacesQuery.Type.BUS_STOP);
        assertEquals(query1.get("type"), "busStop");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.TRAIN_STATION);
        assertEquals(query1.getType(), PlacesQuery.Type.TRAIN_STATION);
        assertEquals(query1.get("type"), "trainStation");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.TOWN_HALL);
        assertEquals(query1.getType(), PlacesQuery.Type.TOWN_HALL);
        assertEquals(query1.get("type"), "townhall");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.setType(PlacesQuery.Type.AIRPORT);
        assertEquals(query1.getType(), PlacesQuery.Type.AIRPORT);
        assertEquals(query1.get("type"), "airport");
        query2 = PlacesQuery.parse(query1.build());
        assertEquals(query2.getType(), query1.getType());

        query1.set("type", "invalid");
        assertNull(query1.getType());
    }
}
