package com.velox.auth.domain;

/** A saved delivery address. In Samannoud the landmark often matters more than the street. */
public record Address(long id, String label, String zone, String address, String landmark, String phone,
                      Double lat, Double lng) {
}
