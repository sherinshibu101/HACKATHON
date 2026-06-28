package com.communityheroai.issue.service;

import com.communityheroai.issue.dto.IssueRequest;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.repository.IssueRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateDetectionServiceTest {
    private final IssueRepository issueRepository = mock(IssueRepository.class);
    private final DuplicateDetectionService service = new DuplicateDetectionService(issueRepository);

    @Test
    void findsSimilarOpenIssueWithinThreeHundredMeters() {
        IssueRequest request = request();
        Issue nearby = Issue.builder().id(7L).title("Large pothole on market road")
                .description("Deep pothole causing traffic near the market")
                .category(IssueCategory.POTHOLE).status(IssueStatus.REPORTED)
                .latitude(28.61395).longitude(77.20905).build();
        when(issueRepository.findByCategory(IssueCategory.POTHOLE))
                .thenReturn(List.of(nearby));

        assertThat(service.findDuplicates(request))
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.getId()).isEqualTo(7L);
                    assertThat(match.getDistanceMeters()).isLessThan(300);
                });
    }

    @Test
    void ignoresSimilarIssueOutsideRadius() {
        IssueRequest request = request();
        Issue farAway = Issue.builder().id(8L).title("Pothole on market road")
                .description("Deep pothole causing traffic near the market")
                .category(IssueCategory.POTHOLE).status(IssueStatus.REPORTED)
                .latitude(28.6239).longitude(77.2190).build();
        when(issueRepository.findByCategory(IssueCategory.POTHOLE))
                .thenReturn(List.of(farAway));

        assertThat(service.findDuplicates(request)).isEmpty();
    }

    @Test
    void includesResolvedIssueSoCitizenCanBeWarned() {
        IssueRequest request = request();
        Issue resolved = Issue.builder().id(9L).title("Large pothole on market road")
                .description("Deep pothole causing traffic near the market")
                .category(IssueCategory.POTHOLE).status(IssueStatus.RESOLVED)
                .latitude(28.61395).longitude(77.20905).build();
        when(issueRepository.findByCategory(IssueCategory.POTHOLE))
                .thenReturn(List.of(resolved));

        assertThat(service.findDuplicates(request))
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.getId()).isEqualTo(9L);
                    assertThat(match.getStatus()).isEqualTo(IssueStatus.RESOLVED);
                });
    }

    private IssueRequest request() {
        IssueRequest request = new IssueRequest();
        request.setTitle("Pothole near market road");
        request.setDescription("Deep pothole is causing traffic at the market");
        request.setCategory(IssueCategory.POTHOLE);
        request.setLatitude(28.6139);
        request.setLongitude(77.2090);
        request.setWard("Ward 12");
        request.setLocality("Central Market");
        return request;
    }
}
