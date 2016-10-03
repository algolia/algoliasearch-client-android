package com.algolia.search.saas;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * This class helps coping with assertions in callbacks.
 * As AssertionErrors are thrown silently when an assertion fails in a callback,
 * this class wraps a CompletionHandler to check if an assert failed and expose the error through {@link AssertCompletionHandler#checkAssertions()}.
 */
abstract class AssertCompletionHandler implements CompletionHandler {
    private AssertionError error;
    private int invocationCount = 0;
    private StackTraceElement[] creationStackTrace;

    /**
     * Global registry of all created completion handlers. It is cleared when the static `checkAllHandlers()` method is
     * called.
     */
    private static List<AssertCompletionHandler> allHandlers = new ArrayList<>();

    // Schedule to check all handlers at JVM shutdown.
    // NOTE: This is a safety net in case the programmer forgets to check the handlers manually (hence the warning).
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (!allHandlers.isEmpty()) {
                    System.err.println("WARNING: Some handlers have not been checked! Doing it now.");
                    checkAllHandlers();
                }
            }
        }));
    }

    public AssertCompletionHandler() {
        // Capture the stack trace at creation.
        // This is used to indicate where the handler was created in case it was never called.
        creationStackTrace = Thread.currentThread().getStackTrace();

        // Add the handler to the global registry.
        synchronized (AssertCompletionHandler.class) {
            allHandlers.add(this);
        }
    }

    abstract public void doRequestCompleted(JSONObject content, AlgoliaException error);

    /**
     * Fail if the handler encountered at least one AssertionError.
     */
    public void checkAssertions() {
        // NOTE: Throwing an exception with an already populated stack trace maintains this stack trace.
        // We use this trick to have a meaningful stack trace displayed.
        if (error != null) {
            throw error;
        } else if (invocationCount == 0) {
            AssertionError e = new AssertionError("A completion handler was never called");
            e.setStackTrace(creationStackTrace);
            throw e;
        } else if (invocationCount > 1) {
            AssertionError e = new AssertionError(String.format("A completion handler was called more than once (%d times)", invocationCount));
            e.setStackTrace(creationStackTrace);
            throw e;
        }
    }

    /**
     * Check assertions on all handlers created since the last call to this method (or since the beginning of the
     * process if this method was never called).
     */
    public synchronized static void checkAllHandlers() {
        // Print all errors.
        boolean failed = false;
        for (AssertCompletionHandler handler : allHandlers) {
            try {
                handler.checkAssertions();
            } catch (AssertionError e) {
                failed = true;
                e.printStackTrace(System.err);
            }
        }
        allHandlers.clear();
        // Cause the test to fail if necessary.
        if (failed) {
            fail("Assertions where caught");
        }
    }

    @Override final public void requestCompleted(JSONObject content, AlgoliaException error) {
        invocationCount += 1;
        try {
            doRequestCompleted(content, error);
        } catch (AssertionError e) {
            this.error = e;
        }
    }
}
