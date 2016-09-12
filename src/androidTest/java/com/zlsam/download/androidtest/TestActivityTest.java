package com.zlsam.download.androidtest;

import android.test.ActivityInstrumentationTestCase2;

import com.zlsam.download.TestActivity;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.zlsam.download.androidtest.TestActivityTest \
 * com.zlsam.download.tests/android.test.InstrumentationTestRunner
 */
public class TestActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public TestActivityTest() {
        super("com.zlsam.download", TestActivity.class);
    }

}
