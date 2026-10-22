package fr.antigaspi.chat.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.antigaspi.chat.llm.ChatException;
import fr.antigaspi.common.api.ApiError;

/**
 * Traduit en 502 un échec du LLM : la requête est valide, c'est le service en amont qui n'a pas répondu correctement.
 */
@RestControllerAdvice(assignableTypes = ChatController.class)
class ChatExceptionHandler {

    @ExceptionHandler(ChatException.class)
    ResponseEntity<ApiError> echecDuLlm(ChatException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(e.getMessage()));
    }
}
