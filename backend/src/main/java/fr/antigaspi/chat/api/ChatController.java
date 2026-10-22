package fr.antigaspi.chat.api;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.chat.llm.ChatAssistant;
import fr.antigaspi.chat.model.ChatReply;

/**
 * API REST du chat avec l'assistant, destinée au front. Une seule conversation, gardée en mémoire du serveur.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatAssistant assistant;

    public ChatController(ChatAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public ChatResponse send(@RequestBody ChatRequest request) {
        ChatReply reply = assistant.replyWithTools(request.message());
        return new ChatResponse(reply.text(), reply.tools().stream()
                .map(tool -> new ChatResponse.ToolCallResponse(tool.name(), tool.arguments())).toList());
    }

    @GetMapping
    public HistoryResponse history() {
        return new HistoryResponse(assistant.history().stream()
                .map(entry -> new HistoryResponse.Entry(entry.role().name().toLowerCase(Locale.ROOT), entry.text()))
                .toList());
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        assistant.reset();
        return ResponseEntity.noContent().build();
    }
}
