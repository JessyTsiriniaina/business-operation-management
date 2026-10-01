package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.requesttype.RequestTypeResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.service.RequestTypeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/request-types")
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    public RequestTypeController(RequestTypeService requestTypeService) {
        this.requestTypeService = requestTypeService;
    }

    @GetMapping
    public ResponseEntity<List<RequestTypeResponseDto>> findAll() {
        return ResponseEntity.ok(requestTypeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestTypeResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(requestTypeService.findById(id));
    }
}
