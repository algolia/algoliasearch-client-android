package com.algolia.search.saas;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/*
 * Copyright (c) 2013 Algolia
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

/**
 * Contains all the functions related to one index
 * You should use APIClient.initIndex(indexName) to retrieve this object
 */
public class Index {
    private APIClient client;
    private String indexName;
    private String originalIndexName;
    
    /**
     * Index initialization (You should not call this initialized yourself)
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     */
    protected Index(APIClient client, String indexName) {
        try {
            this.client = client;
            this.indexName = URLEncoder.encode(indexName, "UTF-8");
            this.originalIndexName = indexName;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getIndexName() {
        return originalIndexName;
    }

    /**
     * Add an object in this index
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
    */
    public JSONObject addObject(String obj) throws AlgoliaException {
        return client._postRequest("/1/indexes/" + indexName, obj);
    }
   
    /**
     * Add an object in this index
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     */
    public JSONObject addObject(String obj, String objectID) throws AlgoliaException {
        try {
            return client._putRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"), obj);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Add several objects
     * 
     * @param objects contains an array of objects to add. If the object contains an objectID
     */
    public JSONObject addObjects(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body",obj);
                array.put(action);
            }
            JSONObject content = new JSONObject();
            content.put("requests", array);
            return client._postRequest("/1/indexes/" + indexName + "/batch", content.toString());
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     */
    public JSONObject getObject(String objectID) throws AlgoliaException {
        try {
            return client._getRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
     */
    public JSONObject getObject(String objectID,  List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            StringBuilder params = new StringBuilder();
            params.append("?attributes=");
            for (int i = 0; i < attributesToRetrieve.size(); ++i) {
                if (i > 0)
                    params.append(",");
                params.append(URLEncoder.encode(attributesToRetrieve.get(i), "UTF-8"));
            }
            return client._getRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8") + params.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Update partially an object (only update attributes passed in argument)
     * 
     * @param partialObject contains the object attributes to override, the 
     *  object must contains an objectID attribute
     */
    public JSONObject partialUpdateObject(String partialObject, String objectID) throws AlgoliaException {
        try {
            return client._postRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial", partialObject);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of object
     * 
     * @param object contains the object to save
     */
    public JSONObject saveObject(String object, String objectID) throws AlgoliaException {
        try {
            return client._putRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"), object);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of several objects
     * 
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     */
    public JSONObject saveObjects(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "updateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body",obj);
                array.put(action);
            }
            JSONObject content = new JSONObject();
            content.put("requests", array);
            return client._postRequest("/1/indexes/" + indexName + "/batch", content.toString());
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete an object from the index 
     * 
     * @param objectID the unique identifier of object to delete
     */
    public JSONObject deleteObject(String objectID) throws AlgoliaException {
        try {
            return client._deleteRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Search inside the index
     */
    public JSONObject search(Query params) throws AlgoliaException {
        String paramsString = params.getQueryString();
        if (paramsString.length() > 0)
            return client._getRequest("/1/indexes/" + indexName + "?" + paramsString);
        else
            return client._getRequest("/1/indexes/" + indexName);
    }

    /**
     * Wait the publication of a task on the server. 
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param timeBeforeRetry the time in milliseconds before retry (default = 100ms)
     */
    public void waitTask(String taskID) throws AlgoliaException {
        try {
            while (true) {
                JSONObject obj = client._getRequest("/1/indexes/" + indexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"));
                if (obj.getString("status").equals("published"))
                    return;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get settings of this index
     */
    public JSONObject getSettings() throws AlgoliaException {
        return client._getRequest("/1/indexes/" + indexName + "/settings");
    }

    /**
     * Set settings for this index
     * 
     * @param settigns the settings object that can contains :
     *  - minWordSizeForApprox1 (integer) the minimum number of characters to accept one typo (default = 3)
     *  - minWordSizeForApprox2: (integer) the minimum number of characters to accept two typos (default = 7)
     *  - hitsPerPage: (integer) the number of hits per page (default = 10)
     *  - attributesToRetrieve: (array of strings) default list of attributes to retrieve for objects
     *  - attributesToHighlight: (array of strings) default list of attributes to highlight
     *  - attributesToIndex: (array of strings) the list of fields you want to index. 
     *    By default all textual attributes of your objects are indexed, but you should update it to get optimal 
     *    results. This parameter has two important uses:
     *       - Limit the attributes to index. 
     *         For example if you store a binary image in base64, you want to store it in the index but you 
     *         don't want to use the base64 string for search.
     *       - Control part of the ranking (see the ranking parameter for full explanation). 
     *         Matches in attributes at the beginning of the list will be considered more important than matches 
     *         in attributes further down the list.
     *  - ranking: (array of strings) controls the way results are sorted. 
     *     We have four available criteria: 
     *       - typo (sort according to number of typos), 
     *       - geo: (sort according to decreassing distance when performing a geo-location based search),
     *       - position (sort according to the matching attribute), 
     *       - custom which is user defined
     *     (the standard order is ["typo", "geo", position", "custom"])
     *  - customRanking: (array of strings) lets you specify part of the ranking. 
     *    The syntax of this condition is an array of strings containing attributes prefixed 
     *    by asc (ascending order) or desc (descending order) operator.
     */
    public JSONObject setSettings(String settings) throws AlgoliaException {
        return client._putRequest("/1/indexes/" + indexName + "/settings", settings);
    }
}
