package com.communityheroai.location;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Validated
@CrossOrigin
public class LocationController {
    private final ReverseGeocodingService reverseGeocodingService;

    @GetMapping("/reverse")
    public ReverseGeocodeResponse reverse(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
        return reverseGeocodingService.reverse(latitude, longitude);
    }

    @GetMapping("/search")
    public List<LocationSearchResponse> search(
            @RequestParam @jakarta.validation.constraints.Size(min = 2, max = 200) String query) {
        return reverseGeocodingService.search(query.trim());
    }
}
