package com.algolia.search.saas;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.fail;

/**
 * This class helps coping with assertions in callbacks.
 * As AssertionErrors are thrown silently when an assertion fails in a callback,
 * this class wraps a CompletionHandler to check if an assert failed and expose the error through {@link AssertCompletionHandler#checkAssertions()}.
 */
abstract class AssertCompletionHandler implements CompletionHandler {
    private AssertionError error;
    private final CompletionHandler handler;
    private final List<AssertCompletionHandler> innerHandlers = new ArrayList<>();

    public AssertCompletionHandler() {
        this.handler = new CompletionHandler() {
            @Override public void requestCompleted(JSONObject content, AlgoliaException error) {
                doRequestCompleted(content, error);
            }
        };
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
            fail("At least one assertion failed: " + error);
        }
        for (AssertCompletionHandler h : innerHandlers) {
            h.checkAssertions();
        }
    }

    @Override final public void requestCompleted(JSONObject content, AlgoliaException error) {
        try {
            handler.requestCompleted(content, error);
        } catch (AssertionError e) {
            this.error = e;
        }
    }
}
