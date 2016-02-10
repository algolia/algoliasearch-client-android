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
            String data = new Scanner(file, "UTF-8").useDelimiter("\\Z").next();
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

    public @NonNull String[] getQueries()
    {
        String[] result = new String[0];
        JSONArray queriesJson = json.optJSONArray("queries");
        if (queriesJson != null) {
            List<String> queries = new ArrayList<>();
            for (int i = 0; i < queriesJson.length(); ++i) {
                String query = queriesJson.optString(i, null);
                if (query != null) {
                    queries.add(query);
                }
            }
            result = queries.toArray(new String[queries.size()]);
        }
        return result;
    }

    public void setQueries(@NonNull String[] queries)
    {
        try {
            JSONArray queriesJson = new JSONArray();
            for (String query : queries) {
                if (query != null)
                    queriesJson.put(query);
            }
            json.put("queries", queriesJson);
        }
        catch (JSONException e) {
            // Should never happen.
        }
    }

    public void addQuery(@NonNull String query)
    {
        try {
            JSONArray queriesJson = json.optJSONArray("queries");
            if (queriesJson == null) {
                queriesJson = new JSONArray();
                json.put("queries", queriesJson);
            }
            queriesJson.put(query);
        }
        catch (JSONException e) {
            // Should never happen.
        }
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
