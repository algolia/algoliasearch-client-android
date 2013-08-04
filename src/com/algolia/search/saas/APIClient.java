package com.algolia.search.saas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
 * Entry point in the Java API.
 * You should instantiate a Client object with your ApplicationID, ApiKey and Hosts 
 * to start using Algolia Search API
 */
public class APIClient {
    private String applicationID;
    private String apiKey;
    private List<String> hostsArray;
    private DefaultHttpClient httpClient;
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     */
    public APIClient(String applicationID, String apiKey) {
        if (applicationID == null || applicationID.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an applicationID.");
        }
        this.applicationID = applicationID;
        if (apiKey == null || apiKey.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an apiKey.");
        }
        this.apiKey = apiKey;
        if (hostsArray == null || hostsArray.size() == 0) {
            throw new RuntimeException("AlgoliaSearch requires a list of hostnames.");
        }
        this.hostsArray = Arrays.asList(applicationID + "-1.algolia.io", 
						        		applicationID + "-2.algolia.io", 
						        		applicationID + "-3.algolia.io");
        // randomize elements of hostsArray (act as a kind of load-balancer)
        Collections.shuffle(hostsArray);
        httpClient = new DefaultHttpClient();
    }
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     */
    public APIClient(String applicationID, String apiKey, List<String> hostsArray) {
        if (applicationID == null || applicationID.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an applicationID.");
        }
        this.applicationID = applicationID;
        if (apiKey == null || apiKey.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an apiKey.");
        }
        this.apiKey = apiKey;
        if (hostsArray == null || hostsArray.size() == 0) {
            throw new RuntimeException("AlgoliaSearch requires a list of hostnames.");
        }
        this.hostsArray = hostsArray;
        // randomize elements of hostsArray (act as a kind of load-balancer)
        Collections.shuffle(hostsArray);
        httpClient = new DefaultHttpClient();
    }
    
    /**
     * List all existing indexes
     * return an JSON Object in the form:
     * { "items": [ {"name": "contacts", "createdAt": "2013-01-18T15:33:13.556Z"},
     *              {"name": "notes", "createdAt": "2013-01-18T15:33:13.556Z"}]}
     */
    public JSONObject listIndexes() throws AlgoliaException {
        return _getRequest("/1/indexes/");
    }

    /**
     * Delete an index
     *
     * @param indexName the name of index to delete
     * return an object containing a "deletedAt" attribute
     */
    public JSONObject deleteIndex(String indexName) throws AlgoliaException {
        try {
            return _deleteRequest("/1/indexes/" + URLEncoder.encode(indexName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
  
    /**
     * Get the index object initialized (no server call needed for initialization)
     *
     * @param indexName the name of index
     */
    public Index initIndex(String indexName) {
        return new Index(this, indexName);
    }

    /**
     * List all existing user keys with their associated ACLs
     */
    public JSONObject listUserKeys() throws AlgoliaException {
        return _getRequest("/1/keys");
    }

    /**
     * Get ACL of a user key
     */
    public JSONObject getUserKeyACL(String key) throws AlgoliaException {
        return _getRequest("/1/keys/" + key);
    }

    /**
     * Delete an existing user key
     */
    public JSONObject deleteUserKey(String key) throws AlgoliaException {
        return _deleteRequest("/1/keys/" + key);
    }

    /**
     * Create a new user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     */
    public JSONObject addUserKey(List<String> acls) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return _postRequest("/1/keys", jsonObject.toString());
    }
    
    /**
     * Create a new user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     * @param validity the number of seconds after which the key will be automatically removed (0 means no time limit for this key)
     */
    public JSONObject addUserKey(List<String> acls, int validity) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return _postRequest("/1/keys", jsonObject.toString());
    }
    
    protected JSONObject _getRequest(String url) throws AlgoliaException {
        for (String host : this.hostsArray) {
            HttpGet httpGet = new HttpGet("https://" + host + url);
            httpGet.setHeader("X-Algolia-Application-Id", this.applicationID);
            httpGet.setHeader("X-Algolia-API-Key", this.apiKey);
            try {
                HttpResponse response = httpClient.execute(httpGet);
                int code = response.getStatusLine().getStatusCode();
                if (code == 403) {
                    throw new AlgoliaException("Invalid Application-ID or API-Key");
                }
                if (code == 404) {
                    throw new AlgoliaException("Resource does not exist");
                }
                if (code == 503)
                    continue;
                InputStream istream = response.getEntity().getContent();
                InputStreamReader is = new InputStreamReader(istream, "UTF-8");
                BufferedReader reader = new BufferedReader(is);
                String json = reader.readLine();
                JSONTokener tokener = new JSONTokener(json);
                JSONObject res = new JSONObject(tokener);
                reader.close();
                is.close();
                is.close();
                return res;
            } catch (IOException e) {
                // on error continue on the next host
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        throw new AlgoliaException("Hosts unreachable");
    }
    
    protected JSONObject _deleteRequest(String url) throws AlgoliaException {
        for (String host : this.hostsArray) {
            HttpDelete httpDelete = new HttpDelete("https://" + host + url);
            httpDelete.setHeader("X-Algolia-Application-Id", this.applicationID);
            httpDelete.setHeader("X-Algolia-API-Key", this.apiKey);
            try {
                HttpResponse response = httpClient.execute(httpDelete);
                int code = response.getStatusLine().getStatusCode();
                if (code == 403) {
                    throw new AlgoliaException("Invalid Application-ID or API-Key");
                }
                if (code == 404) {
                    throw new AlgoliaException("Resource does not exist");
                }
                if (code == 503)
                    continue;
                InputStream istream = response.getEntity().getContent();
                InputStreamReader is = new InputStreamReader(istream, "UTF-8");
                BufferedReader reader = new BufferedReader(is);
                String json = reader.readLine();
                JSONTokener tokener = new JSONTokener(json);
                JSONObject res = new JSONObject(tokener);
                reader.close();
                is.close();
                is.close();
                return res;
            } catch (IOException e) {
                // on error continue on the next host
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        throw new AlgoliaException("Hosts unreachable");
    }
    
    protected JSONObject _postRequest(String url, String obj) throws AlgoliaException {
        for (String host : this.hostsArray) {
            HttpPost httpPost = new HttpPost("https://" + host + url);
            httpPost.setHeader("X-Algolia-Application-Id", this.applicationID);
            httpPost.setHeader("X-Algolia-API-Key", this.apiKey);
            httpPost.setHeader("Content-type", "application/json");
            
            try {
                StringEntity se = new StringEntity(obj, "UTF-8"); 
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se); 
            } catch (UnsupportedEncodingException e) {
                throw new AlgoliaException("Invalid JSON Object: " + obj);
            }
            try {
                HttpResponse response = httpClient.execute(httpPost);
                int code = response.getStatusLine().getStatusCode();
                if (code == 403) {
                    throw new AlgoliaException("Invalid Application-ID or API-Key");
                }
                if (code == 404) {
                    throw new AlgoliaException("Resource does not exist");
                }
                if (code == 503)
                    continue;
                InputStream istream = response.getEntity().getContent();
                InputStreamReader is = new InputStreamReader(istream, "UTF-8");
                BufferedReader reader = new BufferedReader(is);
                String json = reader.readLine();
                JSONTokener tokener = new JSONTokener(json);
                JSONObject res = new JSONObject(tokener);
                reader.close();
                is.close();
                is.close();
                return res;
            } catch (IOException e) {
                // on error continue on the next host
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        throw new AlgoliaException("Hosts unreachable");
    }
    
    protected JSONObject _putRequest(String url, String obj) throws AlgoliaException {
        for (String host : this.hostsArray) {
            HttpPut httpPut = new HttpPut("https://" + host + url);
            httpPut.setHeader("X-Algolia-Application-Id", this.applicationID);
            httpPut.setHeader("X-Algolia-API-Key", this.apiKey);
            httpPut.setHeader("Content-type", "application/json");
            
            try {
                StringEntity se = new StringEntity(obj, "UTF-8"); 
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPut.setEntity(se); 
            } catch (UnsupportedEncodingException e) {
                throw new AlgoliaException("Invalid JSON Object: " + obj);
            }
            try {
                HttpResponse response = httpClient.execute(httpPut);
                int code = response.getStatusLine().getStatusCode();
                if (code == 403) {
                    throw new AlgoliaException("Invalid Application-ID or API-Key");
                }
                if (code == 404) {
                    throw new AlgoliaException("Resource does not exist");
                }
                if (code == 503)
                    continue;
                InputStream istream = response.getEntity().getContent();
                InputStreamReader is = new InputStreamReader(istream, "UTF-8");
                BufferedReader reader = new BufferedReader(is);
                String json = reader.readLine();
                JSONTokener tokener = new JSONTokener(json);
                JSONObject res = new JSONObject(tokener);
                reader.close();
                is.close();
                is.close();
                return res;
            } catch (IOException e) {
                // on error continue on the next host
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        throw new AlgoliaException("Hosts unreachable");
    }
}
