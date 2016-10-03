package com.algolia.search.saas;

import android.os.Build;

import com.algolia.search.saas.android.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(shadows = {ShadowLog.class}, manifest = Config.NONE, constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(CustomRunner.class)
public abstract class RobolectricTestCase {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
    }
}
