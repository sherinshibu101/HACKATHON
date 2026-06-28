package com.communityheroai.issue.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WardStatResponse {
    String ward;
    long count;
}
