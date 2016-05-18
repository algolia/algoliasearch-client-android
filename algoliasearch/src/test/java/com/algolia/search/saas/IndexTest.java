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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    public void testSearchAsync(int waitTimeout) throws Exception {
        // Empty search.
        final CountDownLatch signal = new CountDownLatch(1);
        index.searchAsync(new Query(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Result length does not match nbHits", objects.size(), content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));

        // Search with query.
        final CountDownLatch signal2 = new CountDownLatch(1);
        index.searchAsync(new Query("Francisco"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals(1, content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
                signal2.countDown();
            }
        });
        assertTrue("No callback was called", signal2.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testSearchDisjunctiveFacetingAsync() throws Exception {
        // Set index settings.
        JSONObject setSettingsResult = index.setSettings(new JSONObject("{\"attributesForFaceting\": [\"brand\", \"category\"]}"));
        index.waitTask(setSettingsResult.getString("taskID"));

        // Empty query
        // -----------
        // Not very useful, but we have to check this edge case.
        final CountDownLatch signal = new CountDownLatch(1);
        index.searchDisjunctiveFacetingAsync(new Query(), new ArrayList<String>(), new HashMap<String, List<String>>(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Result length does not match nbHits", objects.size(), content.optInt("nbHits"));
                } else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));

        // "Real" query
        // ------------
        final CountDownLatch signal2 = new CountDownLatch(1);
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

        final Query query = new Query("phone");
        query.setFacets("brand", "category", "stars");
        final List<String> disjunctiveFacets = Arrays.asList("brand");
        final Map<String, List<String>> refinements = new HashMap<>();
        refinements.put("brand", Arrays.asList("Apple", "Samsung")); // disjunctive facet
        refinements.put("category", Arrays.asList("device")); // conjunctive facet
        index.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
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
                signal2.countDown();
            }
        });
        assertTrue("No callback was called", signal2.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testDisjunctiveFaceting() throws Exception {
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
        JSONObject answer;

        answer = index.searchDisjunctiveFaceting(query, disjunctiveFacets, refinements);
        assertEquals(5, answer.getInt("nbHits"));
        assertEquals(1, answer.getJSONObject("facets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").length());

        refinements.put("stars", Arrays.asList("*"));
        answer = index.searchDisjunctiveFaceting(query, disjunctiveFacets, refinements);
        assertEquals(2, answer.getInt("nbHits"));
        assertEquals(1, answer.getJSONObject("facets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("*"));
        assertEquals(1, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("**"));
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("****"));

        refinements.put("city", Arrays.asList("Paris"));
        answer = index.searchDisjunctiveFaceting(query, disjunctiveFacets, refinements);
        assertEquals(2, answer.getInt("nbHits"));
        assertEquals(1, answer.getJSONObject("facets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("*"));
        assertEquals(1, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("****"));

        refinements.put("stars", Arrays.asList("*", "****"));
        answer = index.searchDisjunctiveFaceting(query, disjunctiveFacets, refinements);
        assertEquals(3, answer.getInt("nbHits"));
        assertEquals(1, answer.getJSONObject("facets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").length());
        assertEquals(2, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("*"));
        assertEquals(1, answer.getJSONObject("disjunctiveFacets").getJSONObject("stars").getInt("****"));
    }

    @Test
    public void testAddObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertNotNull("Result has no objectId", content.optString("objectID", null));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testAddObjectWithObjectIDAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals("a1b2c3"));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testAddObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.addObjectsAsync(new JSONArray("[{\"city\": \"New York\"}, {\"city\": \"Paris\"}]"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Objects have unexpected objectId count", 2, content.optJSONArray("objectIDs").length());
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testSaveObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.saveObjectAsync(new JSONObject("{\"city\": \"New York\"}"), "a1b2c3", new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals("a1b2c3"));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testSaveObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.saveObjectsAsync(new JSONArray("[{\"city\": \"New York\", \"objectID\": 123}, {\"city\": \"Paris\", \"objectID\": 456}]"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertEquals("Objects have unexpected objectId count", 2, content.optJSONArray("objectIDs").length());
                    assertEquals("Object has unexpected objectId", 123, content.optJSONArray("objectIDs").optInt(0));
                    assertEquals("Object has unexpected objectId", 456, content.optJSONArray("objectIDs").optInt(1));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testGetObjectAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.getObjectAsync(ids.get(0), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals(ids.get(0)));
                    assertTrue("Object has unexpected 'city' attribute", content.optString("city").equals("San Francisco"));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testGetObjectWithAttributesToRetrieveAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        List<String> attributesToRetrieve = new ArrayList<String>();
        attributesToRetrieve.add("objectID");
        index.getObjectAsync(ids.get(0), attributesToRetrieve, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    assertTrue("Object has unexpected objectId", content.optString("objectID").equals(ids.get(0)));
                    assertFalse("Object has unexpected 'city' attribute", content.has("city"));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testGetObjectsAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        index.getObjectsAsync(ids, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    JSONArray res = content.optJSONArray("results");
                    assertNotNull(res);
                    assertTrue("Object has unexpected objectId", res.optJSONObject(0).optString("objectID").equals(ids.get(0)));
                    assertTrue("Object has unexpected objectId", res.optJSONObject(1).optString("objectID").equals(ids.get(1)));
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testWaitTaskAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(2);
        index.addObjectAsync(new JSONObject("{\"city\": \"New York\"}"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.waitTaskAsync(content.optString("taskID"), new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject content, AlgoliaException error) {
                            if (error == null) {
                                assertEquals(content.optString("status"), "published");
                            }
                            else {
                                fail(error.getMessage());
                            }
                            signal.countDown();
                        }
                    });
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
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

        final CountDownLatch signal = new CountDownLatch(2);
        Query query = new Query();
        query.setHitsPerPage(1000);
        index.browseAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    String cursor = content.optString("cursor", null);
                    assertNotNull(cursor);
                    index.browseFromAsync(cursor, new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject content, AlgoliaException error) {
                            if (error == null) {
                                String cursor = content.optString("cursor", null);
                                assertNull(cursor);
                            } else {
                                fail(error.getMessage());
                            }
                            signal.countDown();
                        }
                    });
                } else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testClearIndexAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(3);
        index.clearIndexAsync(new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.waitTaskAsync(content.optString("taskID"), new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject content, AlgoliaException error) {
                            if (error == null) {
                                index.browseAsync(new Query(), new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        if (error == null) {
                                            assertEquals(content.optInt("nbHits"), 0);
                                        } else {
                                            fail(error.getMessage());
                                        }
                                        signal.countDown();
                                    }
                                });
                            }
                            else {
                                fail(error.getMessage());
                            }
                            signal.countDown();
                        }
                    });
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testDeleteByQueryAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(2);
        addDummyObjects(3000);
        final Query query = new Query();
        query.set("numericFilters", "dummy < 1500");
        index.deleteByQueryAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (error == null) {
                    index.browseAsync(query, new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject content, AlgoliaException error) {
                            if (error == null) {
                                // There should not remain any object matching the query.
                                assertNotNull(content.optJSONArray("hits"));
                                assertEquals(content.optJSONArray("hits").length(), 0);
                                assertNull(content.optString("cursor", null));
                            }
                            else {
                                fail(error.getMessage());
                            }
                            signal.countDown();
                        }
                    });
                }
                else {
                    fail(error.getMessage());
                }
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(Helpers.wait, TimeUnit.SECONDS));
    }

    @Test
    public void testError404() throws Exception {
        Index unknownIndex = client.initIndex("doesnotexist");
        unknownIndex.searchAsync(new Query(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(error);
                assertEquals(404, error.getStatusCode());
                assertNotNull(error.getMessage());
            }
        });
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
}
