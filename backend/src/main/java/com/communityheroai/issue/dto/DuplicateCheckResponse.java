package com.communityheroai.issue.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DuplicateCheckResponse {
    boolean duplicateWarning;
    List<PossibleDuplicateIssueResponse> possibleDuplicateIssues;
}
