package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.LocationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class IssueRequest {
    @NotBlank
    @Size(max = 150)
    private String title;
    @Size(max = 100)
    private String reporterName;
    @jakarta.validation.constraints.Email
    @Size(max = 254)
    private String reporterEmail;
    @NotBlank
    @Size(max = 5000)
    private String description;
    @NotNull
    private IssueCategory category;
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;
    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
    @Size(max = 100)
    private String ward;
    @NotBlank
    @Size(max = 100)
    private String locality;
    @Size(max = 100)
    private String country;
    @Size(max = 100)
    private String state;
    @Size(max = 100)
    private String district;
    @Size(max = 100)
    private String city;
    @Size(max = 20)
    private String postalCode;
    @Size(max = 1000)
    private String formattedAddress;
    @DecimalMin("0.0")
    private Double locationAccuracyMeters;
    private LocationSource locationSource;
}
