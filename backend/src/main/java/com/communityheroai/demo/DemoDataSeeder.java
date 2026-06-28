package com.communityheroai.demo;

import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.*;
import com.communityheroai.ledger.repository.CivicLedgerRepository;
import com.communityheroai.ledger.service.CivicLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final IssueRepository issueRepository;
    private final IssueVerificationRepository verificationRepository;
    private final IssueStatusHistoryRepository statusHistoryRepository;
    private final IssueMediaRepository mediaRepository;
    private final IssueEmailLogRepository emailLogRepository;
    private final IssueVisualFactCheckRepository visualFactCheckRepository;
    private final CivicLedgerRepository ledgerRepository;
    private final CivicLedgerService ledgerService;

    @Value("${app.demo.seed-on-start:false}")
    private boolean seedOnStart;

    @Value("${app.demo.reset-on-start:true}")
    private boolean resetOnStart;

    @Value("${app.demo.primary-email:}")
    private String primaryEmail;

    @Value("${app.demo.media-base-url:http://localhost:8080}")
    private String mediaBaseUrl;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedOnStart) return;
        seed(resetOnStart);
    }

    @Transactional
    public int seed(boolean reset) {
        List<String> preservedEmails = preservedContributorEmails();
        if (reset) {
            resetCivicData();
        } else if (issueRepository.count() > 0) {
            return 0;
        }
        seedDemoData(preservedEmails);
        return (int) issueRepository.count();
    }

    private List<String> preservedContributorEmails() {
        Set<String> emails = new LinkedHashSet<>();
        if (primaryEmail != null && !primaryEmail.isBlank()) emails.add(primaryEmail.trim());
        issueRepository.findAll().forEach(issue -> addEmail(emails, issue.getReporterEmail()));
        verificationRepository.findAll().forEach(verification -> addEmail(emails, verification.getVerifierEmail()));
        return new ArrayList<>(emails);
    }

    private void addEmail(Set<String> emails, String email) {
        if (email != null && !email.isBlank()) emails.add(email.trim());
    }

    private void resetCivicData() {
        visualFactCheckRepository.deleteAllInBatch();
        emailLogRepository.deleteAllInBatch();
        statusHistoryRepository.deleteAllInBatch();
        verificationRepository.deleteAllInBatch();
        mediaRepository.deleteAllInBatch();
        ledgerRepository.deleteAllInBatch();
        issueRepository.deleteAllInBatch();
    }

    private void seedDemoData(List<String> preservedEmails) {
        String primary = emailAt(preservedEmails, 0, "asha.demo@communityhero.ai");
        String second = emailAt(preservedEmails, 1, "nss.team@communityhero.ai");
        String third = emailAt(preservedEmails, 2, "ward7.resident@communityhero.ai");
        LocalDateTime now = LocalDateTime.now();

        Issue showcase = saveIssue(Issue.builder()
                .title("Burst water pipe flooding the school road")
                .reporterName("Asha Nair")
                .reporterEmail(primary)
                .description("A broken roadside pipe is continuously leaking near St. Mary School. Water is spreading across the road, creating slippery conditions and worsening an existing road cavity.")
                .category(IssueCategory.WATER_LEAKAGE)
                .status(IssueStatus.IN_PROGRESS)
                .severity(IssueSeverity.CRITICAL)
                .latitude(9.5916)
                .longitude(76.5222)
                .ward("Ward 7")
                .locality("Kanjikuzhy School Road")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686004")
                .formattedAddress("St. Mary School Road, Kanjikuzhy, Kottayam, Kerala 686004, India")
                .locationAccuracyMeters(18.0)
                .locationSource(LocationSource.GPS)
                .recommendedDepartment("Kerala Water Authority and Public Works Department")
                .impactScore(94)
                .riskExplanation("Continuous water flow can weaken the road base, create skid risk for two-wheelers, and interrupt school commute traffic.")
                .suggestedAction("Shut the leaking line, barricade the wet stretch, and dispatch a joint water works and road maintenance crew.")
                .complaintDraft("Respected Sir/Madam, I would like to report a burst water pipeline near St. Mary School Road, Kanjikuzhy. The leakage is flooding the road and creating a safety hazard for students and vehicles. Kindly arrange urgent repair and temporary barricading.")
                .escalationMessage("This verified issue is affecting a school route and poses a public safety risk. Kindly escalate to the ward engineer and water authority maintenance supervisor.")
                .resolutionUrgency("Immediate response within 24 hours")
                .aiGeneratedAt(now.minusDays(4))
                .authorityEmailSentAt(now.minusDays(3))
                .authorityEmailRecipient("water-authority-demo@example.gov")
                .dispatchDepartment("Kerala Water Authority")
                .dispatchPriority("CRITICAL")
                .dispatchCitizenNotification("A repair crew has been assigned and temporary barricading is in progress.")
                .dispatchAnalyzedAt(now.minusDays(3))
                .build(), now.minusDays(9));
        addMedia(showcase, "demo-water-leak-school-road.jfif", "demo-water-leak-school-road.jfif", ImageValidationStatus.VALID,
                88, "Google Cloud Vision found pipe/water evidence consistent with WATER_LEAKAGE.",
                "Water 93%, Pipe 88%, Road surface 77%, Asphalt 66%");
        addVerifications(showcase, List.of(
                verifier("NSS Volunteer Team", second, "Water is flowing across the road during school hours."),
                verifier("Parent Association", "parent.rep@communityhero.ai", "Children are walking around the flooded section."),
                verifier("Ward 7 Shopkeeper", third, "The leak has continued since yesterday morning.")
        ));
        addHistory(showcase, null, IssueStatus.REPORTED, "Asha Nair", StatusActorType.COMMUNITY,
                "Citizen submitted report with GPS location and visual evidence.", null);
        addHistory(showcase, IssueStatus.REPORTED, IssueStatus.VERIFIED, "Community Hero AI", StatusActorType.SYSTEM,
                "Three community verifications received.", null);
        addHistory(showcase, IssueStatus.VERIFIED, IssueStatus.ESCALATED, "Authority Email Agent", StatusActorType.SYSTEM,
                "Complaint emailed to configured authority: water-authority-demo@example.gov", null);
        addHistory(showcase, IssueStatus.ESCALATED, IssueStatus.IN_PROGRESS, "Ward Engineer Demo", StatusActorType.AUTHORITY,
                "Valve isolation requested and field crew assigned.", "https://example.com/evidence/water-leak-work-order");
        addEmailLog(showcase, "water-authority-demo@example.gov", IssueEmailStatus.SENT);
        addFactCheck(showcase, VisualVerificationResult.LIKELY_VALID, 91,
                "Street-view baseline shows a dry road surface; uploaded image shows new water flow from a roadside pipe. The issue appears recent and valid.");
        appendLedger(showcase, "ISSUE_REPORTED", "Asha Nair");
        appendLedger(showcase, "COMMUNITY_VERIFIED", "Community Hero AI");
        appendLedger(showcase, "AUTHORITY_ESCALATED", "Authority Email Agent");
        appendLedger(showcase, "STATUS_UPDATED", "Ward Engineer Demo");

        Issue duplicate = saveIssue(Issue.builder()
                .title("Second citizen report for school road pipe leak")
                .reporterName("Rahul Varghese")
                .reporterEmail("rahul.demo@communityhero.ai")
                .description("Another resident confirms the same broken water pipe near St. Mary School. Water is still spreading across the school road and vehicles are slowing down near the flooded stretch.")
                .category(IssueCategory.WATER_LEAKAGE)
                .status(IssueStatus.REPORTED)
                .severity(IssueSeverity.CRITICAL)
                .latitude(9.59161)
                .longitude(76.52221)
                .ward("Ward 7")
                .locality("Kanjikuzhy School Road")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686004")
                .formattedAddress("St. Mary School Road, Kanjikuzhy, Kottayam, Kerala 686004, India")
                .locationAccuracyMeters(24.0)
                .locationSource(LocationSource.MAP_PIN)
                .recommendedDepartment("Kerala Water Authority")
                .impactScore(94)
                .riskExplanation("Duplicate community signal for the same critical water leak corridor.")
                .suggestedAction("Attach this resident observation to the verified school-road leak and update residents through the original ticket.")
                .complaintDraft("Respected Sir/Madam, another resident confirms the same burst water pipeline near St. Mary School Road, Kanjikuzhy. The leakage is flooding the school road and needs urgent repair.")
                .escalationMessage("Possible duplicate of a verified high-risk leak. Please consolidate field response.")
                .resolutionUrgency("Within 24 hours")
                .aiGeneratedAt(now.minusDays(2))
                .build(), now.minusDays(2));
        addMedia(duplicate, "demo-duplicate-water-leak.jfif", "demo-duplicate-water-leak.jfif", ImageValidationStatus.VALID,
                81, "Google Cloud Vision matched water/pipe labels to WATER_LEAKAGE.",
                "Water 89%, Pipe 81%, Road 70%");
        addHistory(duplicate, null, IssueStatus.REPORTED, "Rahul Varghese", StatusActorType.COMMUNITY,
                "Potential duplicate report submitted nearby.", null);

        Issue wrongCategory = saveIssue(Issue.builder()
                .title("Possible pothole photo actually shows broken pipe")
                .reporterName("Demo Citizen")
                .reporterEmail("demo.citizen@communityhero.ai")
                .description("This scenario demonstrates image validation catching a wrong selected category.")
                .category(IssueCategory.POTHOLE)
                .status(IssueStatus.REPORTED)
                .severity(IssueSeverity.MEDIUM)
                .latitude(9.5941)
                .longitude(76.5240)
                .ward("Ward 7")
                .locality("Kanjikuzhy")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686004")
                .formattedAddress("Kanjikuzhy, Kottayam")
                .locationAccuracyMeters(30.0)
                .locationSource(LocationSource.MANUAL)
                .recommendedDepartment("Public Works Department")
                .impactScore(48)
                .riskExplanation("Selected category and uploaded evidence do not align.")
                .suggestedAction("Ask reporter to switch category to water leakage or submit clearer pothole evidence.")
                .complaintDraft("Respected Sir/Madam, this report needs evidence review before routing.")
                .escalationMessage("Manual review required due to category mismatch.")
                .resolutionUrgency("Review within 72 hours")
                .aiGeneratedAt(now.minusDays(1))
                .build(), now.minusDays(1));
        addMedia(wrongCategory, "demo-water-pipe-marked-pothole.jfif", "demo-water-pipe-marked-pothole.jfif", ImageValidationStatus.SUSPECT,
                0, "Google Cloud Vision suggests this image looks more like water leakage than pothole. Manual review recommended.",
                "Water 91%, Pipe 84%, Asphalt 70%, Road surface 62%");

        Issue waste = saveIssue(Issue.builder()
                .title("Garbage pile blocking market footpath")
                .reporterName("Market Residents Forum")
                .reporterEmail("market.forum@communityhero.ai")
                .description("Waste has been dumped near the market entrance for several days. The smell is increasing and pedestrians are forced onto the road.")
                .category(IssueCategory.WASTE_MANAGEMENT)
                .status(IssueStatus.VERIFIED)
                .severity(IssueSeverity.HIGH)
                .latitude(9.5884)
                .longitude(76.5201)
                .ward("Ward 3")
                .locality("Baker Junction Market")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686001")
                .formattedAddress("Baker Junction Market, Kottayam")
                .locationAccuracyMeters(20.0)
                .locationSource(LocationSource.GPS)
                .recommendedDepartment("Municipal Sanitation Department")
                .impactScore(76)
                .riskExplanation("Uncollected waste can attract pests and obstruct pedestrian movement in a busy market area.")
                .suggestedAction("Schedule immediate waste pickup and monitor repeated dumping.")
                .complaintDraft("Respected Sir/Madam, garbage has accumulated near Baker Junction Market and requires urgent clearance.")
                .escalationMessage("The issue has community verification and needs sanitation department action.")
                .resolutionUrgency("Within 48 hours")
                .aiGeneratedAt(now.minusDays(6))
                .build(), now.minusDays(8));
        addVerifications(waste, List.of(
                verifier("Fish Vendor Association", "vendor.association@communityhero.ai", "Waste is blocking customer access."),
                verifier("Morning Walkers Club", "walkers@communityhero.ai", "The smell is noticeable from the main road."),
                verifier("Ward 3 Resident", "ward3.resident@communityhero.ai", "This has been unresolved for a week.")
        ));
        addHistory(waste, null, IssueStatus.REPORTED, "Market Residents Forum", StatusActorType.COMMUNITY,
                "Citizen group reported waste accumulation.", null);
        addHistory(waste, IssueStatus.REPORTED, IssueStatus.VERIFIED, "Community Hero AI", StatusActorType.SYSTEM,
                "Three community verifications received.", null);

        Issue streetlight = saveIssue(Issue.builder()
                .title("Streetlight not working near bus stop")
                .reporterName("Anu Thomas")
                .reporterEmail("anu.thomas@communityhero.ai")
                .description("The streetlight near the bus stop has not worked for two nights, making the stop unsafe after 7 PM.")
                .category(IssueCategory.STREETLIGHT_DAMAGE)
                .status(IssueStatus.RESOLVED)
                .severity(IssueSeverity.MEDIUM)
                .latitude(9.5852)
                .longitude(76.5268)
                .ward("Ward 5")
                .locality("Collectorate Bus Stop")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686002")
                .formattedAddress("Collectorate Bus Stop, Kottayam")
                .locationAccuracyMeters(15.0)
                .locationSource(LocationSource.GPS)
                .recommendedDepartment("Electrical Maintenance Wing")
                .impactScore(58)
                .riskExplanation("Poor lighting increases safety concerns for commuters.")
                .suggestedAction("Replace faulty bulb and inspect wiring.")
                .complaintDraft("Respected Sir/Madam, the streetlight near Collectorate Bus Stop is not functioning and requires repair.")
                .escalationMessage("Resolved demo issue for status history walkthrough.")
                .resolutionUrgency("Within 72 hours")
                .aiGeneratedAt(now.minusDays(10))
                .build(), now.minusDays(12));
        addHistory(streetlight, null, IssueStatus.REPORTED, "Anu Thomas", StatusActorType.COMMUNITY,
                "Streetlight outage reported.", null);
        addHistory(streetlight, IssueStatus.REPORTED, IssueStatus.IN_PROGRESS, "Electrical Supervisor Demo", StatusActorType.AUTHORITY,
                "Technician assigned for bulb replacement.", "https://example.com/evidence/streetlight-work-order");
        addHistory(streetlight, IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, "Electrical Supervisor Demo", StatusActorType.AUTHORITY,
                "Bulb replaced and lighting restored.", "https://example.com/evidence/streetlight-after");

        Issue drainage = saveIssue(Issue.builder()
                .title("Open drain overflowing after rain")
                .reporterName("Ward 11 Youth Collective")
                .reporterEmail("ward11.youth@communityhero.ai")
                .description("Drain water is overflowing onto the residential lane after rain. Mosquito breeding and pedestrian safety are concerns.")
                .category(IssueCategory.DRAINAGE_ISSUE)
                .status(IssueStatus.REPORTED)
                .severity(IssueSeverity.CRITICAL)
                .latitude(9.6005)
                .longitude(76.5162)
                .ward("Ward 11")
                .locality("Kodimatha Residential Lane")
                .city("Kottayam")
                .district("Kottayam")
                .state("Kerala")
                .country("India")
                .postalCode("686013")
                .formattedAddress("Kodimatha Residential Lane, Kottayam")
                .locationAccuracyMeters(28.0)
                .locationSource(LocationSource.MAP_PIN)
                .recommendedDepartment("Drainage and Public Health Engineering")
                .impactScore(91)
                .riskExplanation("Overflowing drain creates sanitation and vector-borne disease risks.")
                .suggestedAction("Clear drain blockage and disinfect affected lane.")
                .complaintDraft("Respected Sir/Madam, an open drain is overflowing in Kodimatha residential lane and requires urgent clearing.")
                .escalationMessage("Critical unresolved sanitation risk. Escalate to drainage maintenance team.")
                .resolutionUrgency("Within 24 hours")
                .aiGeneratedAt(now.minusDays(5))
                .build(), now.minusDays(10));
        addHistory(drainage, null, IssueStatus.REPORTED, "Ward 11 Youth Collective", StatusActorType.COMMUNITY,
                "Drainage overflow reported after rain.", null);
    }

    private String emailAt(List<String> emails, int index, String fallback) {
        return emails.size() > index ? emails.get(index) : fallback;
    }

    private Issue saveIssue(Issue issue, LocalDateTime createdAt) {
        Issue saved = issueRepository.save(issue);
        saved.setCreatedAt(createdAt);
        saved.setUpdatedAt(createdAt.plusHours(2));
        return issueRepository.save(saved);
    }

    private VerificationSeed verifier(String name, String email, String comment) {
        return new VerificationSeed(name, email, comment);
    }

    private void addVerifications(Issue issue, List<VerificationSeed> seeds) {
        for (VerificationSeed seed : seeds) {
            verificationRepository.save(IssueVerification.builder()
                    .issue(issue)
                    .verifierName(seed.name())
                    .verifierEmail(seed.email())
                    .comment(seed.comment())
                    .build());
        }
    }

    private void addMedia(Issue issue, String storageKey, String filename, ImageValidationStatus status,
                          Integer confidence, String summary, String labels) {
        IssueMediaType type = filename.endsWith(".mp4") ? IssueMediaType.VIDEO : IssueMediaType.IMAGE;
        mediaRepository.save(IssueMedia.builder()
                .issue(issue)
                .mediaType(type)
                .mediaUrl(cleanBaseUrl(mediaBaseUrl) + "/uploads/" + storageKey)
                .storageKey(storageKey)
                .originalFilename(filename)
                .contentType(type == IssueMediaType.VIDEO ? "video/mp4" : "image/jpeg")
                .fileSize(type == IssueMediaType.VIDEO ? 8_400_000L : 920_000L)
                .processingStatus(MediaProcessingStatus.READY)
                .validationStatus(status)
                .validationConfidence(confidence)
                .validationSummary(summary)
                .validationLabels(labels)
                .validatedAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    private void addHistory(Issue issue, IssueStatus from, IssueStatus to, String actorName,
                            StatusActorType actorType, String note, String evidenceUrl) {
        statusHistoryRepository.save(IssueStatusHistory.builder()
                .issue(issue)
                .fromStatus(from)
                .toStatus(to)
                .actorName(actorName)
                .actorType(actorType)
                .note(note)
                .evidenceUrl(evidenceUrl)
                .build());
    }

    private void addEmailLog(Issue issue, String recipient, IssueEmailStatus status) {
        emailLogRepository.save(IssueEmailLog.builder()
                .issue(issue)
                .recipient(recipient)
                .subject("Urgent civic complaint: " + issue.getTitle())
                .body(issue.getComplaintDraft())
                .status(status)
                .build());
    }

    private void addFactCheck(Issue issue, VisualVerificationResult result, int confidence, String reasoning) {
        IssueMedia media = mediaRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId()).stream()
                .filter(item -> item.getMediaType() == IssueMediaType.IMAGE)
                .findFirst()
                .orElse(null);
        visualFactCheckRepository.save(IssueVisualFactCheck.builder()
                .issue(issue)
                .issueMedia(media)
                .status(VisualFactCheckStatus.COMPLETED)
                .verificationResult(result)
                .confidenceScore(confidence)
                .baselineImageUrl("https://maps.googleapis.com/maps/api/streetview?demo=baseline")
                .userImageUrl(media == null ? null : media.getMediaUrl())
                .reasoningReport(reasoning)
                .riskFlags("Recent water flow; road-safety risk; school commute corridor")
                .build());
    }

    private void appendLedger(Issue issue, String eventType, String actorName) {
        ledgerService.append(eventType, "ISSUE", issue.getId(), actorName,
                "{\"issueId\":%s,\"title\":\"%s\",\"status\":\"%s\"}"
                        .formatted(issue.getId(), issue.getTitle().replace("\"", "'"), issue.getStatus()));
    }

    private String cleanBaseUrl(String value) {
        if (value == null || value.isBlank()) return "http://localhost:8080";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record VerificationSeed(String name, String email, String comment) {}
}
