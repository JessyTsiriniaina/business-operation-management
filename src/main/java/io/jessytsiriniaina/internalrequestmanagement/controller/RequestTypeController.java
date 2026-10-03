package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.requesttype.RequestTypeResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.service.RequestTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/request-types")
@Tag(name = "Request types", description = "Read-only reference data (LEAVE, EQUIPMENT, PURCHASE, IT_SUPPORT, OTHER). Public.")
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    public RequestTypeController(RequestTypeService requestTypeService) {
        this.requestTypeService = requestTypeService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List request types", description = "Public. Returns the 5 seeded reference types.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of request types.")
    })
    public ResponseEntity<List<RequestTypeResponseDto>> findAll() {
        return ResponseEntity.ok(requestTypeService.findAll());
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get request type by id", description = "Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request type found.",
                    content = @Content(schema = @Schema(implementation = RequestTypeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Request type not found.")
    })
    public ResponseEntity<RequestTypeResponseDto> findById(
            @Parameter(description = "Request type id", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(requestTypeService.findById(id));
    }
}
