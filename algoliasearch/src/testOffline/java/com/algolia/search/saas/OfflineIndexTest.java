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
import org.mockito.internal.util.reflection.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Unit tests for the `OfflineIndex` class.
 */
public class OfflineIndexTest extends OfflineTestBase  {
    @Test
    public void testSaveGetDeleteObject() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        index.saveObjectAsync(objects.get("snoopy"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optString("updatedAt", null));
                assertEquals("1", content.optString("objectID", null));
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.getObjectAsync("1", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals("Snoopy", content.optString("name", null));
                                index.beginTransaction();
                                index.deleteObjectAsync("1", new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(content);
                                        assertNotNull(content.optString("deletedAt", null));
                                        index.commitTransactionAsync(new CompletionHandler() {
                                            @Override
                                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                                assertNull(error);
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
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testSaveGetDeleteObjectSync() throws Exception {
        // NOTE: We are not supposed to call synchronous methods from the main thread, but since Robolectric emuates
        // all thread onto the main thread anyway, why bother?
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        try {
            index.beginTransaction();
            index.saveObjectSync(objects.get("snoopy"));
            index.commitTransactionSync();
            index.getObjectAsync("1", new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    assertNotNull(content);
                    assertEquals("Snoopy", content.optString("name", null));
                    try {
                        index.beginTransaction();
                        index.deleteObjectSync("1");
                        index.commitTransactionSync();
                        index.getObjectAsync("1", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(error);
                                assertEquals(404, error.getStatusCode());
                                signal.countDown();
                            }
                        });
                    } catch (AlgoliaException e) {
                        fail(e.getMessage());
                    }
                }
            });
            assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
        } catch (AlgoliaException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSaveGetDeleteObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optJSONArray("objectIDs"));
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                JSONArray results = content.optJSONArray("results");
                                assertNotNull(results);
                                assertEquals(2, results.length());
                                assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                                assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                                index.beginTransaction();
                                index.deleteObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(content);
                                        assertNotNull(content.optJSONArray("objectIDs"));
                                        index.commitTransactionAsync(new CompletionHandler() {
                                            @Override
                                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                                assertNull(error);
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
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testSaveGetDeleteObjectsSync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        try {
            index.beginTransaction();
            index.saveObjectsSync(objects.values());
            index.commitTransactionSync();
            index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    assertNotNull(content);
                    JSONArray results = content.optJSONArray("results");
                    assertNotNull(results);
                    assertEquals(2, results.length());
                    assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                    assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                    try {
                        index.beginTransaction();
                        index.deleteObjectsSync(Arrays.asList("1", "2"));
                        index.commitTransactionSync();
                        index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(error);
                                assertEquals(404, error.getStatusCode());
                                signal.countDown();
                            }
                        });
                    } catch (AlgoliaException e) {
                        fail(e.getMessage());
                    }
                }
            });
            assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
        } catch (AlgoliaException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSearch() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
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
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testGetSetSettings() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final JSONObject settings = new JSONObject().put("attributesToIndex", new JSONArray().put("foo").put("bar"));
        index.beginTransaction();
        index.setSettingsAsync(settings, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
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
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testClear() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.beginTransaction();
                        index.clearIndexAsync(new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNotNull(content.optString("updatedAt", null));
                                index.commitTransactionAsync(new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
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
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.commitTransactionAsync(new CompletionHandler() {
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
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.beginTransaction();
                        index.deleteByQueryAsync(new Query().setNumericFilters(new JSONArray().put("born < 1970")), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                index.commitTransactionAsync(new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
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
        index.beginTransaction();
        index.saveObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.commitTransactionAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
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
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    /**
     * Test adding more objects than the size of the internal in-memory buffer.
     */
    @Test
    public void testAddManyObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.beginTransaction();
        // Artifically reduce the in-memory buffer size.
        Object transaction = Whitebox.getInternalState(index, "transaction");
        Whitebox.setInternalState(transaction, "maxObjectsInMemory", 25);
        int objectCount = 0;
        for (int i = 0; i < 7; ++i) {
            List<JSONObject> objects = new ArrayList<>();
            for (int j = 0; j < 13; ++j) {
                objects.add(new JSONObject().put("objectID", Integer.toString(++objectCount)));
            }
            index.saveObjectsSync(objects);
        }
        index.commitTransactionSync();
        assertTrue(objectCount <= 100); // required for our limited license key to work
        final int finalObjectCount = objectCount;
        index.browseAsync(new Query().setHitsPerPage(1000), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertEquals(finalObjectCount, content.optInt("nbHits"));
                assertNull(content.opt("cursor"));
                signal.countDown();
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }
}