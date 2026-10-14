
package com.akhil.coordinationhub.dto;

public record ResourceWriteRequest(
        long fencingToken,
        String value
) {}
