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

package com.algolia.search.saas.helpers;


import android.support.annotation.NonNull;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Disjunctive faceting helper.
 */
public abstract class DisjunctiveFaceting {

    /**
     * Run multiple queries. To be implemented by subclasses. The contract is the same as {@see Index#multipleQueriesAsync}.
     *
     * @param queries Queries to run.
     * @param completionHandler Completion handler to be notified of results.
     * @return A cancellable request.
     */
    abstract protected Request multipleQueriesAsync(@NonNull Collection<Query> queries, @NonNull CompletionHandler completionHandler);

    /**
     * Perform a search with disjunctive facets, generating as many queries as number of disjunctive facets.
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public <T extends Collection<String>> Request searchDisjunctiveFacetingAsync(@NonNull Query query, @NonNull final Collection<String> disjunctiveFacets, @NonNull final Map<String, T> refinements, @NonNull final CompletionHandler completionHandler) {
        final List<Query> queries = computeDisjunctiveFacetingQueries(query, disjunctiveFacets, refinements);
        return multipleQueriesAsync(queries, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                JSONObject aggregatedResults = null;
                try {
                    if (content != null) {
                        aggregatedResults = aggregateDisjunctiveFacetingResults(content, disjunctiveFacets, refinements);
                    }
                } catch (AlgoliaException e) {
                    error = e;
                }
                completionHandler.requestCompleted(aggregatedResults, error);
            }
        });
    }

    /**
     * Filter disjunctive refinements from generic refinements and a list of disjunctive facets.
     *
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements       Map representing the current refinements
     * @return The disjunctive refinements
     */
    static private @NonNull <T extends Collection<String>> Map<String, T> filterDisjunctiveRefinements(@NonNull Collection<String> disjunctiveFacets, @NonNull Map<String, T> refinements)
    {
        Map<String, T> disjunctiveRefinements = new HashMap<>();
        for (Map.Entry<String, T> elt : refinements.entrySet()) {
            if (disjunctiveFacets.contains(elt.getKey())) {
                disjunctiveRefinements.put(elt.getKey(), elt.getValue());
            }
        }
        return disjunctiveRefinements;
    }

    /**
     * Compute the queries to run to implement disjunctive faceting.
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @return A list of queries suitable for {@link Index#multipleQueries}.
     */
    static private @NonNull <T extends Collection<String>> List<Query> computeDisjunctiveFacetingQueries(@NonNull Query query, @NonNull Collection<String> disjunctiveFacets, @NonNull Map<String, T> refinements) {
        // Retain only refinements corresponding to the disjunctive facets.
        Map<String, ? extends Collection<String>> disjunctiveRefinements = filterDisjunctiveRefinements(disjunctiveFacets, refinements);

        // build queries
        List<Query> queries = new ArrayList<>();

        // first query: hits + regular facets
        JSONArray facetFilters = new JSONArray();
        for (Map.Entry<String, T> elt : refinements.entrySet()) {
            JSONArray orFilters = new JSONArray();

            for (String val : elt.getValue()) {
                // When already refined facet, or with existing refinements
                if (disjunctiveRefinements.containsKey(elt.getKey())) {
                    orFilters.put(formatFilter(elt, val));
                } else {
                    facetFilters.put(formatFilter(elt, val));
                }
            }
            // Add or
            if (disjunctiveRefinements.containsKey(elt.getKey())) {
                facetFilters.put(orFilters);
            }
        }

        queries.add(new Query(query).setFacetFilters(facetFilters));
        // one query per disjunctive facet (use all refinements but the current one + hitsPerPage=1 + single facet
        for (String disjunctiveFacet : disjunctiveFacets) {
            facetFilters = new JSONArray();
            for (Map.Entry<String, T> elt : refinements.entrySet()) {
                if (disjunctiveFacet.equals(elt.getKey())) {
                    continue;
                }
                JSONArray orFilters = new JSONArray();
                for (String val : elt.getValue()) {
                    if (disjunctiveRefinements.containsKey(elt.getKey())) {
                        orFilters.put(formatFilter(elt, val));
                    } else {
                        facetFilters.put(formatFilter(elt, val));
                    }
                }
                // Add or
                if (disjunctiveRefinements.containsKey(elt.getKey())) {
                    facetFilters.put(orFilters);
                }
            }
            String[] facets = new String[]{disjunctiveFacet};
            queries.add(new Query(query).setHitsPerPage(0).setAnalytics(false)
                    .setAttributesToRetrieve().setAttributesToHighlight().setAttributesToSnippet()
                    .setFacets(facets).setFacetFilters(facetFilters));
        }
        return queries;
    }

    private static <T extends Collection<String>> String formatFilter(Map.Entry<String, T> refinement, String value) {
        return String.format("%s:%s", refinement.getKey(), value);
    }

    /**
     * Aggregate results from multiple queries into disjunctive faceting results.
     *
     * @param answers The response from the multiple queries.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements Facet refinements.
     * @return The aggregated results.
     * @throws AlgoliaException
     */
    static private <T extends Collection<String>> JSONObject aggregateDisjunctiveFacetingResults(@NonNull JSONObject answers, @NonNull Collection<String> disjunctiveFacets, @NonNull Map<String, T> refinements) throws AlgoliaException
    {
        Map<String, T> disjunctiveRefinements = filterDisjunctiveRefinements(disjunctiveFacets, refinements);

        // aggregate answers
        // first answer stores the hits + regular facets
        try {
            boolean nonExhaustiveFacetsCount = false;
            JSONArray results = answers.getJSONArray("results");
            JSONObject aggregatedAnswer = results.getJSONObject(0);
            JSONObject disjunctiveFacetsJSON = new JSONObject();
            for (int i = 1; i < results.length(); ++i) {
                if (!results.getJSONObject(i).optBoolean("exhaustiveFacetsCount")) {
                    nonExhaustiveFacetsCount = true;
                }
                JSONObject facets = results.getJSONObject(i).getJSONObject("facets");
                @SuppressWarnings("unchecked")
                Iterator<String> keys = facets.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    // Add the facet to the disjunctive facet hash
                    disjunctiveFacetsJSON.put(key, facets.getJSONObject(key));
                    // concatenate missing refinements
                    if (!disjunctiveRefinements.containsKey(key)) {
                        continue;
                    }
                    for (String refine : disjunctiveRefinements.get(key)) {
                        if (!disjunctiveFacetsJSON.getJSONObject(key).has(refine)) {
                            disjunctiveFacetsJSON.getJSONObject(key).put(refine, 0);
                        }
                    }
                }
            }
            aggregatedAnswer.put("disjunctiveFacets", disjunctiveFacetsJSON);
            if (nonExhaustiveFacetsCount) {
                aggregatedAnswer.put("exhaustiveFacetsCount", false);
            }
            return aggregatedAnswer;
        } catch (JSONException e) {
            throw new AlgoliaException("Failed to aggregate results", e);
        }
    }
}
