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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for the `OfflineIndex` class.
 */
public class OfflineIndexTest extends OfflineTestBase  {
    @Test
    public void testAddGetDeleteObject() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectAsync(objects.get("snoopy"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optString("updatedAt", null));
                assertEquals("1", content.optString("objectID", null));
                index.getObjectAsync("1", new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals("Snoopy", content.optString("name", null));
                        index.deleteObjectAsync("1", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNotNull(content.optString("deletedAt", null));
                                index.getObjectAsync("1", new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testAddWithIDGetDeleteObject() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectAsync(new JSONObject().put("name", "unknown"), "xxx", new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optString("updatedAt", null));
                assertEquals("xxx", content.optString("objectID", null));
                index.getObjectAsync("xxx", new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals("unknown", content.optString("name", null));
                        index.deleteObjectAsync("xxx", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNotNull(content.optString("deletedAt", null));
                                index.getObjectAsync("xxx", new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testAddGetDeleteObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optJSONArray("objectIDs"));
                index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        JSONArray results = content.optJSONArray("results");
                        assertNotNull(results);
                        assertEquals(2, results.length());
                        assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                        assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                        index.deleteObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNotNull(content.optJSONArray("objectIDs"));
                                index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testSearch() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                final Query query = new Query("snoopy");
                index.searchAsync(query, new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals(1, content.optInt("nbHits"));
                        JSONArray hits = content.optJSONArray("hits");
                        assertNotNull(hits);
                        assertEquals(1, hits.length());
                        assertEquals("Snoopy", hits.optJSONObject(0).optString("name"));
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testGetSetSettings() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final JSONObject settings = new JSONObject().put("attributesToIndex", new JSONArray().put("foo").put("bar"));
        index.setSettingsAsync(settings, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.getSettingsAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals(settings.optJSONArray("attributesToIndex"), content.optJSONArray("attributesToIndex"));
                        assertNull(settings.opt("attributesToRetrieve"));
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testClear() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                index.clearIndexAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertNotNull(content.optString("updatedAt", null));
                        index.browseAsync(new Query(), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals(0, content.optInt("nbHits", 666));
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testBrowse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.browseAsync(new Query().setHitsPerPage(1), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        String cursor = content.optString("cursor", null);
                        assertNotNull(cursor);
                        index.browseFromAsync(cursor, new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNull(content.optString("cursor", null));
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.deleteByQueryAsync(new Query().setNumericFilters(new JSONArray().put("born < 1970")), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.browseAsync(new Query(), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals(1, content.optInt("nbHits"));
                                assertNull(content.opt("cursor"));
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleQueries() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.multipleQueriesAsync(Arrays.asList(new Query("snoopy"), new Query("woodstock")), null, new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        JSONArray results = content.optJSONArray("results");
                        assertNotNull(results);
                        assertEquals(2, results.length());
                        for (int i = 0; i < results.length(); ++i) {
                            JSONObject result = results.optJSONObject(i);
                            assertNotNull(result);
                            assertEquals(1, result.optInt("nbHits"));
                        }
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }
}