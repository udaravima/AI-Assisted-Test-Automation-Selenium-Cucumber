package com.sdp.m1.Generator.Model;

import java.util.List;
import java.util.Map;

/**
 * Represents the structure of the SRS JSON file.
 */
public class Srs {
    public String feature;
    public String featureDescription;
    public List<Requirement> requirements;

    public static class Requirement {
        public String id;
        public String label;
        public String type;
        public List<Map<String, Object>> validations;
    }
}
