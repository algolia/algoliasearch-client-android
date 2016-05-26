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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


/**
 * Mirroring settings for a mirrored index.
 */
class MirrorSettings
{
    private JSONObject json = new JSONObject();

    public void save(@NonNull File file)
    {
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            writer.write(json.toString());
            writer.close();
        }
        catch (IOException e) {
            // Bad luck. Ignore. Yeah, shame.
        }
    }

    public void load(@NonNull File file)
    {
        try {
            String data = new Scanner(file, "UTF-8").useDelimiter("\\Z").next(); // reads the entire file as one string
            json = new JSONObject(data);
        }
        catch (IOException | JSONException e) {
            // Bad luck. Ignore. Yeah, shame.
        }
    }

    public @NonNull Date getLastSyncDate()
    {
        long date = json.optLong("lastSyncDate");
        return new Date(date);
    }

    public void setLastSyncDate(@NonNull Date date)
    {
        try {
            json.put("lastSyncDate", date.getTime());
        }
        catch (JSONException e) {
            // Should never happen.
        }
    }

    public @NonNull MirroredIndex.DataSelectionQuery[] getQueries()
    {
        MirroredIndex.DataSelectionQuery[] result = new MirroredIndex.DataSelectionQuery[0];
        JSONArray queriesJson = json.optJSONArray("queries");
        if (queriesJson != null) {
            List<MirroredIndex.DataSelectionQuery> queries = new ArrayList<>();
            for (int i = 0; i < queriesJson.length(); ++i) {
                JSONObject queryJson = queriesJson.optJSONObject(i);
                if (queryJson != null) {
                    String queryString = queryJson.optString("query");
                    int maxObjects = queryJson.optInt("maxObjects");
                    if (queryString != null && maxObjects != 0) {
                        MirroredIndex.DataSelectionQuery query = new MirroredIndex.DataSelectionQuery(Query.parse(queryString), maxObjects);
                        queries.add(query);
                    }
                }
            }
            result = queries.toArray(new MirroredIndex.DataSelectionQuery[queries.size()]);
        }
        return result;
    }

    public void setQueries(@NonNull MirroredIndex.DataSelectionQuery... queries)
    {
        try {
            JSONArray queriesJson = new JSONArray();
            for (MirroredIndex.DataSelectionQuery query : queries) {
                if (query != null)
                    queriesJson.put(serializeQuery(query));
            }
            json.put("queries", queriesJson);
        }
        catch (JSONException e) {
            // Should never happen.
        }
    }

    public void addQuery(@NonNull MirroredIndex.DataSelectionQuery query)
    {
        try {
            JSONArray queriesJson = json.optJSONArray("queries");
            if (queriesJson == null) {
                queriesJson = new JSONArray();
                json.put("queries", queriesJson);
            }
            queriesJson.put(serializeQuery(query));
        }
        catch (JSONException e) {
            // Should never happen.
        }
    }

    private JSONObject serializeQuery(MirroredIndex.DataSelectionQuery query) throws JSONException
    {
        JSONObject queryJson = new JSONObject();
        queryJson.put("query", query.query.build());
        queryJson.put("maxObjects", query.maxObjects);
        return queryJson;
    }

    public @NonNull Date getQueriesModificationDate()
    {
        long date = json.optLong("queriesModificationDate");
        return new Date(date);
    }

    public void setQueriesModificationDate(@NonNull Date date)
    {
        try {
            json.put("queriesModificationDate", date.getTime());
        }
        catch (JSONException e) {
            // Should never happen.
        }
    }

}
