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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.util.concurrent.RoboExecutorService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

        client.deleteIndex(indexName);

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
        testSearchAsync(Helpers.wait);
    }

    public void testSearchAsync(int waitTimeoutSeconds) throws Exception {
        final long begin = System.nanoTime();
        // Empty search.
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Result length does not match nbHits", objects.size(), content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.searchAsync(new Query(), handler);
        handler.checkAssertions();

        // Search with query.
        handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals(1, content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.searchAsync(new Query("Francisco"), handler);
        handler.checkAssertions();

        final long elapsedMillis = (System.nanoTime() - begin) / 1000000;
        final int waitTimeoutMillis = waitTimeoutSeconds * 1000;
        assertTrue("The test took longer than given timeout (" + elapsedMillis + " > " + waitTimeoutMillis + ").", elapsedMillis <= waitTimeoutMillis);
    }

    @Test
    public void testSearchDisjunctiveFacetingAsync() throws Exception {
        // Set index settings.
        JSONObject setSettingsResult = index.setSettings(new JSONObject("{\"attributesForFaceting\": [\"brand\", \"category\"]}"));
        index.waitTask(setSettingsResult.getString("taskID"));

        // Empty query
        // -----------
        // Not very useful, but we have to check this edge case.
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Result length does not match nbHits", objects.size(), content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.searchDisjunctiveFacetingAsync(new Query(), new ArrayList<String>(), new HashMap<String, List<String>>(), handler);
        handler.checkAssertions();

        // "Real" query
        // ------------
        // Create data set.
        objects = new ArrayList<>();
        objects.add(new JSONObject("{\"name\": \"iPhone 6\", \"brand\": \"Apple\", \"category\": \"device\",\"stars\":4}"));
        objects.add(new JSONObject("{\"name\": \"iPhone 6 Plus\", \"brand\": \"Apple\", \"category\": \"device\",\"stars\":5}"));
        objects.add(new JSONObject("{\"name\": \"iPhone cover\", \"brand\": \"Apple\", \"category\": \"accessory\",\"stars\":3}"));
        objects.add(new JSONObject("{\"name\": \"Galaxy S5\", \"brand\": \"Samsung\", \"category\": \"device\",\"stars\":4}"));
        objects.add(new JSONObject("{\"name\": \"Wonder Phone\", \"brand\": \"Samsung\", \"category\": \"device\",\"stars\":5}"));
        objects.add(new JSONObject("{\"name\": \"Platinum Phone Cover\", \"brand\": \"Samsung\", \"category\": \"accessory\",\"stars\":2}"));
        objects.add(new JSONObject("{\"name\": \"Lame Phone\", \"brand\": \"Whatever\", \"category\": \"device\",\"stars\":1}"));
        objects.add(new JSONObject("{\"name\": \"Lame Phone cover\", \"brand\": \"Whatever\", \"category\": \"accessory\",\"stars\":1}"));
        JSONObject task = index.addObjects(new JSONArray(objects));
        index.waitTask(task.getString("taskID"));

        final Query query = new Query("phone").setFacets("brand", "category", "stars");
        final List<String> disjunctiveFacets = Arrays.asList("brand");
        final Map<String, List<String>> refinements = new HashMap<>();
        refinements.put("brand", Arrays.asList("Apple", "Samsung")); // disjunctive facet
        refinements.put("category", Arrays.asList("device")); // conjunctive facet
        handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error != null) {
                    fail(error.getMessage());
                } else {
                    assertEquals(3, content.optInt("nbHits"));
                    JSONObject disjunctiveFacetsResult = content.optJSONObject("disjunctiveFacets");
                    assertNotNull(disjunctiveFacetsResult);
                    JSONObject brandFacetCounts = disjunctiveFacetsResult.optJSONObject("brand");
                    assertNotNull(brandFacetCounts);
                    assertEquals(2, brandFacetCounts.optInt("Apple"));
                    assertEquals(1, brandFacetCounts.optInt("Samsung"));
                    assertEquals(1, brandFacetCounts.optInt("Whatever"));
                }
            }
        };
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, handler);
        handler.checkAssertions();
    }

    @Test
    public void testDisjunctiveFacetingAsync2() throws Exception {
        // Set index settings.
        JSONObject setSettingsResult = index.setSettings(new JSONObject("{\"attributesForFaceting\":[\"city\", \"stars\", \"facilities\"]}"));
        index.waitTask(setSettingsResult.getString("taskID"));

        // Add objects.
        JSONObject addObjectsResult = index.addObjects(new JSONArray()
                .put(new JSONObject("{\"name\":\"Hotel A\", \"stars\":\"*\", \"facilities\":[\"wifi\", \"bath\", \"spa\"], \"city\":\"Paris\"}"))
                .put(new JSONObject("{\"name\":\"Hotel B\", \"stars\":\"*\", \"facilities\":[\"wifi\"], \"city\":\"Paris\"}"))
                .put(new JSONObject("{\"name\":\"Hotel C\", \"stars\":\"**\", \"facilities\":[\"bath\"], \"city\":\"San Fancisco\"}"))
                .put(new JSONObject("{\"name\":\"Hotel D\", \"stars\":\"****\", \"facilities\":[\"spa\"], \"city\":\"Paris\"}"))
                .put(new JSONObject("{\"name\":\"Hotel E\", \"stars\":\"****\", \"facilities\":[\"spa\"], \"city\":\"New York\"}")));
        index.waitTask(addObjectsResult.getString("taskID"));

        // Search.
        final Query query = new Query("h").setFacets("city");
        final List<String> disjunctiveFacets = Arrays.asList("stars", "facilities");
        final Map<String, List<String>> refinements = new HashMap<>();

        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertEquals(5, content.optInt("nbHits"));
                assertEquals(1, content.optJSONObject("facets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").length());
            }
        };
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, handler);
        handler.checkAssertions();

        refinements.put("stars", Arrays.asList("*"));
        handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertEquals(2, content.optInt("nbHits"));
                assertEquals(1, content.optJSONObject("facets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("*"));
                assertEquals(1, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("**"));
                assertEquals(2, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("****"));
            }
        };
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, handler);
        handler.checkAssertions();

        refinements.put("city", Arrays.asList("Paris"));
        handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertEquals(2, content.optInt("nbHits"));
                assertEquals(1, content.optJSONObject("facets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("*"));
                assertEquals(1, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("****"));
            }
        };
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, handler);
        handler.checkAssertions();

        refinements.put("stars", Arrays.asList("*", "****"));
        handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {

                assertEquals(3, content.optInt("nbHits"));
                assertEquals(1, content.optJSONObject("facets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").length());
                assertEquals(2, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("*"));
                assertEquals(1, content.optJSONObject("disjunctiveFacets").optJSONObject("stars").optInt("****"));
            }
        };
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, handler);
        handler.checkAssertions();
    }

    @Test
    public void testAddObjectAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertNotNull("Result has no objectId:" + content, content.optString("objectID", null));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), handler);
        handler.checkAssertions();
    }

    @Test
    public void testAddObjectWithObjectIDAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals("a1b2c3"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", handler);
        handler.checkAssertions();
    }

    @Test
    public void testAddObjectsAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Objects have unexpected objectId count", 2, content.optJSONArray("objectIDs").length());
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.addObjectsAsync(new JSONArray("[{\"city\": \"New York\"}, {\"city\": \"Paris\"}]"), handler);
        handler.checkAssertions();
    }

    @Test
    public void testSaveObjectAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals("a1b2c3"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.saveObjectAsync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", handler);
        handler.checkAssertions();
    }

    @Test
    public void testSaveObjectsAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Objects have unexpected objectId count", 2, content.optJSONArray("objectIDs").length());
                    assertEquals("Object has unexpected objectId", 123, content.optJSONArray("objectIDs").optInt(0));
                    assertEquals("Object has unexpected objectId", 456, content.optJSONArray("objectIDs").optInt(1));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.saveObjectsAsync(new JSONArray("[{\"city\": \"New York\", \"objectID\": 123}, {\"city\": \"Paris\", \"objectID\": 456}]"), handler);
        handler.checkAssertions();
    }

    @Test
    public void testGetObjectAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals(ids.get(0)));
                    assertTrue("Object has unexpected 'city' attribute", content.optString("city").equals("San Francisco"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.getObjectAsync(ids.get(0), handler);
        handler.checkAssertions();
    }

    @Test
    public void testGetObjectWithAttributesToRetrieveAsync() throws Exception {
        List<String> attributesToRetrieve = new ArrayList<String>();
        attributesToRetrieve.add("objectID");
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals(ids.get(0)));
                    assertFalse("Object has unexpected 'city' attribute", content.has("city"));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.getObjectAsync(ids.get(0), attributesToRetrieve, handler);
        handler.checkAssertions();
    }

    @Test
    public void testGetObjectsAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    JSONArray res = content.optJSONArray("results");
                    assertNotNull(res);
                    assertTrue("Object has unexpected objectId", res.optJSONObject(0).optString("objectID").equals(ids.get(0)));
                    assertTrue("Object has unexpected objectId", res.optJSONObject(1).optString("objectID").equals(ids.get(1)));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.getObjectsAsync(ids, handler);
        handler.checkAssertions();
    }

    @Test
    public void testWaitTaskAsync() throws Exception {
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.waitTaskAsync(content.optString("taskID"), new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject content, AlgoliaException error) {
                            if (error == null) {
                                assertEquals(content.optString("status"), "published");
                            } else {
                                fail(error.getMessage());
                            }
                        }
                    });
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), handler);
        handler.checkAssertions();
    }

    @Test
    public void testHostSwitch() throws Exception {
        // Given first host as an unreachable domain
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, "thissentenceshouldbeuniqueenoughtoguaranteeinexistentdomain.com");
        Whitebox.setInternalState(client, "readHosts", hostsArray);

        // Expect a switch to the next URL and successful search
        testSearchAsync();
    }

    @Test
    public void testDNSTimeout() throws Exception {
        // Given first host resulting in a DNS Timeout
        String appId = (String) Whitebox.getInternalState(client, "applicationID");
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, appId + "-dsn.algolia.biz");
        Whitebox.setInternalState(client, "readHosts", hostsArray);
        client.setConnectTimeout(2000);

        //And an index that does not cache search queries
        index.disableSearchCache();


        // Expect successful search within 5 seconds
        long startTime = System.nanoTime();
        testSearchAsync(5);
        final long duration = (System.nanoTime() - startTime) / 1000000;

        // Which should take at least 2 seconds, as per Client.connectTimeout
        assertTrue("We should first timeout before successfully searching, but test took only " + duration + " ms.", duration > 2000);
    }

    @Test
    public void testConnectTimeout() throws AlgoliaException {
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, "notcp-xx-1.algolianet.com");
        Whitebox.setInternalState(client, "readHosts", hostsArray);

        client.setConnectTimeout(1000);
        client.setReadTimeout(1000);

        Long start = System.currentTimeMillis();
        assertNotNull(client.listIndexes());
        assertTrue((System.currentTimeMillis() - start) < 2 * 1000);
    }

    @Test
    public void testMultipleConnectTimeout() throws AlgoliaException {
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, "notcp-xx-1.algolianet.com");
        hostsArray.set(1, "notcp-xx-1.algolianet.com");
        Whitebox.setInternalState(client, "readHosts", hostsArray);

        client.setConnectTimeout(1000);
        client.setReadTimeout(1000);

        Long start = System.currentTimeMillis();
        assertNotNull(client.listIndexes());
        assertTrue((System.currentTimeMillis() - start) < 3 * 1000);
    }


    @Test
    public void testConnectionResetException() throws IOException, AlgoliaException {
        Thread runnable = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(8080);
                    Socket socket = serverSocket.accept();
                    socket.setSoLinger(true, 0);
                    socket.close();
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        };

        runnable.start();

        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, "localhost:8080");
        hostsArray.set(1, "notcp-xx-1.algolianet.com");

        client.setConnectTimeout(1000);
        client.setReadTimeout(1000);

        Long start = System.currentTimeMillis();
        assertNotNull(client.listIndexes());
        long end = System.currentTimeMillis() - start;
        assertTrue(end < 2 * 1000);
    }

    @Test
    public void testSNI() throws Exception {
        // Given all hosts using SNI
        String appId = (String) Whitebox.getInternalState(client, "applicationID");
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, appId + "-1.algolianet.com");
        hostsArray.set(1, appId + "-2.algolianet.com");
        hostsArray.set(2, appId + "-3.algolianet.com");
        hostsArray.set(3, appId + "-3.algolianet.com");
        Whitebox.setInternalState(client, "readHosts", hostsArray);

        // Expect correct certificate handling and successful search
        testSearchAsync();
    }

    @Test
    public void testKeepAlive() throws Exception {
        final int nbTimes = 10;

        // Given all hosts being the same one
        String appId = (String) Whitebox.getInternalState(client, "applicationID");
        List<String> hostsArray = (List<String>) Whitebox.getInternalState(client, "readHosts");
        hostsArray.set(0, appId + "-1.algolianet.com");
        hostsArray.set(1, appId + "-1.algolianet.com");
        hostsArray.set(2, appId + "-1.algolianet.com");
        hostsArray.set(3, appId + "-1.algolianet.com");
        Whitebox.setInternalState(client, "readHosts", hostsArray);

        //And an index that does not cache search queries
        index.disableSearchCache();


        // Expect first successful search
        final long[] startEndTimeArray = new long[2];
        startEndTimeArray[0] = System.nanoTime();
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals(1, content.optInt("nbHits"));
                    startEndTimeArray[1] = System.nanoTime();
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.searchAsync(new Query("Francisco"), handler);

        final long firstDurationNanos = startEndTimeArray[1] - startEndTimeArray[0];
        for (int i = 0; i < nbTimes; i++) {
            startEndTimeArray[0] = System.nanoTime();
            final int finalIter = i;
            AssertCompletionHandler iterationHandler = new AssertCompletionHandler() {
                @Override
                public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                    if (error == null) {
                        startEndTimeArray[1] = System.nanoTime();
                        final long iterDiff = startEndTimeArray[1] - startEndTimeArray[0];
                        final String iterString = String.format("iteration %d: %d < %d", finalIter, iterDiff, firstDurationNanos);

                        // And successful fastest subsequent calls
                        assertEquals(1, content.optInt("nbHits"));
                        assertTrue("Subsequent calls should be fastest than first (" + iterString + ")", startEndTimeArray[1] - startEndTimeArray[0] < firstDurationNanos);
                    } else {
                        fail(error.getMessage());
                    }
                }
            };
            handler.addInnerHandler(iterationHandler);
            index.searchAsync(new Query("Francisco"), iterationHandler);
        }
        handler.checkAssertions();
    }


    private void addDummyObjects(int objectCount) throws Exception {
        // Construct an array of dummy objects.
        objects = new ArrayList<JSONObject>();
        for (int i = 0; i < objectCount; ++i) {
            objects.add(new JSONObject(String.format("{\"dummy\": %d}", i)));
        }

        // Add objects.
        JSONObject task = index.addObjects(new JSONArray(objects));
        index.waitTask(task.getString("taskID"));
    }

    @Test
    public void testBrowseAsync() throws Exception {
        addDummyObjects(1500);

        Query query = new Query().setHitsPerPage(1000);
        final AssertCompletionHandler innerHandler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    String cursor = content.optString("cursor", null);
                    assertNull(cursor);
                } else {
                    fail(error.getMessage());
                }
            }
        };

        final AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    String cursor = content.optString("cursor", null);
                    assertNotNull(cursor);
                    index.browseFromAsync(cursor, innerHandler);
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.browseAsync(query, handler);
        innerHandler.checkAssertions();
        handler.checkAssertions();
    }

    @Test
    public void testClearIndexAsync() throws Exception {
        final AssertCompletionHandler browseHandler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals(content.optInt("nbHits"), 0);
                } else {
                    fail(error.getMessage());
                }
            }
        };
        final AssertCompletionHandler waitTaskHandler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.browseAsync(new Query(), browseHandler);
                } else {
                    fail(error.getMessage());
                }
            }
        };
        final AssertCompletionHandler clearIndexHandler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.waitTaskAsync(content.optString("taskID"), waitTaskHandler);
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.clearIndexAsync(clearIndexHandler);
        clearIndexHandler.checkAssertions();
        waitTaskHandler.checkAssertions();
        browseHandler.checkAssertions();
    }

    @Test
    public void testDeleteByQueryAsync() throws Exception {
        addDummyObjects(3000);
        final Query query = new Query().setNumericFilters(new JSONArray().put("dummy < 1500"));
        final AssertCompletionHandler innerHandler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    // There should not remain any object matching the query.
                    assertNotNull(content.optJSONArray("hits"));
                    assertEquals(content.optJSONArray("hits").length(), 0);
                    assertNull(content.optString("cursor", null));
                } else {
                    fail(error.getMessage());
                }
            }
        };
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.browseAsync(query, innerHandler);
                } else {
                    fail(error.getMessage());
                }
            }
        };
        index.deleteByQueryAsync(query, handler);
        innerHandler.checkAssertions();
        handler.checkAssertions();
    }

    @Test
    public void testError404() throws Exception {
        Index unknownIndex = client.initIndex("doesnotexist");
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(error);
                assertEquals(404, error.getStatusCode());
                assertNotNull(error.getMessage());
            }
        };
        unknownIndex.searchAsync(new Query(), handler);
        handler.checkAssertions();
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
        Client mockClient = mock(Client.class);
        Whitebox.setInternalState(index, "client", mockClient);
        when(mockClient.postRequestRaw(anyString(), anyString(), anyBoolean())).thenReturn("{foo:42}".getBytes());

        // When searching twice separated by waitBetweenSeconds, fires nbTimes requests
        final Query query = new Query("San");
        index.search(query);
        if (waitBetweenSeconds > 0) {
            Thread.sleep(waitBetweenSeconds * 1000);
        }
        index.search(query);
        verify(mockClient, times(nbTimes)).postRequestRaw(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testNullCompletionHandler() throws Exception {
        // Check that the code does not crash when no completion handler is specified.
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), null);
    }

    @Test
    public void testMultipleQueries() throws Exception {
        final List<Query> queries = Arrays.asList(
                new Query("francisco").setHitsPerPage(1),
                new Query("jose")
        );
        AssertCompletionHandler handler = new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                if (error != null) {
                    fail(error.getMessage());
                } else {
                    JSONArray results = content.optJSONArray("results");
                    assertNotNull(results);
                    assertEquals(results.length(), 2);
                    JSONObject results1 = results.optJSONObject(0);
                    assertNotNull(results1);
                    assertEquals(results1.optInt("nbHits"), 1);
                    JSONObject results2 = results.optJSONObject(1);
                    assertNotNull(results2);
                    assertEquals(results2.optBoolean("processed", true), false);
                    assertEquals(results2.optInt("nbHits"), 0);
                }
            }
        };
        index.multipleQueriesAsync(queries, Client.MultipleQueriesStrategy.STOP_IF_ENOUGH_MATCHES, handler);
        handler.checkAssertions();
    }
}
