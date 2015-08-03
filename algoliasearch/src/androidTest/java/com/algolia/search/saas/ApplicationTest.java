package com.algolia.search.saas;

import android.app.Application;
import android.test.ApplicationTestCase;

import junit.framework.Assert;

import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    APIClient client;

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = new APIClient("XXXXX", "XXXXX");
    }
}