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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Unit tests for the `MirroredIndex` class.
 */
public class MirroredIndexTest extends OfflineTestBase  {

    /** Higher timeout for online queries. */
    protected static long onlineTimeout = 100;

    /** The current sync listener. */
    private SyncListener listener;

    protected static Map<String, JSONObject> moreObjects = new HashMap<>();
    static {
        try {
            moreObjects.put("snoopy", new JSONObject()
                    .put("objectID", "1")
                    .put("name", "Snoopy")
                    .put("kind", new JSONArray().put("dog").put("animal"))
                    .put("born", 1967)
                    .put("series", "Peanuts")
            );
            moreObjects.put("woodstock", new JSONObject()
                    .put("objectID", "2")
                    .put("name", "Woodstock")
                    .put("kind", new JSONArray().put("bird").put("animal"))
                    .put("born", 1970)
                    .put("series", "Peanuts")
            );
            moreObjects.put("charlie", new JSONObject()
                    .put("objectID", "3")
                    .put("name", "Charlie Brown")
                    .put("kind", new JSONArray().put("human"))
                    .put("born", 1950)
                    .put("series", "Peanuts")
            );
            moreObjects.put("hobbes", new JSONObject()
                    .put("objectID", "4")
                    .put("name", "Hobbes")
                    .put("kind", new JSONArray().put("tiger").put("animal").put("teddy"))
                    .put("born", 1985)
                    .put("series", "Calvin & Hobbes")
            );
            moreObjects.put("calvin", new JSONObject()
                    .put("objectID", "5")
                    .put("name", "Calvin")
                    .put("kind", new JSONArray().put("human"))
                    .put("born", 1985)
                    .put("series", "Calvin & Hobbes")
            );
        }
        catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    interface SyncCompletionHandler {
        void syncCompleted(@Nullable Throwable error);
    }

    private void populate(final @NonNull MirroredIndex index, final @NonNull SyncCompletionHandler completionHandler) {
        // Delete the index.
        client.deleteIndexAsync(index.getIndexName(), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                int taskID = content.optInt("taskID", -1);
                assertNotEquals(-1, taskID);
                index.waitTaskAsync(Integer.toString(taskID), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        // Populate the online index.
                        index.addObjectsAsync(new JSONArray(moreObjects.values()), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                int taskID = content.optInt("taskID", -1);
                                assertNotEquals(-1, taskID);
                                index.waitTaskAsync(Integer.toString(taskID), new AssertCompletionHandler() {
                                    @Override
                                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNull(error);
                                        completionHandler.syncCompleted(error);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void sync(final @NonNull MirroredIndex index, final @NonNull SyncCompletionHandler completionHandler) {
        populate(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                // Sync the offline mirror.
                index.setMirrored(true);
                Query query = new Query();
                query.setNumericFilters(new JSONArray().put("born < 1980"));
                index.setDataSelectionQueries(
                        new MirroredIndex.DataSelectionQuery(query, 10)
                );

                listener = new SyncListener() {
                    @Override
                    public void syncDidStart(MirroredIndex index) {
                        // Nothing to do.
                    }

                    @Override
                    public void syncDidFinish(MirroredIndex index, Throwable error, MirroredIndex.SyncStats stats) {
                        Log.d(MirroredIndexTest.class.getSimpleName(), "Sync finished");
                        index.removeSyncListener(listener);
                        completionHandler.syncCompleted(error);
                    }
                };
                index.addSyncListener(listener);
                index.sync();
            }
        });
    }

    @Test
    public void testSync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final long waitTimeout = 5;

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                try {
                    assertNull(error);

                    // Check that a call to `syncIfNeeded()` does *not* trigger a new sync.
                    listener = new SyncListener() {
                        @Override
                        public void syncDidStart(MirroredIndex index) {
                            fail("The sync should not have been started again");
                        }

                        @Override
                        public void syncDidFinish(MirroredIndex index, Throwable error, MirroredIndex.SyncStats stats) {
                            // Nothing to do.
                        }
                    };
                    final CountDownLatch signal2 = new CountDownLatch(1);
                    index.addSyncListener(listener);
                    index.syncIfNeeded();
                    assertFalse(signal2.await(waitTimeout, TimeUnit.SECONDS));
                    index.removeSyncListener(listener);

                    // Check that changing the data selection queries makes a new sync needed.
                    index.setDataSelectionQueries(new MirroredIndex.DataSelectionQuery(new Query(), 6));
                    listener = new SyncListener() {
                        @Override
                        public void syncDidStart(MirroredIndex index) {
                            // Nothing to do.
                        }

                        @Override
                        public void syncDidFinish(MirroredIndex index, Throwable error, MirroredIndex.SyncStats stats) {
                            index.removeSyncListener(listener);
                            assertNull(error);
                            signal.countDown();
                        }
                    };
                    index.addSyncListener(listener);
                    index.syncIfNeeded();
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    @Test
    public void testSearch() throws Exception {
        final CountDownLatch signal = new CountDownLatch(2);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Query the online index explicitly.
                index.searchOnlineAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertEquals("remote", content.optString("origin"));
                        signal.countDown();
                    }
                });

                // Query the offline index explicitly.
                index.searchOfflineAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(3, content.optInt("nbHits"));
                        assertEquals("local", content.optString("origin"));
                        signal.countDown();
                    }
                });
            }
        });
    }

    @Test
    public void testSearchFallback() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Test offline fallback.
                client.setReadHosts("unknown.algolia.com");
                index.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_FAILURE);
                index.searchAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(3, content.optInt("nbHits"));
                        assertEquals("local", content.optString("origin"));
                        signal.countDown();
                    }
                });
            }
        });
    }

    @Test
    public void testBrowse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(2);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Query the online index explicitly.
                index.browseAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        signal.countDown();
                    }
                });

                // Query the offline index explicitly.
                index.browseMirrorAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(3, content.optInt("nbHits"));
                        signal.countDown();
                    }
                });
            }
        });
    }

    @Test
    public void testBuildOffline() {
        final CountDownLatch signal = new CountDownLatch(4);

        // Retrieve data files from resources.
        File resourcesDir = new File(RuntimeEnvironment.application.getPackageResourcePath() + "/src/testOffline/res");
        File rawDir = new File(resourcesDir, "raw");
        File settingsFile = new File(rawDir, "settings.json");
        File objectFile = new File(rawDir, "objects.json");

        // Create the index.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        index.setMirrored(true);

        // Check that no offline data exists.
        assertFalse(index.hasOfflineData());

        // Build the index.
        index.addBuildListener(new BuildListener() {
            @Override
            public void buildDidStart(@NonNull MirroredIndex index) {
                signal.countDown();
            }

            @Override
            public void buildDidFinish(@NonNull MirroredIndex index, @Nullable Throwable error) {
                assertNull(error);
                signal.countDown();
            }
        });
        index.buildOfflineFromFiles(settingsFile, new File[]{ objectFile }, new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);

                // Check that offline data exists now.
                assertTrue(index.hasOfflineData());

                // Search.
                Query query = new Query().setQuery("peanuts").setFilters("kind:animal");
                index.searchOfflineAsync(query, new AssertCompletionHandler() {
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

    @Test
    public void testGetObject() {
        final CountDownLatch signal = new CountDownLatch(4);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Query the online index explicitly.
                index.getObjectOnlineAsync("1", new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals("Snoopy", content.optString("name"));

                        // Test offline fallback.
                        client.setReadHosts("unknown.algolia.com");
                        index.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_FAILURE);
                        index.getObjectAsync("3", new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                assertEquals("Charlie Brown", content.optString("name"));
                                signal.countDown();
                            }
                        });
                        signal.countDown();
                    }
                });

                // Query the offline index explicitly.
                index.getObjectOfflineAsync("2", new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals("Woodstock", content.optString("name"));
                        signal.countDown();
                    }
                });

                signal.countDown();
            }
        });
    }

    @Test
    public void testGetObjects() {
        final CountDownLatch signal = new CountDownLatch(4);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Query the online index explicitly.
                index.getObjectsOnlineAsync(Arrays.asList("1"), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertNotNull(content.optJSONArray("results"));
                        assertEquals(1, content.optJSONArray("results").length());
                        assertEquals("remote", content.optString("origin"));

                        // Test offline fallback.
                        client.setReadHosts("unknown.algolia.com");
                        index.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_FAILURE);
                        index.getObjectsAsync(Arrays.asList("1", "2", "3"), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                assertNotNull(content.optJSONArray("results"));
                                assertEquals(3, content.optJSONArray("results").length());
                                assertEquals("local", content.optString("origin"));
                                signal.countDown();
                            }
                        });
                        signal.countDown();
                    }
                });

                // Query the offline index explicitly.
                index.getObjectsOfflineAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertNotNull(content.optJSONArray("results"));
                        assertEquals(2, content.optJSONArray("results").length());
                        assertEquals("local", content.optString("origin"));
                        signal.countDown();
                    }
                });

                signal.countDown();
            }
        });
    }

    /**
     * Test the `ONLINE_ONLY` request strategy.
     */
    @Test
    public void testRequestStrategyOnlineOnly() {
        final CountDownLatch signal = new CountDownLatch(1);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        index.setRequestStrategy(MirroredIndex.Strategy.ONLINE_ONLY);
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Test success.
                index.searchAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertEquals("remote", content.optString("origin"));

                        // Test failure.
                        client.setReadHosts("unknown.algolia.com");
                        index.searchAsync(new Query(), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(error);
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Test the `OFFLINE_ONLY` request strategy.
     */
    @Test
    public void testRequestStrategyOfflineOnly() {
        final CountDownLatch signal = new CountDownLatch(1);

        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        index.setMirrored(true);
        index.setRequestStrategy(MirroredIndex.Strategy.OFFLINE_ONLY);

        // Check that a request without local data fails.
        index.searchAsync(new Query(), new AssertCompletionHandler() {
            @Override
            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                assertNotNull(error);

                // Populate the online index & sync the offline mirror.
                sync(index, new SyncCompletionHandler() {
                    @Override
                    public void syncCompleted(@Nullable Throwable error) {
                        assertNull(error);

                        // Test success.
                        index.searchAsync(new Query(), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                assertEquals(3, content.optInt("nbHits"));
                                assertEquals("local", content.optString("origin"));

                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Test the `FALLBACK_ON_FAILURE` request strategy.
     */
    @Test
    public void testRequestStrategyFallbackOnFailure() {
        final CountDownLatch signal = new CountDownLatch(1);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        index.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_FAILURE);
        // Populate the online index & sync the offline mirror.
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Test online success.
                index.searchAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertEquals("remote", content.optString("origin"));

                        // Test network failure.
                        client.setReadHosts("unknown.algolia.com");
                        index.searchAsync(new Query(), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                assertEquals(3, content.optInt("nbHits"));
                                assertEquals("local", content.optString("origin"));
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Test the `FALLBACK_ON_TIMEOUT` request strategy.
     */
    @Test
    public void testRequestStrategyFallbackOnTimeout() {
        final CountDownLatch signal = new CountDownLatch(1);

        // Populate the online index & sync the offline mirror.
        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        index.setRequestStrategy(MirroredIndex.Strategy.FALLBACK_ON_TIMEOUT);
        // Populate the online index & sync the offline mirror.
        sync(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                assertNull(error);

                // Test online success.
                index.searchAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertEquals("remote", content.optString("origin"));

                        // Test network failure.
                        final String timeoutingHost = UUID.randomUUID().toString() + ".algolia.biz";
                        client.setReadHosts(timeoutingHost);
                        final long startTime = System.currentTimeMillis();
                        index.searchAsync(new Query(), new AssertCompletionHandler() {
                            @Override
                            public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                                final long stopTime = System.currentTimeMillis();
                                final long duration = stopTime - startTime;
                                assertNull(error);
                                assertEquals(3, content.optInt("nbHits"));
                                assertEquals("local", content.optString("origin"));
                                // Check that we hit the fallback time out, but not the complete online timeout.
                                // NOTE: Those tests cannot be performed because of Robolectric's single-threaded model.
                                if (false) {
                                    assertTrue(duration >= index.getOfflineFallbackTimeout());
                                    assertTrue(duration < Math.min(client.getSearchTimeout(), client.getReadTimeout()));
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
     * Test that a non-mirrored index behaves like a purely online index.
     */
    @Test
    public void testNotMirrored() {
        final CountDownLatch signal = new CountDownLatch(5);

        final MirroredIndex index = client.getIndex(Helpers.safeIndexName(Helpers.getMethodName()));
        // Check that the index is *not* mirrored by default.
        assertFalse(index.isMirrored());

        populate(index, new SyncCompletionHandler() {
            @Override
            public void syncCompleted(@Nullable Throwable error) {
                // Check that a non-mirrored index returns online results without origin tagging.
                index.searchAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertNull(content.opt("origin"));
                        signal.countDown();
                    }
                });
                index.browseAsync(new Query(), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals(5, content.optInt("nbHits"));
                        assertNull(content.opt("origin"));
                        signal.countDown();
                    }
                });
                index.getObjectAsync("1", new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertEquals("Snoopy", content.optString("name"));
                        assertNull(content.opt("origin"));
                        signal.countDown();
                    }
                });
                index.getObjectsAsync(Arrays.asList("1", "2"), new AssertCompletionHandler() {
                    @Override
                    public void doRequestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        assertNotNull(content.optJSONArray("results"));
                        assertEquals(2, content.optJSONArray("results").length());
                        assertNull(content.opt("origin"));
                        signal.countDown();
                    }
                });
                signal.countDown();
            }
        });
    }
}
