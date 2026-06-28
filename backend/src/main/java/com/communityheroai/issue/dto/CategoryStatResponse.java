package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueCategory;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryStatResponse {
    IssueCategory category;
    long count;
}
