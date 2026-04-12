package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcTopicTag {
    private String name;
    private String slug;
}
