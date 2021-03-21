package com.algolia.search.saas.helpers;

/**
 * Builder class to help creating filter strings for algolia
 *
 * @author Felipe Conde (felipe@dice.fm)
 *         On 31/10/2016.
 */

public class Filters {

    private String filters;

    private Filters(String filters) {
        this.filters = filters;
    }

    public String getFilters() {
        return filters;
    }

    public enum Operator {
        EQUALS("="),
        GREATER_THAN(">"),
        SMALLER_THAN("<"),
        GREATER_THAN_EQUALS(">="),
        SMALLER_THAN_EQUALS("<=");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Builder {

        protected static final String OR = "OR";
        protected static final String AND = "AND";
        protected static final String TO = "TO";
        protected static final String NOT = "NOT";
        static final String[] Operators = {OR, AND, TO, NOT};
        static final char EMPTY_SPACE = ' ';
        static final char OPEN_CURLY_BRACES = '(';
        static final char CLOSE_CURLY_BRACES = ')';
        static final char COLON = ':';
        StringBuilder filtersBuilder = new StringBuilder();

        public Builder or() {
            filtersBuilder.append(OR);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder and() {
            filtersBuilder.append(AND);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder to() {
            filtersBuilder.append(TO);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder not() {
            filtersBuilder.append(NOT);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder addFilter(String key) {
            filtersBuilder.append(key);
            //default operator
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder addFilter(String key, String value) {
            filtersBuilder.append(key);
            //default operator
            filtersBuilder.append(COLON);
            filtersBuilder.append(value);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder addFilter(String key, String value, Operator operator) {
            filtersBuilder.append(key);
            //default operator
            filtersBuilder.append(operator.getValue());
            filtersBuilder.append(value);
            filtersBuilder.append(EMPTY_SPACE);
            return this;
        }

        public Builder addFilter(Filters filters) {
            if (!filters.getFilters().isEmpty()) {
                filtersBuilder.append(OPEN_CURLY_BRACES);
                filtersBuilder.append(filters.getFilters());
                filtersBuilder.append(CLOSE_CURLY_BRACES);
                filtersBuilder.append(EMPTY_SPACE);
            }
            return this;
        }

        private void lookTrailingOperators() {
            for (String operator : Operators) {
                if (filtersBuilder.toString().trim().endsWith(operator)) {
                    int lastIndexOf = filtersBuilder.lastIndexOf(operator);
                    filtersBuilder.delete(lastIndexOf, filtersBuilder.length() - 1);
                }
            }
        }

        public Filters build() {
            lookTrailingOperators();
            return new Filters(filtersBuilder.toString().trim());
        }
    }
}
