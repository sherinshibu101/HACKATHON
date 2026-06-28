package com.communityheroai.location;

public record LocationSearchResponse(
        Double latitude,
        Double longitude,
        String formattedAddress,
        String country,
        String state,
        String district,
        String city,
        String locality,
        String postalCode
) {
}
