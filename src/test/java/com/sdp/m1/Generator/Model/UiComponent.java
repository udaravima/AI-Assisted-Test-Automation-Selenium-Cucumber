package com.sdp.m1.Generator.Model;

import java.util.Map;
import java.util.Set;

/**
 * Represents the structure of the JSON output from WebPageExtractorJSON.
 * This is a simplified version focusing on what's needed for correlation.
 */
public class UiComponent {
    public String type;
    public Set<Field> fields;

    public static class Field {
        public String selector;
        public String text;
        public String type;
        public String name;
        public String label;
    }
}
