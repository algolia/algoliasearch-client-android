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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Emulates multiple queries from individual queries.
 */
abstract class MultipleQueryEmulator {
    private final String indexName;

    public MultipleQueryEmulator(@NonNull String indexName) {
        this.indexName = indexName;
    }

    abstract protected JSONObject singleQuery(@NonNull Query query) throws AlgoliaException;

    public JSONObject multipleQueries(@NonNull List<Query> queries, @Nullable String strategy) throws AlgoliaException {
        try {
            JSONArray results = new JSONArray();
            boolean shouldProcess = true;
            for (Query query: queries) {
                // Implement the "stop if enough matches" strategy.
                if (!shouldProcess) {
                    JSONObject returnedContent = new JSONObject()
                            .put("hits", new JSONArray())
                            .put("page", 0)
                            .put("nbHits", 0)
                            .put("nbPages", 0)
                            .put("hitsPerPage", 0)
                            .put("processingTimeMS", 1)
                            .put("params", query.build())
                            .put("index", indexName)
                            .put("processed", false);
                    results.put(returnedContent);
                    continue;
                }

                JSONObject returnedContent = singleQuery(query);
                returnedContent.put("index", indexName);
                results.put(returnedContent);

                // Implement the "stop if enough matches strategy".
                if (strategy != null && strategy.equals(Client.MultipleQueriesStrategy.STOP_IF_ENOUGH_MATCHES.toString())) {
                    int nbHits = returnedContent.getInt("nbHits");
                    int hitsPerPage = returnedContent.getInt("hitsPerPage");
                    if (nbHits >= hitsPerPage) {
                        shouldProcess = false;
                    }
                }
            }
            return new JSONObject()
                .put("results", results)
                .put(MirroredIndex.JSON_KEY_ORIGIN, MirroredIndex.JSON_VALUE_ORIGIN_LOCAL);
        }
        catch (JSONException e) {
            // The `put()` calls should never throw, but the `getInt()` calls may if individual queries return
            // unexpected results.
            throw new AlgoliaException("When running multiple queries", e);
        }
    }
}
