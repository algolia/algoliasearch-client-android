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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Abstract class for Index methods
 */
abstract class BaseIndex {
    private APIClient client;
    private String encodedIndexName;
    private String indexName;
    private final long MAX_TIME_MS_TO_WAIT = 10000L;

    /**
     * Index initialization (You should not call this initialized yourself)
     */
    protected BaseIndex(APIClient client, String indexName) {
        try {
            this.client = client;
            this.encodedIndexName = URLEncoder.encode(indexName, "UTF-8");
            this.indexName = indexName;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString()
    {
        return String.format("%s{%s}", this.getClass().getSimpleName(), getIndexName());
    }

    public String getIndexName() {
        return indexName;
    }

    public APIClient getClient()
    {
        return client;
    }

    protected String getEncodedIndexName()
    {
        return encodedIndexName;
    }

    /**
     * Add an object in this index
     *
     * @param obj the object to add.
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj) throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName, obj.toString(), false);
    }

    /**
     * Add an object in this index
     *
     * @param obj the object to add.
     * @param objectID an objectID you want to attribute to this object
     * (if the attribute already exist the old object will be overwrite)
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), obj.toString());
        } catch (UnsupportedEncodingException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @throws AlgoliaException
     */
    protected JSONObject batch(JSONArray actions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("requests", actions);
            return client.postRequest("/1/indexes/" + encodedIndexName + "/batch", content.toString(), false);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Add several objects
     *
     * @param inputArray contains an array of objects to add.
     * @throws AlgoliaException
     */
    protected JSONObject addObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body", inputArray.getJSONObject(n));
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID the unique identifier of the object to retrieve
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID) throws AlgoliaException {
        try {
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve contains the list of attributes to retrieve.
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID,  List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            StringBuilder params = new StringBuilder();
            params.append("?attributes=");
            for (int i = 0; i < attributesToRetrieve.size(); ++i) {
                if (i > 0) {
                    params.append(",");
                }
                params.append(URLEncoder.encode(attributesToRetrieve.get(i), "UTF-8"));
            }
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + params.toString(), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get several objects from this index
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    protected JSONObject getObjects(List<String> objectIDs) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (String id : objectIDs) {
                JSONObject request = new JSONObject();
                request.put("indexName", this.indexName);
                request.put("objectID", id);
                requests.put(request);
            }
            JSONObject body = new JSONObject();
            body.put("requests", requests);
            return client.postRequest("/1/indexes/*/objects", body.toString(), true);
        } catch (JSONException e){
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Update partially an object (only update attributes passed in argument)
     *
     * @param partialObject the object attributes to override
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObject(JSONObject partialObject, String objectID) throws AlgoliaException {
        try {
            return client.postRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial", partialObject.toString(), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Partially Override the content of several objects
     *
     * @param inputArray the array of objects to update (each object must contains an objectID attribute)
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
                JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "partialUpdateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Override the content of object
     *
     * @param object the object to save
     * @throws AlgoliaException
     */
    protected JSONObject saveObject(JSONObject object, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), object.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of several objects
     *
     * @param inputArray contains an array of objects to update (each object must contains an objectID attribute)
     * @throws AlgoliaException
     */
    protected JSONObject saveObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
                JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "updateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete an object from the index
     *
     * @param objectID the unique identifier of object to delete
     * @throws AlgoliaException
     */
    protected JSONObject deleteObject(String objectID) throws AlgoliaException {
        if (objectID.length() == 0) {
            throw new AlgoliaException("Invalid objectID");
        }
        try {
            return client.deleteRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete several objects
     *
     * @param objects the array of objectIDs to delete
     * @throws AlgoliaException
     */
    protected JSONObject deleteObjects(List<String> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (String id : objects) {
                JSONObject obj = new JSONObject();
                obj.put("objectID", id);
                JSONObject action = new JSONObject();
                action.put("action", "deleteObject");
                action.put("body",obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete all objects matching a query
     *
     * @param query the query string
     * @throws AlgoliaException
     */
    protected void deleteByQuery(Query query) throws AlgoliaException {
        query.setAttributesToRetrieve("objectID");
        query.setHitsPerPage(100);

        JSONObject results = this.search(query);
        try {
            while (results.getInt("nbHits") != 0) {
                List<String> objectIDs = new ArrayList<String>();
                for (int i = 0; i < results.getJSONArray("hits").length(); ++i) {
                    JSONObject hit = results.getJSONArray("hits").getJSONObject(i);
                    objectIDs.add(hit.getString("objectID"));
                }
                JSONObject task = this.deleteObjects(objectIDs);
                this.waitTask(task.getString("taskID"));
                results = this.search(query);
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Search inside the index
     * @return a JSONObject containing search results
     * @throws AlgoliaException
     */
    protected JSONObject search(Query query) throws AlgoliaException {
        String paramsString = query.build();
        if (paramsString.length() > 0) {
            return client.getRequest("/1/indexes/" + encodedIndexName + "?" + paramsString, true);
        } else {
            return client.getRequest("/1/indexes/" + encodedIndexName, true);
        }
    }

    /**
     * Search inside the index
     * @return a byte array containing search results
     * @throws AlgoliaException
     */
    protected byte[] searchRaw(Query query) throws AlgoliaException {
        String paramsString = query.build();
        if (paramsString.length() > 0) {
            return client.getRequestRaw("/1/indexes/" + encodedIndexName + "?" + paramsString, true);
        } else {
            return client.getRequestRaw("/1/indexes/" + encodedIndexName, true);
        }
    }

    /**
     * Wait the publication of a task on the server.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param timeToWait time to sleep seed
     * @throws AlgoliaException
     */
    protected void waitTask(String taskID, long timeToWait) throws AlgoliaException {
        try {
            while (true) {
                JSONObject obj = client.getRequest("/1/indexes/" + encodedIndexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"), false);
                if (obj.getString("status").equals("published")) {
                    return;
                }
                try {
                    Thread.sleep(timeToWait >= MAX_TIME_MS_TO_WAIT ? MAX_TIME_MS_TO_WAIT : timeToWait);
                } catch (InterruptedException e) {
                    continue;
                }
                timeToWait *= 2;
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait the publication of a task on the server.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @throws AlgoliaException
     */
    protected void waitTask(String taskID) throws AlgoliaException {
        waitTask(taskID, MAX_TIME_MS_TO_WAIT);
    }

    /**
     * Get settings of this index
     *
     * @throws AlgoliaException
     */
    protected JSONObject getSettings() throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/settings", false);
    }

    /**
     * Set settings for this index
     *
     * @param settings the settings object
     * @throws AlgoliaException
     */
    protected JSONObject setSettings(JSONObject settings) throws AlgoliaException {
        return client.putRequest("/1/indexes/" + encodedIndexName + "/settings", settings.toString());
    }

    /**
     * Delete the index content without removing settings and index specific API keys.
     *
     * @throws AlgoliaException
     */
    protected JSONObject clearIndex() throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName + "/clear", "", false);
    }

    /**
     * Perform a search with disjunctive facets generating as many queries as number of disjunctive facets
     *
     * @param query             the query
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements       Map representing the current refinements
     * @throws AlgoliaException
     */
    public JSONObject searchDisjunctiveFaceting(Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements) throws AlgoliaException {
        if (refinements == null) {
            refinements = new HashMap<String, List<String>>();
        }
        HashMap<String, List<String>> disjunctiveRefinements = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
            if (disjunctiveFacets.contains(elt.getKey())) {
                disjunctiveRefinements.put(elt.getKey(), elt.getValue());
            }
        }

        // build queries
        List<IndexQuery> queries = new ArrayList<IndexQuery>();
        // hits + regular facets query
        StringBuilder filters = new StringBuilder();
        boolean first_global = true;
        for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
            StringBuilder or = new StringBuilder();
            or.append("(");
            boolean first = true;
            for (String val : elt.getValue()) {
                if (disjunctiveRefinements.containsKey(elt.getKey())) {
                    // disjunctive refinements are ORed
                    if (!first) {
                        or.append(',');
                    }
                    first = false;
                    or.append(String.format("%s:%s", elt.getKey(), val));
                } else {
                    if (!first_global) {
                        filters.append(',');
                    }
                    first_global = false;
                    filters.append(String.format("%s:%s", elt.getKey(), val));
                }
            }
            // Add or
            if (disjunctiveRefinements.containsKey(elt.getKey())) {
                or.append(')');
                if (!first_global) {
                    filters.append(',');
                }
                first_global = false;
                filters.append(or.toString());
            }
        }

        queries.add(new IndexQuery(this.indexName, new Query(query).setFacetFilters(filters.toString())));
        // one query per disjunctive facet (use all refinements but the current one + hitsPerPage=1 + single facet
        for (String disjunctiveFacet : disjunctiveFacets) {
            filters = new StringBuilder();
            first_global = true;
            for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
                if (disjunctiveFacet.equals(elt.getKey())) {
                    continue;
                }
                StringBuilder or = new StringBuilder();
                or.append("(");
                boolean first = true;
                for (String val : elt.getValue()) {
                    if (disjunctiveRefinements.containsKey(elt.getKey())) {
                        // disjunctive refinements are ORed
                        if (!first) {
                            or.append(',');
                        }
                        first = false;
                        or.append(String.format("%s:%s", elt.getKey(), val));
                    } else {
                        if (!first_global) {
                            filters.append(',');
                        }
                        first_global = false;
                        filters.append(String.format("%s:%s", elt.getKey(), val));
                    }
                }
                // Add or
                if (disjunctiveRefinements.containsKey(elt.getKey())) {
                    or.append(')');
                    if (!first_global) {
                        filters.append(',');
                    }
                    first_global = false;
                    filters.append(or.toString());
                }
            }
            String[] facets = new String[]{disjunctiveFacet};
            queries.add(new IndexQuery(this.indexName, new Query(query).setHitsPerPage(0).enableAnalytics(false)
                    .setAttributesToRetrieve("").setAttributesToHighlight("").setAttributesToSnippet("")
                    .setFacets(facets).setFacetFilters(filters.toString())));
        }
        JSONObject answers = this.client.multipleQueries(queries, null);

        // aggregate answers
        // first answer stores the hits + regular facets
        try {
            JSONArray results = answers.getJSONArray("results");
            JSONObject aggregatedAnswer = results.getJSONObject(0);
            JSONObject disjunctiveFacetsJSON = new JSONObject();
            for (int i = 1; i < results.length(); ++i) {
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
            return aggregatedAnswer;
        } catch (JSONException e) {
            throw new Error(e);
        }
    }

    public JSONObject searchDisjunctiveFaceting(Query query, List<String> disjunctiveFacets) throws AlgoliaException {
        return searchDisjunctiveFaceting(query, disjunctiveFacets, null);
    }
}