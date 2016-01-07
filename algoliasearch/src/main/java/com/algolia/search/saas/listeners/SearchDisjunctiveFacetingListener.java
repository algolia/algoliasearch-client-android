package com.algolia.search.saas.listeners;

import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.AlgoliaException;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Asynchronously receive result of search method
 */
public interface SearchDisjunctiveFacetingListener {
    /**
     * Asynchronously receive result of Index.searchASync method.
     */
    void searchDisjunctiveFacetingResult(Index index, Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements, JSONObject results);

    /**
     * Asynchronously receive error of Index.searchASync method.
     */
    void searchDisjunctiveFacetingError(Index index, Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements, AlgoliaException e);
}