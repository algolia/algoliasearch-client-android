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

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.UiThreadTest;

import com.algolia.search.saas.Listener.Index.IndexingListener;
import com.algolia.search.saas.Listener.Index.SearchListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

public class ApplicationTest extends ApplicationTestCase<Application> {
    APIClient client;
    Index index;
    String indexName;

    List<JSONObject> objects;
    List<String> ids;

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = new APIClient(Helpers.app_id, Helpers.api_key);
        indexName = Helpers.safeIndexName("àlgol?à-android");
        index = client.initIndex(indexName);

        objects = new ArrayList<JSONObject>();
        objects.add(new JSONObject("{\"city\": \"San Francisco\"}"));
        objects.add(new JSONObject("{\"city\": \"San José\"}"));

        JSONObject task = index.addObjects(new JSONArray(objects));
        index.waitTask(task.getString("taskID"));

        JSONArray objectIDs = task.getJSONArray("objectIDs");
        ids = new ArrayList<String>();
        for (int i = 0; i < objectIDs.length(); ++i) {
            ids.add(objectIDs.getString(i));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        client.deleteIndex(indexName);
        super.tearDown();
    }

    @UiThreadTest
    public void testSearchAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements SearchListener {
            @Override
            public void searchResult(Index index, Query query, JSONObject results) {
                assertEquals(objects.size(), results.optInt("nbHits"));
                signal.countDown();
            }

            @Override
            public void searchError(Index index, Query query, AlgoliaException e) {
                fail(String.format("Error during search: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener searchListener = new Listener();

        index.searchASync(new Query(), searchListener);
        assertTrue(signal.await(30, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertFalse(results.optString("objectID").equals(""));
                signal.countDown();
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during addObject: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener indexingListener = new Listener();

        index.addObjectASync(new JSONObject("{\"city\": \"New York\"}"), indexingListener);
        assertTrue(signal.await(30, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectWithObjectIDAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertTrue(results.optString("objectID").equals("a1b2c3"));
                signal.countDown();
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during addObjectWithObjectID: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener indexingListener = new Listener();

        index.addObjectASync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", indexingListener);
        assertTrue(signal.await(30, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertEquals(2, results.optJSONArray("objectIDs").length());
                signal.countDown();
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during addObjects: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener indexingListener = new Listener();

        index.addObjectsASync(new JSONArray("[{\"city\": \"New York\"}, {\"city\": \"Paris\"}]"), indexingListener);
        assertTrue(signal.await(30, TimeUnit.SECONDS));
    }
}
