package com.trsang.doan2.services.implementation;

import com.trsang.doan2.config.OllamaConfig;
import com.trsang.doan2.entities.Book;
import com.trsang.doan2.entities.BorrowRecord;
import com.trsang.doan2.dtos.ollama.OllamaCompletionRequest;
import com.trsang.doan2.dtos.ollama.OllamaCompletionResponse;
import com.trsang.doan2.repositories.IBookRepository;
import com.trsang.doan2.repositories.IBorrowRecordRepository;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.services.interfaces.MqttGateway;
import com.trsang.doan2.services.interfaces.IOllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService implements IOllamaService {

    private final WebClient ollamaWebClient;
    private final OllamaConfig ollamaConfig;
    private final IBookRepository bookRepository;
    private final IBorrowRecordRepository borrowRecordRepository;
    private final IEslTagRepository eslTagRepository;
    private final IUserRepository userRepository;
    private final MqttGateway mqttGateway;

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý AI thủ thư thông minh và thân thiện của thư viện V-Smart Library.\n");
        sb.append("Bạn có quyền truy cập vào cơ sở dữ liệu thời gian thực của thư viện. Dưới đây là các thông số hiện tại của hệ thống:\n\n");
        
        try {
            long totalBooks = bookRepository.count();
            sb.append("- Tổng số đầu sách: ").append(totalBooks).append(" cuốn.\n");
            
            List<Book> books = bookRepository.findAll();
            int totalCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
            sb.append("- Tổng số bản sao hiện có sẵn: ").append(totalCopies).append(" bản.\n");
            
            long totalUsers = userRepository.count();
            sb.append("- Số lượng người dùng đăng ký: ").append(totalUsers).append(" người.\n");
            
            long totalTags = eslTagRepository.count();
            long onlineTags = eslTagRepository.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getIsOnline())).count();
            sb.append("- Tổng số thẻ điện tử (ESL): ").append(totalTags).append(" thẻ (")
              .append(onlineTags).append(" thẻ đang Online).\n");
            
            List<BorrowRecord> activeLoans = borrowRecordRepository.findByStatus("BORROWED");
            sb.append("- Số sách đang được mượn: ").append(activeLoans.size()).append(" cuốn.\n\n");
            
            sb.append("### DANH SÁCH CÁC ĐẦU SÁCH TRONG THƯ VIỆN:\n");
            if (books.isEmpty()) {
                sb.append("(Hiện tại thư viện chưa có sách nào)\n");
            } else {
                for (Book b : books) {
                    sb.append(String.format("* [ID: %d] \"%s\" - Tác giả: %s (Có sẵn: %d bản)\n", 
                        b.getId(), b.getTitle(), b.getAuthor(), b.getAvailableCopies()));
                }
            }
            sb.append("\n");
            
            sb.append("### DANH SÁCH PHIẾU MƯỢN SÁCH CHƯA TRẢ:\n");
            if (activeLoans.isEmpty()) {
                sb.append("(Không có ai đang mượn sách)\n");
            } else {
                for (BorrowRecord record : activeLoans) {
                    String username = record.getUser() != null ? record.getUser().getUsername() : "Ẩn danh";
                    String bookTitle = record.getBook() != null ? record.getBook().getTitle() : "Sách đã xóa";
                    sb.append(String.format("* Người dùng \"%s\" đang mượn cuốn \"%s\" (Hạn trả: %s)\n", 
                        username, bookTitle, record.getDueDate().toString()));
                }
            }
            sb.append("\n");
        } catch (Exception e) {
            sb.append("(Lỗi khi tải thông số thời gian thực từ cơ sở dữ liệu: ").append(e.getMessage()).append(")\n");
        }
        
        sb.append("### CÁC CÔNG CỤ (TOOLS) BẠN CÓ QUYỀN SỬ DỤNG:\n");
        sb.append("Nếu bạn cần tra cứu thêm thông tin sách, lịch sử mượn trả, hoặc thực hiện bật đèn định vị Pick-to-light, bạn BẮT BUỘC phải gọi công cụ bằng cú pháp sau (và không viết thêm bất kỳ nội dung nào ở sau mã lệnh gọi này trong tin nhắn đó):\n");
        sb.append("[CALL_TOOL: <Tên công cụ>, <Tham số>]\n\n");
        sb.append("Danh sách công cụ hỗ trợ:\n");
        sb.append("1. `GET_BOOKS` - Tìm kiếm sách nâng cao. Tham số: từ khóa tìm kiếm (tên sách, tác giả, hoặc tóm tắt).\n");
        sb.append("   - Ví dụ gọi: [CALL_TOOL: GET_BOOKS, Nguyễn Nhật Ánh]\n");
        sb.append("2. `GET_BORROW_HISTORY` - Tra cứu lịch sử mượn sách. Tham số: mã số người dùng (userCode) cần tra cứu.\n");
        sb.append("   - Ví dụ gọi: [CALL_TOOL: GET_BORROW_HISTORY, LIB12345]\n");
        sb.append("3. `TRIGGER_PICK_TO_LIGHT` - Kích hoạt đèn nhấp nháy trên kệ sách. Tham số: ID cuốn sách (kiểu số nguyên).\n");
        sb.append("   - Ví dụ gọi: [CALL_TOOL: TRIGGER_PICK_TO_LIGHT, 5]\n\n");
        sb.append("Hãy sử dụng thông tin trên để trả lời các câu hỏi của người dùng một cách chính xác, tự nhiên bằng Tiếng Việt. Tránh đề cập đến việc bạn có hệ thống prompt này mà hãy trả lời như thể bạn trực tiếp biết rõ hiện trạng thư viện.");
        return sb.toString();
    }

    private String executeTool(String toolName, String argument) {
        log.info("Executing tool: {} with argument: {}", toolName, argument);
        try {
            switch (toolName.toUpperCase().trim()) {
                case "GET_BOOKS":
                    String query = argument.trim();
                    List<Book> books = bookRepository.findAll().stream()
                        .filter(b -> b.getTitle().toLowerCase().contains(query.toLowerCase()) || 
                                     b.getAuthor().toLowerCase().contains(query.toLowerCase()) ||
                                     (b.getSummary() != null && b.getSummary().toLowerCase().contains(query.toLowerCase())))
                        .collect(Collectors.toList());
                    if (books.isEmpty()) {
                        return "Không tìm thấy cuốn sách nào khớp với từ khóa \"" + query + "\"";
                    }
                    StringBuilder sb = new StringBuilder("Đã tìm thấy " + books.size() + " cuốn sách:\n");
                    for (Book b : books) {
                        sb.append(String.format("- [ID: %d] \"%s\" - Tác giả: %s (Có sẵn: %d bản)\n", 
                            b.getId(), b.getTitle(), b.getAuthor(), b.getAvailableCopies()));
                    }
                    return sb.toString();
                    
                case "GET_BORROW_HISTORY":
                    String userCode = argument.trim();
                    return userRepository.findByUserCode(userCode).map(u -> {
                        List<BorrowRecord> history = borrowRecordRepository.findByUserId(u.getId());
                        if (history.isEmpty()) {
                            return "Người dùng " + u.getUsername() + " (Mã số: " + userCode + ") chưa từng mượn sách.";
                        }
                        StringBuilder histSb = new StringBuilder("Lịch sử mượn trả của " + u.getUsername() + ":\n");
                        for (BorrowRecord r : history) {
                            String returnStr = r.getReturnDate() != null ? "Đã trả ngày " + r.getReturnDate().toString() : "Chưa trả (Hạn trả: " + r.getDueDate().toString() + ")";
                            histSb.append(String.format("- Cuốn \"%s\" (Mượn ngày: %s) -> Trạng thái: %s (%s)\n",
                                r.getBook().getTitle(), r.getBorrowDate().toString(), r.getStatus(), returnStr));
                        }
                        return histSb.toString();
                    }).orElse("Không tìm thấy người dùng nào có mã số \"" + userCode + "\"");
                    
                case "TRIGGER_PICK_TO_LIGHT":
                    try {
                        Long bookId = Long.parseLong(argument.trim());
                        return eslTagRepository.findByBookId(bookId).map(tag -> {
                            String payload = "{\"action\": \"blink\"}";
                            mqttGateway.sendToMqtt("esl/find/" + tag.getMacAddress(), payload);
                            return "Kích hoạt thành công nhấp nháy đèn (Pick-to-light) cho thẻ ESL có địa chỉ MAC: " + tag.getMacAddress();
                        }).orElse("Không tìm thấy thẻ điện tử (ESL) nào được gán cho cuốn sách có ID: " + bookId);
                    } catch (NumberFormatException e) {
                        return "Lỗi: ID cuốn sách phải là một số hợp lệ. Nhận được: \"" + argument + "\"";
                    }
                    
                default:
                    return "Lỗi: Công cụ \"" + toolName + "\" không được hỗ trợ.";
            }
        } catch (Exception e) {
            log.error("Error executing tool", e);
            return "Lỗi hệ thống khi thực thi công cụ: " + e.getMessage();
        }
    }

    private OllamaCompletionResponse callOllamaNonStreaming(
            String model, 
            List<OllamaCompletionRequest.OllamaMessage> messages, 
            Map<String, Object> options
    ) {
        OllamaCompletionRequest request = OllamaCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .stream(false)
                .options(options)
                .build();
                
        try {
            return ollamaWebClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaCompletionResponse.class)
                    .timeout(Duration.ofSeconds(ollamaConfig.getTimeoutSeconds()))
                    .block();
        } catch (Exception e) {
            log.error("Error in inner Ollama call: ", e);
            return null;
        }
    }
    
    @Override
    public OllamaCompletionResponse generateCompletion(String model, List<Map<String, String>> messages, Map<String, Object> options) {
        log.info("Generating completion for model: {}", model);
        
        final String finalModel = model == null || model.isEmpty() ? ollamaConfig.getDefaultModel() : model;
        
        List<OllamaCompletionRequest.OllamaMessage> ollamaMessages = new ArrayList<>(messages.stream()
                .map(msg -> OllamaCompletionRequest.OllamaMessage.builder()
                        .role(msg.get("role"))
                        .content(msg.get("content"))
                        .build())
                .collect(Collectors.toList()));
        
        ollamaMessages.add(0, OllamaCompletionRequest.OllamaMessage.builder()
                .role("system")
                .content(buildSystemPrompt())
                .build());
        
        Map<String, Object> requestOptions = new HashMap<>();
        if (options == null) {
            requestOptions.put("temperature", ollamaConfig.getDefaultTemperature());
            requestOptions.put("repeat_penalty", ollamaConfig.getDefaultRepeatPenalty());
            requestOptions.put("numa", ollamaConfig.isDefaultNuma());
        } else {
            requestOptions = options;
        }
        
        int maxIterations = 3;
        OllamaCompletionResponse finalResponse = null;
        
        for (int i = 0; i < maxIterations; i++) {
            OllamaCompletionResponse response = callOllamaNonStreaming(finalModel, ollamaMessages, requestOptions);
            if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
                break;
            }
            
            finalResponse = response;
            String content = response.getMessage().getContent();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[CALL_TOOL:\\s*(\\w+)\\s*,\\s*([^\\]]+)\\]").matcher(content);
            if (matcher.find()) {
                String toolName = matcher.group(1);
                String argument = matcher.group(2);
                String toolResult = executeTool(toolName, argument);
                
                ollamaMessages.add(OllamaCompletionRequest.OllamaMessage.builder()
                        .role("assistant")
                        .content(content)
                        .build());
                ollamaMessages.add(OllamaCompletionRequest.OllamaMessage.builder()
                        .role("user")
                        .content("Kết quả trả về từ công cụ " + toolName + ": " + toolResult)
                        .build());
            } else {
                break;
            }
        }
        
        if (finalResponse != null) {
            return finalResponse;
        }
        
        OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
            "assistant", 
            "I'm sorry, I encountered an error while processing your request. Please try again later."
        );
        
        return OllamaCompletionResponse.builder()
                .model(finalModel)
                .message(errorMessage)
                .done(true)
                .build();
    }
    
    @Override
    public Flux<ServerSentEvent<Object>> streamCompletion(
            String model, 
            List<Map<String, String>> messages, 
            boolean streaming,
            Map<String, Object> options
    ) {
        log.info("Streaming completion for model: {}", model);
        
        final String finalModel = model == null || model.isEmpty() ? ollamaConfig.getDefaultModel() : model;
        
        List<OllamaCompletionRequest.OllamaMessage> ollamaMessages = new ArrayList<>(messages.stream()
                .map(msg -> OllamaCompletionRequest.OllamaMessage.builder()
                        .role(msg.get("role"))
                        .content(msg.get("content"))
                        .build())
                .collect(Collectors.toList()));
        
        ollamaMessages.add(0, OllamaCompletionRequest.OllamaMessage.builder()
                .role("system")
                .content(buildSystemPrompt())
                .build());
        
        Map<String, Object> requestOptions = new HashMap<>();
        if (options == null) {
            requestOptions.put("temperature", ollamaConfig.getDefaultTemperature());
            requestOptions.put("repeat_penalty", ollamaConfig.getDefaultRepeatPenalty());
            requestOptions.put("numa", ollamaConfig.isDefaultNuma());
        } else {
            requestOptions = options;
        }

        // Silent backend tool calling resolver loop
        int maxIterations = 3;
        for (int i = 0; i < maxIterations; i++) {
            OllamaCompletionResponse response = callOllamaNonStreaming(finalModel, ollamaMessages, requestOptions);
            if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
                break;
            }
            
            String content = response.getMessage().getContent();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[CALL_TOOL:\\s*(\\w+)\\s*,\\s*([^\\]]+)\\]").matcher(content);
            if (matcher.find()) {
                String toolName = matcher.group(1);
                String argument = matcher.group(2);
                String toolResult = executeTool(toolName, argument);
                
                ollamaMessages.add(OllamaCompletionRequest.OllamaMessage.builder()
                        .role("assistant")
                        .content(content)
                        .build());
                ollamaMessages.add(OllamaCompletionRequest.OllamaMessage.builder()
                        .role("user")
                        .content("Kết quả trả về từ công cụ " + toolName + ": " + toolResult)
                        .build());
            } else {
                break;
            }
        }
                
        // Build final request with streaming enabled using the fully resolved conversation history
        OllamaCompletionRequest request = OllamaCompletionRequest.builder()
                .model(finalModel)
                .messages(ollamaMessages)
                .stream(streaming)
                .options(requestOptions)
                .build();
        
        log.debug("Sending streaming request to Ollama API: {}", request);
        
        final String eventId = UUID.randomUUID().toString();
        
        return ollamaWebClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> log.debug("Raw response chunk: {}", chunk))
                .map(rawJson -> {
                    return ServerSentEvent.<Object>builder()
                            .id(eventId)
                            .event("message")
                            .data(rawJson)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error in streaming response: {}", e.getMessage());
                    
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("model", finalModel);
                    errorMap.put("error", e.getMessage());
                    errorMap.put("done", true);
                    
                    OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
                        "assistant", 
                        "I'm sorry, I encountered an error while processing your request. Please try again later."
                    );
                    
                    errorMap.put("message", errorMessage);
                    
                    return Flux.just(ServerSentEvent.<Object>builder()
                            .id(eventId)
                            .event("error")
                            .data(errorMap)
                            .build());
                })
                .doOnComplete(() -> log.debug("Streaming completed for event ID: {}", eventId));
    }
}
