package io.jessytsiriniaina.internalrequestmanagement.controller;

import io.jessytsiriniaina.internalrequestmanagement.dto.requestcomment.CreateRequestCommentDto;
import io.jessytsiriniaina.internalrequestmanagement.dto.requestcomment.RequestCommentResponseDto;
import io.jessytsiriniaina.internalrequestmanagement.service.RequestCommentService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/requests/{requestId}/comments")
@Tag(name = "Request comments", description = "Append-only comments on requests. JWT required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class RequestCommentController {

    private final RequestCommentService requestCommentService;

    public RequestCommentController(RequestCommentService requestCommentService) {
        this.requestCommentService = requestCommentService;
    }

    @PostMapping
    @Operation(summary = "Add comment", description = "Appends a comment to a request as the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created.",
                    content = @Content(schema = @Schema(implementation = RequestCommentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed JSON."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Outside request read scope."),
            @ApiResponse(responseCode = "404", description = "Request not found.")
    })
    public ResponseEntity<RequestCommentResponseDto> create(
            @Parameter(description = "Request id", example = "1") @PathVariable Long requestId,
            @Valid @RequestBody CreateRequestCommentDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        RequestCommentResponseDto created = requestCommentService.create(requestId, dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List comments", description = "Paginated comments of a request, oldest first by default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of comments."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "404", description = "Request not found.")
    })
    public ResponseEntity<Page<RequestCommentResponseDto>> findAll(
            @Parameter(description = "Request id", example = "1") @PathVariable Long requestId,
            @Parameter(description = "Include soft-deleted comments", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<RequestCommentResponseDto> page = requestCommentService.findAll(requestId, includeDeleted, pageable);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment", description = "Soft delete. Author, MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted."),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT."),
            @ApiResponse(responseCode = "403", description = "Not the author and not MANAGER/ADMIN."),
            @ApiResponse(responseCode = "404", description = "Request or comment not found.")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Request id", example = "1") @PathVariable Long requestId,
            @Parameter(description = "Comment id", example = "1") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        requestCommentService.delete(requestId, commentId, principal);
        return ResponseEntity.noContent().build();
    }
}
