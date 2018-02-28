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

package com.algolia.search.saas;

import android.support.annotation.NonNull;

import com.algolia.search.saas.helpers.BrowseIterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.android.util.concurrent.RoboExecutorService;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.fail;

public class BrowseIteratorTest extends RobolectricTestCase {
    Client client;
    Index index;
    String indexName;

    List<JSONObject> objects;
    List<String> ids;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = new Client(Helpers.app_id, Helpers.api_key);
        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(client, "searchExecutorService", new RoboExecutorService());
        indexName = Helpers.safeIndexName("àlgol?à-android");
        index = client.initIndex(indexName);

        objects = new ArrayList<JSONObject>();
        for (int i = 0; i < 1500; ++i) {
            objects.add(new JSONObject(String.format("{\"dummy\": %d}", i)));
        }
        JSONObject task = index.addObjects(new JSONArray(objects), /* requestOptions: */ null);
        index.waitTask(task.getString("taskID"));

        JSONArray objectIDs = task.getJSONArray("objectIDs");
        ids = new ArrayList<String>();
        for (int i = 0; i < objectIDs.length(); ++i) {
            ids.add(objectIDs.getString(i));
        }
    }

    @Override
    public void tearDown() throws Exception {
        client.deleteIndex(indexName, /* requestOptions: */ null);
    }

    @Test
    public void nominal() throws Exception {
        Query query = new Query();
        AssertBrowseHandler handler = new AssertBrowseHandler() {
            @Override
            void doHandleBatch(BrowseIterator iterator, JSONObject result, AlgoliaException error) {
                if (error != null) {
                    fail(error.getMessage());
                }
            }
        };
        BrowseIterator iterator = new BrowseIterator(index, query, handler);
        iterator.start();
        handler.checkAssertions();
        handler.checkCalledMax(2);
    }

    @Test
    public void cancel() throws Exception {
        Query query = new Query();
        AssertBrowseHandler handler = new AssertBrowseHandler() {
            @Override
            void doHandleBatch(BrowseIterator iterator, JSONObject result, AlgoliaException error) {
                if (error == null) {
                    if (getCount() == 1) {
                        iterator.cancel();
                    }
                } else {
                    fail(error.getMessage());
                }
            }
        };
        BrowseIterator iterator = new BrowseIterator(index, query, handler);
        iterator.start();
        handler.checkAssertions();
        handler.checkCalledMax(1);
    }

    private abstract class AssertBrowseHandler implements BrowseIterator.BrowseIteratorHandler {

        private AssertionError error;
        private int count;

        abstract void doHandleBatch(BrowseIterator iterator, JSONObject result, AlgoliaException error);

        /**
         * Fail if the handler encountered at least one AssertionError.
         */
        public void checkAssertions() {
            if (error != null) {
                fail(error.getMessage());
            }
        }

        /**
         * Fail if the handler was called more than a certain amount of time.
         * @param times the maximum allowed before failing.
         */
        public void checkCalledMax(int times) {
            if (count > times) {
                fail("The BrowseIterator was called more than " + times + " times.");
            }
        }

        @Override
        public void handleBatch(@NonNull BrowseIterator iterator, JSONObject result, AlgoliaException error) {
            count++;
            try {

                doHandleBatch(iterator, result, error);
            } catch (AssertionError e) {
                this.error = e;
            }
        }

        public int getCount() {
            return count;
        }
    }
}