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

import android.annotation.SuppressLint;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.android.util.concurrent.RoboExecutorService;

import java.lang.ref.WeakReference;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

@SuppressLint("DefaultLocale") //We use format for logging errors, locale issues are irrelevant
public class ClientTest extends RobolectricTestCase {
    Client client;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = new Client(Helpers.app_id, Helpers.api_key);
        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(client, "searchExecutorService", new RoboExecutorService());
    }

    @Override
    public void tearDown() throws Exception {
    }

    @Test
    public void testIndexReuse() throws Exception {
        Map<String, WeakReference<Index>> indices = (Map<String, WeakReference<Index>>) Whitebox.getInternalState(client, "indices");
        final String indexName = "name";

        // Ask for the same index twice and check that it is re-used.
        assertEquals(0, indices.size());
        Index index1 = client.getIndex(indexName);
        assertEquals(1, indices.size());
        Index index2 = client.getIndex(indexName);
        assertEquals(index1, index2);
        assertEquals(1, indices.size());

        // Release the index and check that the reference is null.
        // NOTE: This ought to work (works with a simple test case)... but does not.
        // Keeping the code for the sake of completeness.
        /*
        index1 = null;
        index2 = null;
        System.gc();
        assertNull(indices.get(indexName).get());
        */
    }

    @Test
    public void testUniqueAgent() {
        client.addUserAgent(new AbstractClient.LibraryVersion("foo", "bar"));
        client.addUserAgent(new AbstractClient.LibraryVersion("foo", "bar"));
        final AbstractClient.LibraryVersion[] userAgents = client.getUserAgents();
        int found = 0;
        for (AbstractClient.LibraryVersion userAgent : userAgents) {
            if ("foo".equals(userAgent.name)) {
                found++;
            }
        }
        assertEquals("There should be only one foo user agent.", 1, found);
    }
}
