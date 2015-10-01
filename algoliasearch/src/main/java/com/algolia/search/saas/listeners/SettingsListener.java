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

package com.algolia.search.saas.listeners;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.TaskParams;

import org.json.JSONObject;

/**
 * Asynchronously receive result of Index.(get/set)SettingsASync methods
 */
public interface SettingsListener {
    /**
     * Asynchronously receive result of Index.(get/set)SettingsASync methods.
     */
    void settingsResult(Index index, TaskParams.Settings context, JSONObject results);

    /**
     * Asynchronously receive error of Index.(get/set)SettingsASync methods.
     */
    void settingsError(Index index, TaskParams.Settings context, AlgoliaException e);
}
