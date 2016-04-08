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

import com.algolia.search.saas.listeners.GetObjectsListener;
import com.algolia.search.saas.listeners.IndexingListener;
import com.algolia.search.saas.listeners.SearchDisjunctiveFacetingListener;
import com.algolia.search.saas.listeners.SearchListener;
import com.algolia.search.saas.listeners.WaitTaskListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void testQueryExtraParameters() throws Exception {
        // Accessor methods.
        {
            Query query = new Query();
            query.set("extra", "value");
            assertEquals("value", query.get("extra"));
            assertEquals("extra=value", query.getQueryString());
            query.set("abc", "def");
            String url = query.getQueryString();
            assertTrue(url.equals("abc=def&extra=value") || url.equals("extra=value&abc=def"));
        }
        // Percent escaping.
        {
            Query query = new Query();
            query.set("%&=", "àéïôù");
            assertEquals("%25%26%3D=%C3%A0%C3%A9%C3%AF%C3%B4%C3%B9", query.getQueryString());
        }
        // Overriding a typed query parameter.
        {
            Query query = new Query();
            query.hitsPerPage = 100;
            query.set("hitsPerPage", "666");
            assertEquals("hitsPerPage=100&hitsPerPage=666", query.getQueryString());
        }
        // Deleting a parameter.
        {
            Query query = new Query();
            query.set("abc", "ABC");
            query.set("def", "DEF");
            query.set("abc", null);
            assertEquals("def=DEF", query.getQueryString());
        }
    }

    @UiThreadTest
    public void testSearchAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements SearchListener {
            @Override
            public void searchResult(Index index, Query query, JSONObject results) {
                assertEquals("Result length does not match nbHits", objects.size(), results.optInt("nbHits"));
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
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testSearchDisjunctiveFacetingAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements SearchDisjunctiveFacetingListener {
            @Override
            public void searchDisjunctiveFacetingResult(Index index, Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements, JSONObject results) {
                assertEquals("Result length does not match nbHits", objects.size(), results.optInt("nbHits"));
                signal.countDown();
            }

            @Override
            public void searchDisjunctiveFacetingError(Index index, Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements, AlgoliaException e) {
                fail(String.format("Error during search: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener searchListener = new Listener();

        index.searchDisjunctiveFacetingAsync(new Query(), new ArrayList<String>(), new HashMap<String, List<String>>(), searchListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertFalse("Result has no objectId", results.optString("objectID").equals(""));
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
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectWithObjectIDAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertTrue("Object has unexpected objectId", results.optString("objectID").equals("a1b2c3"));
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
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testAddObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertEquals("Objects have unexpected objectId count", 2, results.optJSONArray("objectIDs").length());
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
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testSaveObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertTrue("Object has unexpected objectId", results.optString("objectID").equals("a1b2c3"));
                signal.countDown();
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during saveObject: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener indexingListener = new Listener();

        index.saveObjectASync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", indexingListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testSaveObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements IndexingListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                assertEquals("Objects have unexpected objectId count", 2, results.optJSONArray("objectIDs").length());
                assertEquals("Object has unexpected objectId", 123, results.optJSONArray("objectIDs").optInt(0));
                assertEquals("Object has unexpected objectId", 456, results.optJSONArray("objectIDs").optInt(1));
                signal.countDown();
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during saveObjects: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener indexingListener = new Listener();

        index.saveObjectsASync(new JSONArray("[{\"city\": \"New York\", \"objectID\": 123}, {\"city\": \"Paris\", \"objectID\": 456}]"), indexingListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testGetObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements GetObjectsListener {
            @Override
            public void getObjectsResult(Index index, TaskParams.GetObjects context, JSONObject results) {
                assertTrue("Object has unexpected objectId", results.optString("objectID").equals(ids.get(0)));
                assertTrue("Object has unexpected 'city' attribute", results.optString("city").equals("San Francisco"));
                signal.countDown();
            }

            @Override
            public void getObjectsError(Index index, TaskParams.GetObjects context, AlgoliaException e) {
                fail(String.format("Error during getObject: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener getObjectsListener = new Listener();

        index.getObjectASync(ids.get(0), getObjectsListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testGetObjectWithAttributesToRetrieveAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements GetObjectsListener {
            @Override
            public void getObjectsResult(Index index, TaskParams.GetObjects context, JSONObject results) {
                assertTrue("Object has unexpected objectId", results.optString("objectID").equals(ids.get(0)));
                assertFalse("Object has unexpected 'city' attribute", results.has("city"));
                signal.countDown();
            }

            @Override
            public void getObjectsError(Index index, TaskParams.GetObjects context, AlgoliaException e) {
                fail(String.format("Error during getObjectWithAttributesToRetrieve: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener getObjectsListener = new Listener();

        List<String> attributesToRetrieve = new ArrayList<String>();
        attributesToRetrieve.add("objectID");
        index.getObjectASync(ids.get(0), attributesToRetrieve, getObjectsListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testGetObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements GetObjectsListener {
            @Override
            public void getObjectsResult(Index index, TaskParams.GetObjects context, JSONObject results) {
                JSONArray res = results.optJSONArray("results");
                assertTrue("Object has unexpected objectId", res.optJSONObject(0).optString("objectID").equals(ids.get(0)));
                assertTrue("Object has unexpected objectId", res.optJSONObject(1).optString("objectID").equals(ids.get(1)));
                signal.countDown();
            }

            @Override
            public void getObjectsError(Index index, TaskParams.GetObjects context, AlgoliaException e) {
                fail(String.format("Error during getObjects: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener getObjectsListener = new Listener();

        index.getObjectsASync(ids, getObjectsListener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testWaitTaskAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(2);

        class Listener implements IndexingListener, WaitTaskListener {
            @Override
            public void indexingResult(Index index, TaskParams.Indexing context, JSONObject results) {
                signal.countDown();
                index.waitTaskASync(results.optString("taskID"), this);
            }

            @Override
            public void indexingError(Index index, TaskParams.Indexing context, AlgoliaException e) {
                fail(String.format("Error during addObject: %s", e.getMessage()));
                signal.countDown();
            }

            @Override
            public void waitTaskResult(Index index, String taskID) {
                assertFalse("Task ID not found", taskID.equals(""));
                signal.countDown();
            }

            @Override
            public void waitTaskError(Index index, String taskID, AlgoliaException e) {
                fail(String.format("Error during waitTask: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener listener = new Listener();

        index.addObjectASync(new JSONObject("{\"city\": \"New York\"}"), listener);
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }
}