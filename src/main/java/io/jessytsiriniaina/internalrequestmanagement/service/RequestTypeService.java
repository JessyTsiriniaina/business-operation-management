package io.jessytsiriniaina.internalrequestmanagement.service;

import io.jessytsiriniaina.internalrequestmanagement.dto.requesttype.RequestTypeResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.entity.RequestType;
import io.jessytsiriniaina.internalrequestmanagement.exception.ResourceNotFoundException;
import io.jessytsiriniaina.internalrequestmanagement.mapper.RequestTypeMapper;
import io.jessytsiriniaina.internalrequestmanagement.repository.RequestTypeRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RequestTypeService {

    private final RequestTypeRepository requestTypeRepository;

    public RequestTypeService(RequestTypeRepository requestTypeRepository) {
        this.requestTypeRepository = requestTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestTypeResponseDto> findAll() {
        return requestTypeRepository.findAll(Sort.by("name")).stream()
                .map(RequestTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestTypeResponseDto findById(Long id) {
        RequestType requestType =
                requestTypeRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("RequestType not found: " + id));
        return RequestTypeMapper.toResponse(requestType);
    }
}
