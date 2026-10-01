package io.jessytsiriniaina.internalrequestmanagement.dto.requesttype;

public record RequestTypeResponseDto(
        Long id,
        String name,
        String description,
        boolean active) {}
