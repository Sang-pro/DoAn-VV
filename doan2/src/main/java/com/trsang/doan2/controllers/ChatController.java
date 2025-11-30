package com.trsang.doan2.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.requests.ChatRequest;
import com.trsang.doan2.dtos.requests.MessageRequest;
import com.trsang.doan2.dtos.responses.ChatMessageResponse;
import com.trsang.doan2.dtos.responses.ChatResponse;
import com.trsang.doan2.services.interfaces.IChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat API endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final IChatService chatService;

    @PostMapping
    @Operation(
        summary = "Create a new chat",
        description = "Create a new chat session with a specified AI model",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Chat created successfully",
                content = @Content(schema = @Schema(implementation = ChatResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<ChatResponse> createChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {
        
        ChatResponse createdChat = chatService.createChat(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdChat);
    }

    @GetMapping("/{chatId}")
    @Operation(
        summary = "Get chat by ID",
        description = "Retrieve a specific chat with its messages",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Chat retrieved successfully",
                content = @Content(schema = @Schema(implementation = ChatResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
        }
    )
    public ResponseEntity<ChatResponse> getChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Chat ID", required = true)
            @PathVariable UUID chatId) {
        
        ChatResponse chat = chatService.getChat(userDetails.getUsername(), chatId);
        return ResponseEntity.ok(chat);
    }

    @GetMapping
    @Operation(
        summary = "List user chats",
        description = "Get paginated list of user's chats",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Chats retrieved successfully",
                content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<Page<ChatResponse>> getUserChats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<ChatResponse> chats = chatService.getUserChats(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(chats);
    }

    @DeleteMapping("/{chatId}")
    @Operation(
        summary = "Delete chat",
        description = "Delete a specific chat and all its messages",
        responses = {
            @ApiResponse(responseCode = "204", description = "Chat deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
        }
    )
    public ResponseEntity<Void> deleteChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Chat ID", required = true)
            @PathVariable UUID chatId) {
        
        chatService.deleteChat(userDetails.getUsername(), chatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatId}/messages")
    @Operation(
        summary = "Send message to chat",
        description = "Send a message to the chat and get AI response",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Message processed, AI response returned",
                content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid message"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
        }
    )
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Chat ID", required = true)
            @PathVariable UUID chatId,
            @Valid @RequestBody MessageRequest request) {
        
        ChatMessageResponse response = chatService.sendMessage(userDetails.getUsername(), chatId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatId}/messages")
    @Operation(
        summary = "Get chat messages",
        description = "Retrieve all messages in a specific chat",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Messages retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Chat not found")
        }
    )
    public ResponseEntity<List<ChatMessageResponse>> getChatMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Chat ID", required = true)
            @PathVariable UUID chatId) {
        
        List<ChatMessageResponse> messages = chatService.getChatMessages(userDetails.getUsername(), chatId);
        return ResponseEntity.ok(messages);
    }
}
