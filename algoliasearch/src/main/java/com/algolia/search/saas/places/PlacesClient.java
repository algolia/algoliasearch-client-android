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

package com.algolia.search.saas.places;

import android.support.annotation.NonNull;

import com.algolia.search.saas.AbstractClient;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client for [Algolia Places](https://community.algolia.com/places/).
 */
public class PlacesClient extends AbstractClient {
    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    /**
     * Create a new authenticated Algolia Places client.
     *
     * @param applicationID The application ID (available in your Algolia Dashboard).
     * @param apiKey A valid API key for the service.
     */
    public PlacesClient(@NonNull String applicationID, @NonNull String apiKey) {
        super(applicationID, apiKey, null, null);
        setDefaultHosts();
    }

    /**
     * Create a new unauthenticated Algolia Places client.
     *
     * NOTE: The rate limit for the unauthenticated API is significantly lower than for the authenticated API.
     */
    public PlacesClient() {
        super(null, null, null, null);
        setDefaultHosts();
    }

    /**
     * Set the default hosts for Algolia Places.
     */
    private void setDefaultHosts() {
        List<String> fallbackHosts = Arrays.asList(
                "places-1.algolianet.com",
                "places-2.algolianet.com",
                "places-3.algolianet.com"
        );
        Collections.shuffle(fallbackHosts);

        List<String> hosts = new ArrayList<>(fallbackHosts.size() + 1);
        hosts.add("places-dsn.algolia.net");
        hosts.addAll(fallbackHosts);
        String[] hostsArray = hosts.toArray(new String[hosts.size()]);

        setReadHosts(hostsArray);
        setWriteHosts(hostsArray);
    }

    // ----------------------------------------------------------------------
    // Public operations
    // ----------------------------------------------------------------------

    /**
     * Search for places.
     *
     * @param params Search parameters.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchAsync(@NonNull PlacesQuery params, @NonNull CompletionHandler completionHandler) {
        final PlacesQuery paramsCopy = new PlacesQuery(params);
        return new AsyncTaskRequest(completionHandler) {
            @Override
            protected @NonNull JSONObject run() throws AlgoliaException {
                return search(paramsCopy);
            }
        }.start();
    }

    // ----------------------------------------------------------------------
    // Internal operations
    // ----------------------------------------------------------------------

    /**
     * Search for places.
     *
     * @param params Search parameters.
     */
    protected JSONObject search(@NonNull PlacesQuery params) throws AlgoliaException {
        try {
            JSONObject body = new JSONObject()
                .put("params", params.build());
            return postRequest("/1/places/query", body.toString(), true /* readOperation */, /* requestOptions: */ null);
        }
        catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }
}
