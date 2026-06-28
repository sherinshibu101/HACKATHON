package com.communityheroai.location;

public record ReverseGeocodeResponse(
        Double latitude,
        Double longitude,
        String country,
        String state,
        String district,
        String city,
        String locality,
        String ward,
        String postalCode,
        String road,
        String formattedAddress,
        boolean resolved,
        String message
) {
}
