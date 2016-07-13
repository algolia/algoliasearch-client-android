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

import android.content.Context;
import android.support.annotation.NonNull;

import com.algolia.search.offline.core.Sdk;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An API client that adds offline features on top of the regular online API client.
 *
 * <p>NOTE: Requires Algolia's SDK. The {@link #enableOfflineMode(String)} method must be called with a valid license
 * key prior to calling any offline-related method.</p>
 */
public class OfflineClient extends Client
{
    private Context context;
    private File rootDataDir;

    // Threading facilities
    // --------------------
    // Used by the indices to coordinate their execution.
    //
    // NOTE: The build and search queues must be serial to prevent concurrent searches or builds on a given index, but
    // may be distinct because building can be done in parallel with search.
    //
    // NOTE: Although serialization is only strictly needed at the index level, we use global queues as a way to limit
    // resource consumption by the SDK.

    /** Background queue used to build local indices. */
    protected ExecutorService localBuildExecutorService = Executors.newSingleThreadExecutor();

    /** Background queue used to search local indices. */
    protected ExecutorService localSearchExecutorService = Executors.newSingleThreadExecutor();

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey)
    {
        this(context, applicationID, apiKey, null, null);
    }

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     * @param dataDir Path to the directory where the local data will be stored. If null, the default directory will
     *                be used. See {@link #getDefaultDataDir()}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey, File dataDir)
    {
        this(context, applicationID, apiKey, dataDir, null);
    }

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     * @param dataDir Path to the directory where the local data will be stored. If null, the default directory will
     *                be used. See {@link #getDefaultDataDir()}.
     * @param hosts See {@link Client}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey, File dataDir, String[] hosts)
    {
        super(applicationID, apiKey, hosts);
        this.context = context;
        if (dataDir != null) {
            this.rootDataDir = dataDir;
        } else {
            this.rootDataDir = getDefaultDataDir();
        }
        userAgent += ";algoliasearch-offline-core-android " + Sdk.getInstance().getVersionString();
    }

    /**
     * Create a new index. Although this will always be an instance of {@link MirroredIndex}, mirroring is deactivated
     * by default.
     * @param indexName the name of index
     * @return The newly created index.
     */
    @Override
    public MirroredIndex initIndex(@NonNull String indexName)
    {
        return new MirroredIndex(this, indexName);
    }

    /**
     * Get the path to directory where the local data is stored.
     */
    public @NonNull File getRootDataDir()
    {
        return rootDataDir;
    }

    /**
     * Enable the offline mode.
     * @param licenseData License for Algolia's SDK.
     */
    public void enableOfflineMode(@NonNull String licenseData) {
        // Init the SDK.
        Sdk.getInstance().init(context, licenseData);
        // TODO: Report any error.
    }

    /**
     * Get the default data directory.
     * This is an "algolia" subdirectory inside the application's files directory.
     *
     * @return The default data directory.
     */
    public File getDefaultDataDir() {
        return new File(context.getFilesDir(), "algolia");
    }
}
