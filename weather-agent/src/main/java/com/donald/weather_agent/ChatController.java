package com.donald.weather_agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

  private final ChatClient chatClient;

  public ChatController(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @PostMapping("/chat")
  public ChatResponse chat(@RequestBody ChatRequest request) {

    String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
        ? "default"
        : request.conversationId();

    String answer = chatClient.prompt()
        .user(request.question())
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
    return new ChatResponse(answer);
  }

  public record ChatRequest(String question, String conversationId) { }

  public record ChatResponse(String answer) { }
}
