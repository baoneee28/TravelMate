package com.travelmate.controller.api;

import com.travelmate.service.ChatbotService;
import com.travelmate.service.ChatbotService.ChatbotResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    public ChatbotApiController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> message(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userMessage = body.getOrDefault("message", "");
        String lastDestination = body.getOrDefault("lastDestination", "");
        String username = (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName()
                : null;
        ChatbotResponse response = chatbotService.processMessage(userMessage, username, lastDestination);
        return ResponseEntity.ok(Map.of(
                "intent",       response.intent(),
                "reply",        response.reply(),
                "quickReplies", response.quickReplies()
        ));
    }
}
