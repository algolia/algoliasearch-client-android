package com.algolia.search.saas;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
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
    private final static int HTTP_TIMEOUT_MS = 30000;
    
    private final String applicationID;
    private final String apiKey;
    private final List<String> hostsArray;
    private final DefaultHttpClient httpClient;
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     */
    public APIClient(String applicationID, String apiKey) {
        this(applicationID, apiKey, Arrays.asList(applicationID + "-1.algolia.io", 
						        		applicationID + "-2.algolia.io", 
						        		applicationID + "-3.algolia.io"));
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
        // randomize elements of hostsArray (act as a kind of load-balancer)
        Collections.shuffle(hostsArray);
        this.hostsArray = hostsArray;
        httpClient = new DefaultHttpClient();
    }
    
    /**
     * List all existing indexes
     * return an JSON Object in the form:
     * { "items": [ {"name": "contacts", "createdAt": "2013-01-18T15:33:13.556Z"},
     *              {"name": "notes", "createdAt": "2013-01-18T15:33:13.556Z"}]}
     */
    public JSONObject listIndexes() throws AlgoliaException {
        return getRequest("/1/indexes/");
    }

    /**
     * Delete an index
     *
     * @param indexName the name of index to delete
     * return an object containing a "deletedAt" attribute
     */
    public JSONObject deleteIndex(String indexName) throws AlgoliaException {
        try {
            return deleteRequest("/1/indexes/" + URLEncoder.encode(indexName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Move an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public JSONObject moveIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
    	try {
	        JSONObject content = new JSONObject();
	        content.put("operation", "move");
	        content.put("destination", dstIndexName);
	        return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString()); 	
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException(e);
    	} catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }
    
    /**
     * Copy an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public JSONObject copyIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
    	try {
	        JSONObject content = new JSONObject();
	        content.put("operation", "copy");
	        content.put("destination", dstIndexName);
	        return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString()); 	
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException(e);
    	} catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }
    
    /**
     * Return 10 last log entries.
     */
    public JSONObject getLogs() throws AlgoliaException {
    	 return getRequest("/1/logs");
    }

    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     */
    public JSONObject getLogs(int offset, int length) throws AlgoliaException {
    	 return getRequest("/1/logs?offset=" + offset + "&length=" + length);
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
        return getRequest("/1/keys");
    }

    /**
     * Get ACL of a user key
     */
    public JSONObject getUserKeyACL(String key) throws AlgoliaException {
        return getRequest("/1/keys/" + key);
    }

    /**
     * Delete an existing user key
     */
    public JSONObject deleteUserKey(String key) throws AlgoliaException {
        return deleteRequest("/1/keys/" + key);
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
        return postRequest("/1/keys", jsonObject.toString());
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
     * @param maxQueriesPerIPPerHour Specify the maximum number of API calls allowed from an IP address per hour.  Defaults to 0 (no rate limit).
     * @param maxHitsPerQuery Specify the maximum number of hits this API key can retrieve in one call. Defaults to 0 (unlimited) 
     */
    public JSONObject addUserKey(List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery) throws AlgoliaException {
        return addUserKey(acls, validity, maxQueriesPerIPPerHour, maxHitsPerQuery, null);
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
     * @param maxQueriesPerIPPerHour Specify the maximum number of API calls allowed from an IP address per hour.  Defaults to 0 (no rate limit).
     * @param maxHitsPerQuery Specify the maximum number of hits this API key can retrieve in one call. Defaults to 0 (unlimited)
     * @param indexes the list of targeted indexes 
     */
    public JSONObject addUserKey(List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery, String indexes) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
            jsonObject.put("maxQueriesPerIPPerHour", maxQueriesPerIPPerHour);
            jsonObject.put("maxHitsPerQuery", maxHitsPerQuery);
            if (indexes != null) {
                jsonObject.put("indexes", indexes);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return postRequest("/1/keys", jsonObject.toString());
    }
    
    /**
     * Generate a secured and public API Key from a list of tagFilters and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param tagFilters the list of tags applied to the query (used as security)
     */
    public String generateSecuredApiKey(String privateApiKey, String tagFilters) {
        return generateSecuredApiKey(privateApiKey, tagFilters, null);
    }
    
    /**
     * Generate a secured and public API Key from a list of tagFilters and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param tagFilters the list of tags applied to the query (used as security)
     * @param userToken an optional token identifying the current user
     */
    public String generateSecuredApiKey(String privateApiKey, String tagFilters, String userToken) {
        return sha256(privateApiKey + tagFilters + (userToken != null ? userToken : ""));
    }
    
    static String sha256(String str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
        StringBuffer sb = new StringBuffer();
        for (byte b : md.digest(str.getBytes())) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private static enum Method {
        GET, POST, PUT, DELETE, OPTIONS, TRACE, HEAD;
    }
    
    protected JSONObject getRequest(String url) throws AlgoliaException {
    	return _request(Method.GET, url, null);
    }
    
    protected JSONObject deleteRequest(String url) throws AlgoliaException {
    	return _request(Method.DELETE, url, null);
    }
    
    protected JSONObject postRequest(String url, String obj) throws AlgoliaException {
    	return _request(Method.POST, url, obj);
    }
    
    protected JSONObject putRequest(String url, String obj) throws AlgoliaException {
    	return _request(Method.PUT, url, obj);
    }
    
    private JSONObject _request(Method m, String url, String json) throws AlgoliaException {
    	HttpRequestBase req;

    	// for each host
    	for (String host : this.hostsArray) {
        	switch (m) {
    		case DELETE:
    			req = new HttpDelete();
    			break;
    		case GET:
    			req = new HttpGet();
    			break;
    		case POST:
    			req = new HttpPost();
    			break;
    		case PUT:
    			req = new HttpPut();
    			break;
    		default:
    			throw new IllegalArgumentException("Method " + m + " is not supported");
        	}

            // set URL
            try {
    			req.setURI(new URI("https://" + host + url));
    		} catch (URISyntaxException e) {
    			// never reached
    			throw new IllegalStateException(e);
    		}
        	
        	// set auth headers
        	req.setHeader("X-Algolia-Application-Id", this.applicationID);
            req.setHeader("X-Algolia-API-Key", this.apiKey);
            
            // set JSON entity
            if (json != null) {
            	if (!(req instanceof HttpEntityEnclosingRequestBase)) {
            		throw new IllegalArgumentException("Method " + m + " cannot enclose entity");
            	}
	            req.setHeader("Content-type", "application/json");
	            try {
	                StringEntity se = new StringEntity(json, "UTF-8"); 
	                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	                ((HttpEntityEnclosingRequestBase) req).setEntity(se); 
	            } catch (UnsupportedEncodingException e) {
	                throw new AlgoliaException("Invalid JSON Object: " + json);
	            }
            }
            
            httpClient.getParams().setParameter("http.socket.timeout", HTTP_TIMEOUT_MS);
            httpClient.getParams().setParameter("http.connection.timeout", HTTP_TIMEOUT_MS);

            HttpResponse response;
            try {
            	response = httpClient.execute(req);
            } catch (IOException e) {
            	// on error continue on the next host
            	continue;
            }
            int code = response.getStatusLine().getStatusCode();
            if (code == 200 || code == 201) {
                // OK
            } else if (code == 400) {
                consumeQuietly(response.getEntity());
                throw new AlgoliaException("Bad request");
            } else if (code == 403) {
                consumeQuietly(response.getEntity());
                throw new AlgoliaException("Invalid Application-ID or API-Key");
            } else if (code == 404) {
                consumeQuietly(response.getEntity());
                throw new AlgoliaException("Resource does not exist");
            } else {
                consumeQuietly(response.getEntity());
                // KO, continue
                continue;
            }
            try {
            	InputStream istream = response.getEntity().getContent();
            	InputStreamReader is = new InputStreamReader(istream, "UTF-8");
            	StringBuilder builder= new StringBuilder();
                char[] buf = new char[1000];
                int l = 0;
                while (l >= 0) {
                    builder.append(buf, 0, l);
                    l = is.read(buf);
                }
                JSONTokener tokener = new JSONTokener(builder.toString());
                JSONObject res = new JSONObject(tokener);
                is.close();
                return res;
            } catch (IOException e) {
            	continue;
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        throw new AlgoliaException("Hosts unreachable");
    }
    
    /**
     * Ensures that the entity content is fully consumed and the content stream, if exists,
     * is closed.
     *
     * @param entity
     */
    private void consumeQuietly(final HttpEntity entity) {
        if (entity == null) {
            return;
        }
        try {
	        if (entity.isStreaming()) {
	            InputStream instream = entity.getContent();
	            if (instream != null) {
	                instream.close();
	            }
	        }
        } catch (IOException e) {
        	// not fatal
        }
    }
}
