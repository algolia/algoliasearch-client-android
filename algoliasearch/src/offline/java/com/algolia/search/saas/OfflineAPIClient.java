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

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An API client that adds offline features on top of the regular online API client.
 *
 * @note Requires Algolia's SDK.
 */
public class OfflineAPIClient extends APIClient
{
    private File rootDataDir;

    // Threading facilities
    // --------------------
    // Used by the indices to coordinate their execution.

    /** Background queue used to build indices. */
    protected ExecutorService buildExecutorService = Executors.newSingleThreadExecutor();

    /** Handler used to execute operations on the main thread. */
    protected Handler mainHandler = new Handler(Looper.getMainLooper());

    public OfflineAPIClient(String applicationID, String apiKey, File dataDir)
    {
        this(applicationID, apiKey, dataDir, null, false, null);
    }

    public OfflineAPIClient(String applicationID, String apiKey, File dataDir, List<String> hostsArray)
    {
        this(applicationID, apiKey, dataDir, hostsArray, false, null);
    }

    public OfflineAPIClient(String applicationID, String apiKey, File dataDir, boolean enableDsn)
    {
        this(applicationID, apiKey, dataDir, null, enableDsn, null);
    }

    public OfflineAPIClient(String applicationID, String apiKey, File dataDir, List<String> hostsArray, boolean enableDsn, String dsnHost)
    {
        super(applicationID, apiKey, hostsArray, enableDsn, dsnHost);
        this.rootDataDir = dataDir;
    }

    @Override
    public MirroredIndex initIndex(String indexName)
    {
        return new MirroredIndex(this, indexName);
    }

    public File getRootDataDir()
    {
        return rootDataDir;
    }

    /**
     * Enable the offline mode.
     * @param licenseData License for Algolia's SDK.
     */
    public void enableOfflineMode(@NonNull String licenseData) {
        // Init the SDK.
        Sdk.getInstance().init(licenseData);
        // TODO: Report any error.
    }
}
