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

import com.algolia.search.saas.listeners.GetObjectsListener;
import com.algolia.search.saas.listeners.IndexingListener;
import com.algolia.search.saas.listeners.SearchDisjunctiveFacetingListener;
import com.algolia.search.saas.listeners.SearchListener;
import com.algolia.search.saas.listeners.WaitTaskListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

public class IndexTest extends PowerMockTestCase {
    APIClient client;
    Index index;
    String indexName;

    List<JSONObject> objects;
    List<String> ids;

    @Override
    public void setUp() throws Exception {
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
    public void tearDown() throws Exception {
        client.deleteIndex(indexName);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testHostSwitch() throws Exception {
        // Given first host as an unreachable domain
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHostsArray");
        hostsArray.set(0, "thissentenceshouldbeuniqueenoughtoguaranteeinexistentdomain.com");
        Whitebox.setInternalState(client, "readHostsArray", hostsArray);

        // Expect a switch to the next URL and successful search
        testSearchAsync();
    }


    @Test
    public void testSNI() throws Exception {
        // Given all hosts using SNI
        String appId = (String) Whitebox.getInternalState(client, "applicationID");
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHostsArray");
        hostsArray.set(0, appId + "-1.algolianet.com");
        hostsArray.set(1, appId + "-2.algolianet.com");
        hostsArray.set(2, appId + "-3.algolianet.com");
        hostsArray.set(3, appId + "-3.algolianet.com");
        Whitebox.setInternalState(client, "readHostsArray", hostsArray);

        // Expect correct certificate handling and successful search
        testSearchAsync();
    }

    @Test
    public void testCacheUseIfEnabled() throws Exception {
        index.enableSearchCache();
        verifySearchTwiceCalls(1);
    }

    @Test
    public void testCacheDontUseByDefault() throws Exception {
        verifySearchTwiceCalls(2);
    }

    @Test
    public void testCacheDontUseIfDisabled() throws Exception {
        index.disableSearchCache();
        verifySearchTwiceCalls(2);
    }

    @Test
    public void testCacheTimeout() throws Exception {
        index.enableSearchCache(1, ExpiringCache.defaultMaxSize);
        verifySearchTwiceCalls(2, 2);
    }

    /**
     * Verifies the number of requests fired by two successive search queries
     *
     * @param nbTimes expected amount of requests
     */
    private void verifySearchTwiceCalls(int nbTimes) throws Exception {
        verifySearchTwiceCalls(nbTimes, 0);
    }

    /**
     * Verifies the number of requests fired by two search queries
     *
     * @param nbTimes            expected amount of requests
     * @param waitBetweenSeconds optional time to wait between the two queries
     */
    private void verifySearchTwiceCalls(int nbTimes, int waitBetweenSeconds) throws Exception {
        // Given a index, using a client that returns some json on search
        APIClient mockClient = mock(APIClient.class);
        Whitebox.setInternalState(index, "client", mockClient);
        when(mockClient.getRequestRaw(anyString(), anyBoolean())).thenReturn("{foo:42}".getBytes());

        // When searching twice separated by waitBetweenSeconds, fires nbTimes requests
        final Query query = new Query("San");
        index.search(query);
        if (waitBetweenSeconds > 0) {
            Thread.sleep(waitBetweenSeconds * 1000);
        }
        index.search(query);
        verify(mockClient, times(nbTimes)).getRequestRaw(anyString(), anyBoolean());
    }
}