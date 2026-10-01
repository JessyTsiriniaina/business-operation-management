package io.jessytsiriniaina.internalrequestmanagement.mapper;

import io.jessytsiriniaina.internalrequestmanagement.dto.requesttype.RequestTypeResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.RequestType;

public final class RequestTypeMapper {

    private RequestTypeMapper() {}

    public static RequestTypeResponseDto toResponse(RequestType requestType) {
        return new RequestTypeResponseDto(
                requestType.getId(),
                requestType.getName(),
                requestType.getDescription(),
                requestType.isActive());
    }
}
