package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.request.AssignRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.CancelRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.CreateRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.RejectRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.RequestResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.request.UpdateRequestDto;
import io.jessytsiriniaina.internalrequestmanagement.enums.RequestPriority;
import io.jessytsiriniaina.internalrequestmanagement.enums.RequestStatus;
import io.jessytsiriniaina.internalrequestmanagement.service.RequestService;
import io.jessytsiriniaina.internalrequestmanagement.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/requests")
@Tag(name = "Requests", description = "Request lifecycle and workflow actions. JWT required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @Operation(summary = "Create request", description = "Any authenticated user. URGENT priority requires a justification.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure, malformed JSON, or URGENT without justification."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "404", description = "Request type or department not found.")
    })
    public ResponseEntity<RequestResponseDto> create(
            @Valid @RequestBody CreateRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        RequestResponseDto created = requestService.create(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List requests", description = "Scope is role-based: EMPLOYEE sees own requests, "
            + "MANAGER sees own department, ADMIN sees all. Supports status/priority/type/assignee/department filters plus pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of requests."),
            @ApiResponse(responseCode = "400", description = "Invalid filter value, e.g. ?status=FOO."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    })
    public ResponseEntity<Page<RequestResponseDto>> findAll(
            @Parameter(description = "Filter by status", example = "PENDING") @RequestParam(required = false) RequestStatus status,
            @Parameter(description = "Filter by priority", example = "HIGH") @RequestParam(required = false) RequestPriority priority,
            @Parameter(description = "Filter by request type id", example = "1") @RequestParam(required = false) Long typeId,
            @Parameter(description = "Filter by creator user id", example = "1") @RequestParam(required = false) Long createdById,
            @Parameter(description = "Filter by assignee user id", example = "2") @RequestParam(required = false) Long assignedToId,
            @Parameter(description = "Filter by department id or name", example = "IT") @RequestParam(required = false) String department,
            @Parameter(description = "Include soft-deleted requests", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @ParameterObject @PageableDefault(size = 10, sort = "updatedAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        Page<RequestResponseDto> page =
                requestService.findAll(status, priority, typeId, createdById, assignedToId, department, includeDeleted, pageable, principal);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request by id", description = "Read scope follows R4: own / department / all by role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request found.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Outside read scope."),
            @ApiResponse(responseCode = "404", description = "Request not found.")
    })
    public ResponseEntity<RequestResponseDto> findById(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.findById(id, principal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update request", description = "Only while PENDING (R1). EMPLOYEE can edit own request only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request updated.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Not the owner or outside scope."),
            @ApiResponse(responseCode = "404", description = "Request not found."),
            @ApiResponse(responseCode = "409", description = "Illegal state, e.g. editing a non-PENDING request.")
    })
    public ResponseEntity<RequestResponseDto> update(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.update(id, dto, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete request", description = "Soft delete. EMPLOYEE can delete own request only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Request deleted."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Not the owner or outside scope."),
            @ApiResponse(responseCode = "404", description = "Request not found.")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        requestService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start-progress")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Start progress", description = "PENDING -> IN_PROGRESS. MANAGER or ADMIN only (R2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request moved to IN_PROGRESS.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "404", description = "Request not found."),
            @ApiResponse(responseCode = "409", description = "Illegal transition for current status.")
    })
    public ResponseEntity<RequestResponseDto> startProgress(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(requestService.startProgress(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Approve request", description = "MANAGER or ADMIN only (R2). A REJECTED request cannot be approved (R3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "404", description = "Request not found."),
            @ApiResponse(responseCode = "409", description = "Illegal transition, e.g. approving a PENDING or REJECTED request.")
    })
    public ResponseEntity<RequestResponseDto> approve(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(requestService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Reject request", description = "MANAGER or ADMIN only (R2). Requires a rejection reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "404", description = "Request not found."),
            @ApiResponse(responseCode = "409", description = "Illegal transition for current status.")
    })
    public ResponseEntity<RequestResponseDto> reject(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectRequestDto dto) {
        return ResponseEntity.ok(requestService.reject(id, dto));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Assign request", description = "Assigns the request to a user. MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request assigned.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "EMPLOYEE caller."),
            @ApiResponse(responseCode = "404", description = "Request or assignee not found."),
            @ApiResponse(responseCode = "409", description = "Illegal transition for current status.")
    })
    public ResponseEntity<RequestResponseDto> assign(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody AssignRequestDto dto) {
        return ResponseEntity.ok(requestService.assign(id, dto));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel request", description = "Owner, MANAGER or ADMIN. Typically from PENDING/IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request cancelled.",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Not the owner and not MANAGER/ADMIN."),
            @ApiResponse(responseCode = "404", description = "Request not found."),
            @ApiResponse(responseCode = "409", description = "Illegal transition for current status.")
    })
    public ResponseEntity<RequestResponseDto> cancel(
            @Parameter(description = "Request id", example = "1") @PathVariable Long id,
            @Valid @RequestBody CancelRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.cancel(id, dto, principal));
    }
}
