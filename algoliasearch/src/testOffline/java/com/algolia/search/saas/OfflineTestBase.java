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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.concurrent.RoboExecutorService;

import java.util.HashMap;
import java.util.Map;

public abstract class OfflineTestBase extends RobolectricTestCase {
    /** Offline client. */
    protected OfflineClient client;

    /** Maximum time to wait for each test case. */
    protected static long waitTimeout = 5;

    /** Useful object constants. */
    protected static Map<String, JSONObject> objects = new HashMap<>();
    static {
        try {
            objects.put("snoopy", new JSONObject()
                    .put("objectID", "1")
                    .put("name", "Snoopy")
                    .put("kind", "dog")
                    .put("born", 1967)
                    .put("series", new JSONArray().put("Peanuts"))
            );
            objects.put("woodstock", new JSONObject()
                    .put("objectID", "2")
                    .put("name", "Woodstock")
                    .put("kind", "bird")
                    .put("born", 1970)
                    .put("series", new JSONArray().put("Peanuts"))
            );
        }
        catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        client = new OfflineClient(RuntimeEnvironment.application, Helpers.app_id, Helpers.api_key);
        // NOTE: We don't really control the package name with Robolectric's supplied application.
        // The license below is generated for package "com.algolia.search.saas.android".
        client.enableOfflineMode(" AkwBAQH/3YXDBf+GxMAFZBxDbJYBbWVudCBMZSBQcm92b3N0IChBbGdvbGlhKR9jb20uYWxnb2xpYS5zZWFyY2guc2Fhcy5hbmRyb2lkMC0CFAP8/jWtJskE4iRYYWAvHYbOOsf8AhUAsS5RNputtb8FEMkqn0r3MOgPmes=");

        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(client, "searchExecutorService", new RoboExecutorService());

        // Log the local directory used by Robolectric. Useful when debugging.
        Log.v(this.getClass().getName(), "Robolectric files dir: " + RuntimeEnvironment.application.getFilesDir().getAbsolutePath());
    }
}
