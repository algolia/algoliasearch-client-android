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
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectAsync(objects.get("snoopy"), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertEquals("1", content.optString("objectID", null));
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.getObjectAsync("1", new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals("Snoopy", content.optString("name", null));
                                final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                                transaction.deleteObjectAsync("1", new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(content);
                                        transaction.commitAsync(new AssertCompletionHandler() {
                                            @Override
                                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                                assertNull(error);
                                                index.getObjectAsync("1", new AssertCompletionHandler() {
                                                    @Override
                                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testSaveGetDeleteObjectSync() throws Exception {
        // NOTE: We are not supposed to call synchronous methods from the main thread, but since Robolectric emuates
        // all thread onto the main thread anyway, why bother?
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        try {
            final OfflineIndex.WriteTransaction transaction = index.newTransaction();
            transaction.saveObjectSync(objects.get("snoopy"));
            transaction.commitSync();
            index.getObjectAsync("1", new AssertCompletionHandler() {
                @Override
                public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                    assertNotNull(content);
                    assertEquals("Snoopy", content.optString("name", null));
                    try {
                        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                        transaction.deleteObjectSync("1");
                        transaction.commitSync();
                        index.getObjectAsync("1", new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
        } catch (AlgoliaException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSaveGetDeleteObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertNotNull(content.optJSONArray("objectIDs"));
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.getObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                JSONArray results = content.optJSONArray("results");
                                assertNotNull(results);
                                assertEquals(2, results.length());
                                assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                                assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                                final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                                transaction.deleteObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(content);
                                        assertNotNull(content.optJSONArray("objectIDs"));
                                        transaction.commitAsync(new AssertCompletionHandler() {
                                            @Override
                                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                                assertNull(error);
                                                index.getObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                                                    @Override
                                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testSaveGetDeleteObjectsSync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        try {
            final OfflineIndex.WriteTransaction transaction = index.newTransaction();
            transaction.saveObjectsSync(new JSONArray(objects.values()));
            transaction.commitSync();
            index.getObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                @Override
                public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                    assertNotNull(content);
                    JSONArray results = content.optJSONArray("results");
                    assertNotNull(results);
                    assertEquals(2, results.length());
                    assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                    assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                    try {
                        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                        transaction.deleteObjectsSync(Arrays.asList("1", "2"));
                        transaction.commitSync();
                        index.getObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
        } catch (AlgoliaException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSearch() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        final Query query = new Query("snoopy");
                        index.searchAsync(query, new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testGetSetSettings() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final JSONObject settings = new JSONObject().put("attributesToIndex", new JSONArray().put("foo").put("bar"));
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.setSettingsAsync(settings, new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        index.getSettingsAsync(new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testClear() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                        transaction.clearIndexAsync(new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                transaction.commitAsync(new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                        index.browseAsync(new Query(), new AssertCompletionHandler() {
                                            @Override
                                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testBrowse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.browseAsync(new Query().setHitsPerPage(1), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                String cursor = content.optString("cursor", null);
                                assertNotNull(cursor);
                                index.browseFromAsync(cursor, new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
                        index.deleteByQueryAsync(new Query().setNumericFilters(new JSONArray().put("born < 1970")), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                transaction.commitAsync(new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                        index.browseAsync(new Query(), new AssertCompletionHandler() {
                                            @Override
                                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    @Test
    public void testMultipleQueries() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.saveObjectsAsync(new JSONArray(objects.values()), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                transaction.commitAsync(new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        index.multipleQueriesAsync(Arrays.asList(new Query("snoopy"), new Query("woodstock")), null, new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
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
    }

    /**
     * Test adding more objects than the size of the internal in-memory buffer.
     */
    @Test
    public void testAddManyObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        // Artifically reduce the in-memory buffer size.
        Whitebox.setInternalState(transaction, "maxObjectsInMemory", 25);
        int objectCount = 0;
        for (int i = 0; i < 7; ++i) {
            List<JSONObject> objects = new ArrayList<>();
            for (int j = 0; j < 13; ++j) {
                objects.add(new JSONObject().put("objectID", Integer.toString(++objectCount)));
            }
            transaction.saveObjectsSync(new JSONArray(objects));
        }
        transaction.commitSync();
        assertTrue(objectCount <= 100); // required for our limited license key to work
        final int finalObjectCount = objectCount;
        index.browseAsync(new Query().setHitsPerPage(1000), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertEquals(finalObjectCount, content.optInt("nbHits"));
                assertNull(content.opt("cursor"));
                signal.countDown();
            }
        });
    }

    @Test
    public void testRollback() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final String indexName = Helpers.getMethodName();
        final OfflineIndex index = client.getOfflineIndex(indexName);
        final OfflineIndex.WriteTransaction transaction1 = index.newTransaction();
        transaction1.saveObjectsSync(new JSONArray(objects.values()));
        transaction1.rollbackSync();
        assertFalse(client.hasOfflineData(indexName));

        final OfflineIndex.WriteTransaction transaction2 = index.newTransaction();
        transaction2.saveObjectSync(objects.get("snoopy"));
        transaction2.commitSync();
        assertTrue(client.hasOfflineData(indexName));

        final OfflineIndex.WriteTransaction transaction3 = index.newTransaction();
        transaction3.saveObjectSync(objects.get("woodstock"));
        transaction3.rollbackSync();

        index.browseAsync(new Query(), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(content);
                assertEquals(1, content.optInt("nbHits"));
                assertNull(content.optString("cursor", null));
                signal.countDown();
            }
        });
    }

    /**
     * Test that we can chain async write operations without waiting for the handler to be called, and that it
     * still works.
     * */
    @Test
    public void testAsyncUpdatesInParallel() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex.WriteTransaction transaction = index.newTransaction();
        transaction.clearIndexAsync(null);
        transaction.saveObjectAsync(objects.get("snoopy"), null);
        transaction.saveObjectAsync(objects.get("woodstock"), null);
        transaction.deleteObjectAsync("1", null);
        transaction.setSettingsAsync(new JSONObject(), null);
        transaction.commitAsync(new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.browseAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals(1, content.optInt("nbHits"));
                        assertNull(content.optString("cursor", null));
                        assertEquals("Woodstock", content.optJSONArray("hits").optJSONObject(0).optString("name"));
                        signal.countDown();
                    }
                });
            }
        });
    }

    @Test
    public void testBuild() {
        final CountDownLatch signal = new CountDownLatch(2);

        // Retrieve data files from resources.
        File resourcesDir = new File(RuntimeEnvironment.application.getPackageResourcePath() + "/src/testOffline/res");
        File rawDir = new File(resourcesDir, "raw");
        File settingsFile = new File(rawDir, "settings.json");
        File objectFile = new File(rawDir, "objects.json");

        // Create the index.
        final OfflineIndex index = client.getOfflineIndex(Helpers.safeIndexName(Helpers.getMethodName()));

        // Check that no offline data exists.
        assertFalse(index.hasOfflineData());

        // Build the index.
        index.buildFromFiles(settingsFile, new File[]{ objectFile }, new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);

                // Check that offline data exists now.
                assertTrue(index.hasOfflineData());

                // Search.
                Query query = new Query().setQuery("peanuts").setFilters("kind:animal");
                index.searchAsync(query, new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(2, content.optInt("nbHits"));
                        signal.countDown();
                    }
                });
                signal.countDown();
            }
        });
    }
}