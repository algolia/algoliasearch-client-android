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

import android.annotation.SuppressLint;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.AssertCompletionHandler;
import com.algolia.search.saas.Helpers;
import com.algolia.search.saas.RobolectricTestCase;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.util.concurrent.RoboExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressLint("DefaultLocale")
public class PlacesClientTest extends RobolectricTestCase {
    public static final String OBJECT_ID_RUE_RIVOLI = "afd71bb8613f70ca495d8996923b5fd5";
    PlacesClient places;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        places = new PlacesClient(Helpers.PLACES_APP_ID, Helpers.PLACES_API_KEY);

        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(places, "searchExecutorService", new RoboExecutorService());
    }

    @Override
    public void tearDown() {
        AssertCompletionHandler.checkAllHandlers();
    }

    @Test
    public void searchAsync() throws Exception {
        PlacesQuery query = new PlacesQuery();
        query.setQuery("Paris");
        query.setType(PlacesQuery.Type.CITY);
        query.setHitsPerPage(10);
        query.setAroundLatLngViaIP(false);
        query.setAroundLatLng(new PlacesQuery.LatLng(32.7767, -96.7970)); // Dallas, TX, USA
        query.setLanguage("en");
        query.setCountries("fr", "us");
        places.searchAsync(query, new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error != null) {
                    fail(error.getMessage());
                }
                assertNotNull(content);
                assertNotNull(content.optJSONArray("hits"));
                assertTrue(content.optJSONArray("hits").length() > 0);
            }
        });
    }

    @Test
    public void getByObjectIDValid() throws Exception {
        final JSONObject rivoli = places.getByObjectID(OBJECT_ID_RUE_RIVOLI);
        assertNotNull(rivoli);
        assertEquals(OBJECT_ID_RUE_RIVOLI, rivoli.getString("objectID"));
    }

    @Test(expected = AlgoliaException.class)
    public void getByObjectIDInvalid() throws Exception {
        places.getByObjectID("4242424242");
    }
}
