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
import org.robolectric.android.util.concurrent.RoboExecutorService;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for offline test cases.
 *
 * **WARNING:** Robolectric tests run *on the local platform* (whatever that is, e.g. Linux or macOS), not in a real
 * Android environment. Therefore, the various native libraries supplied with `algoliasearch-offline-core-android`
 * cannot be loaded, leaving this module with an unsatisfied runtime dependency that will make the tests crash.
 * The solution is to provide an ad hoc JNI library similar to those bundled with `algoliasearch-offline-core-android`,
 * but compiled for the local platform. Currently, such a library only exists on macOS.
 * Consequently, **the offline tests cannot run in Travis**.
 *
 * **Note:** The JNI library should be placed somewhere in the JVM's path (as indicated by the system property
 * `java.library.path`). On macOS, it normally contains the following directories;
 *
 * - `~/Library/Java/Extensions`
 * - `/Library/Java/Extensions`
 * - `/Network/Library/Java/Extensions`
 * - `/System/Library/Java/Extensions`
 * - `/usr/lib/java`
 *
 * I found the first one to be the most convenient to use.
 */
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

    /** Index settings. */
    protected static JSONObject settings = new JSONObject();
    static {
        try {
            settings
                .put("searchableAttributes", new JSONArray()
                    .put("name").put("kind").put("series")
                )
                .put("attributesForFaceting", new JSONArray().put("searchable(series)")
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
        client.enableOfflineMode("AkcFAQH/pIS5Bf+zpLUFZBhBbGdvbGlhIERldmVsb3BtZW50IFRlYW0fY29tLmFsZ29saWEuc2VhcmNoLnNhYXMuYW5kcm9pZDAtAhR5PKPCETwiBwN+FnUsMtDHwnIlngIVAKY1bFra5zh0fMscmoJ71RA6L3aQ");

        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(client, "searchExecutorService", new RoboExecutorService());
        Whitebox.setInternalState(client, "localBuildExecutorService", new RoboExecutorService());
        Whitebox.setInternalState(client, "localSearchExecutorService", new RoboExecutorService());
        Whitebox.setInternalState(client, "transactionExecutorService", new RoboExecutorService());

        // Log the local directory used by Robolectric. Useful when debugging.
        Log.v(this.getClass().getName(), "Robolectric files dir: " + RuntimeEnvironment.application.getFilesDir().getAbsolutePath());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        AssertCompletionHandler.checkAllHandlers();
    }
}
