package com.sdp.m1.Generator.Model;

import java.util.List;
import java.util.Map;

/**
 * Represents a successful mapping between an SRS requirement and a UI component.
 */
public class CorrelatedField {
    public Srs.Requirement srsRequirement;
    public UiComponent.Field uiField;
    public MatchType matchType;

    public enum MatchType {
        ID,
        NAME,
        LABEL
    }
}