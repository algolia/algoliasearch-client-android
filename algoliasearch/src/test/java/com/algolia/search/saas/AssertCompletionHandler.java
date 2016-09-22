package com.algolia.search.saas;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class helps coping with assertions in callbacks.
 * As AssertionErrors are thrown silently when an assertion fails in a callback,
 * this class wraps a CompletionHandler to check if an assert failed and expose the error through {@link AssertCompletionHandler#checkAssertions()}.
 */
abstract class AssertCompletionHandler implements CompletionHandler {
    private AssertionError error;
    private final CompletionHandler handler;
    private final List<AssertCompletionHandler> innerHandlers = new ArrayList<>();

    /**
     * Global registry of all created completion handlers. It is cleared when the static `checkAllHandlers()` method is
     * called.
     */
    private static List<AssertCompletionHandler> allHandlers = new ArrayList<>();

    public AssertCompletionHandler() {
        this.handler = new CompletionHandler() {
            @Override public void requestCompleted(JSONObject content, AlgoliaException error) {
                doRequestCompleted(content, error);
            }
        };
        synchronized (AssertCompletionHandler.class) {
            allHandlers.add(this);
        }
    }

    abstract public void doRequestCompleted(JSONObject content, AlgoliaException error);

    /**
     * Add an inner handler to be checked as well in {@link AssertCompletionHandler#checkAssertions()}.
     */
    public void addInnerHandler(AssertCompletionHandler handler) {
        innerHandlers.add(handler);
    }

    /**
     * Fail if the handler encountered at least one AssertionError.
     */
    public void checkAssertions() {
        if (error != null) {
            // Throwing the original exception maintains the stack trace... though I am not entirely sure why. =:)
            // (An alternative would be to chain the exception.)
            throw error;
        }
        for (AssertCompletionHandler h : innerHandlers) {
            h.checkAssertions();
        }
    }

    /**
     * Check assertions on all handlers created since the last call to this method (or since the beginning of the
     * process if this method was never called).
     */
    public synchronized static void checkAllHandlers() {
        for (AssertCompletionHandler handler : allHandlers) {
            handler.checkAssertions();
        }
        allHandlers.clear();
    }

    @Override final public void requestCompleted(JSONObject content, AlgoliaException error) {
        try {
            handler.requestCompleted(content, error);
        } catch (AssertionError e) {
            this.error = e;
        }
    }
}
