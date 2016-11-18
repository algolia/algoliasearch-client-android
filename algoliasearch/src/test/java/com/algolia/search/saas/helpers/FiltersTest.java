package com.algolia.search.saas.helpers;

import com.algolia.search.saas.RobolectricTestCase;
import com.google.common.base.CharMatcher;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Filters builder class
 * <p/>
 *
 * @author Felipe Conde (felipe@dice.fm)
 *         On 17/11/2016.
 */

public class FiltersTest extends RobolectricTestCase {

    private Filters.Builder mBuilder;

    @Before
    public void init() {
        mBuilder = new Filters.Builder();
    }

    @Test
    public void addFilterKeyTest() {
        String key = "a";
        mBuilder.addFilter(key);
        String filters = mBuilder.build().getFilters();

        assertEquals(key, filters);
    }

    @Test
    public void addFilterKeyValueTest() {
        String key = "a";
        String value = "b";
        mBuilder.addFilter(key, value);
        String filters = mBuilder.build().getFilters();

        assertEquals(key + Filters.Builder.COLON + value, filters);
    }

    @Test
    public void addFilterKeyValueOperatorTest() {
        String key = "a";
        String value = "b";
        Filters.Operator operator = Filters.Operator.EQUALS;
        mBuilder.addFilter(key, value, operator);
        String filters = mBuilder.build().getFilters();

        assertEquals(key + operator.getValue() + value, filters);
        assertTrue(filters.contains(Filters.Operator.EQUALS.getValue() + ""));
    }

    @Test
    public void addFilterOtherEmptyFilter() {
        String key = "a";
        String value = "b";
        Filters.Operator operator = Filters.Operator.EQUALS;
        mBuilder.addFilter(key, value, operator);
        Filters filters = mBuilder.build();

        Filters.Builder builder = new Filters.Builder();
        builder.addFilter(filters);

        assertEquals(key + operator.getValue() + value, filters.getFilters());
    }

    @Test
    public void addFilterToOtherFilter() {
        String key = "a";
        String value = "b";
        Filters.Operator operator = Filters.Operator.EQUALS;
        mBuilder.addFilter(key, value, operator);
        Filters filters = mBuilder.build();

        mBuilder.and();
        mBuilder.addFilter(key);
        mBuilder.addFilter(filters);
        Filters filterWithFilter = mBuilder.build();

        assertNotEquals(filters.getFilters(), filterWithFilter.getFilters());
        assertTrue(filterWithFilter.getFilters().contains(filters.getFilters()));
        assertTrue(1 == CharMatcher.is(Filters.Builder.OPEN_CURLY_BRACES).countIn(filterWithFilter.getFilters()));
        assertTrue(1 == CharMatcher.is(Filters.Builder.CLOSE_CURLY_BRACES).countIn(filterWithFilter.getFilters()));
    }

    @Test
    public void andTest() {
        String keyA = "a";
        String keyB = "b";
        mBuilder.addFilter(keyA);
        mBuilder.and();
        mBuilder.addFilter(keyB);

        assertTrue(mBuilder.build().getFilters().contains(Filters.Builder.AND));
        assertEquals(keyA + Filters.Builder.EMPTY_SPACE + Filters.Builder.AND + Filters.Builder.EMPTY_SPACE + keyB, mBuilder.build().getFilters());
    }

    @Test
    public void orTest() {
        String keyA = "a";
        String keyB = "b";
        mBuilder.addFilter(keyA);
        mBuilder.or();
        mBuilder.addFilter(keyB);

        assertTrue(mBuilder.build().getFilters().contains(Filters.Builder.OR));
        assertEquals(keyA + Filters.Builder.EMPTY_SPACE + Filters.Builder.OR + Filters.Builder.EMPTY_SPACE + keyB, mBuilder.build().getFilters());
    }

    @Test
    public void toTest() {
        String keyA = "a";
        String valueA = "valueA";
        String keyB = "b";
        mBuilder.addFilter(keyA, valueA);
        mBuilder.to();
        mBuilder.addFilter(keyB);

        assertTrue(mBuilder.build().getFilters().contains(Filters.Builder.TO));
        assertEquals(keyA + Filters.Builder.COLON + valueA + Filters.Builder.EMPTY_SPACE + Filters.Builder.TO + Filters.Builder.EMPTY_SPACE + keyB, mBuilder.build().getFilters());
    }

    @Test
    public void notTest() {
        String keyA = "a";
        String keyB = "b";
        mBuilder.addFilter(keyA);
        mBuilder.not();
        mBuilder.addFilter(keyB);

        assertTrue(mBuilder.build().getFilters().contains(Filters.Builder.NOT));
        assertEquals(keyA + Filters.Builder.EMPTY_SPACE + Filters.Builder.NOT + Filters.Builder.EMPTY_SPACE + keyB, mBuilder.build().getFilters());
    }

    @Test
    public void lookTrailingAndOperatorTest() {
        String key = "a";
        mBuilder.addFilter(key);
        mBuilder.and();

        assertFalse(mBuilder.build().getFilters().contains(Filters.Builder.AND));
    }

    @Test
    public void lookTrailingOrOperatorTest() {
        String key = "a";
        mBuilder.addFilter(key);
        mBuilder.or();

        assertFalse(mBuilder.build().getFilters().contains(Filters.Builder.OR));
    }

    @Test
    public void lookTrailingToOperatorTest() {
        String key = "a";
        mBuilder.addFilter(key);
        mBuilder.to();

        assertFalse(mBuilder.build().getFilters().contains(Filters.Builder.TO));
    }

    @Test
    public void lookTrailingNotOperatorTest() {
        String key = "a";
        mBuilder.addFilter(key);
        mBuilder.not();

        assertFalse(mBuilder.build().getFilters().contains(Filters.Builder.NOT));
    }

    @Test
    public void buildTest() {
        String key = "a";
        mBuilder.addFilter(key);
        String filters = mBuilder.build().getFilters();

        assertFalse(filters.lastIndexOf(Filters.Builder.EMPTY_SPACE) == filters.length() - 1);
        assertFalse(filters.indexOf(Filters.Builder.EMPTY_SPACE) == 0);
    }
}
