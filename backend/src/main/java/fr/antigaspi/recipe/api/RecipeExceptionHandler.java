package fr.antigaspi.recipe.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.antigaspi.common.api.ApiError;
import fr.antigaspi.recipe.llm.RecipeGenerationException;

/**
 * Traduit en 502 un échec du LLM : la requête est valide, c'est le service en amont qui n'a pas répondu correctement.
 */
@RestControllerAdvice(assignableTypes = RecipeController.class)
class RecipeExceptionHandler {

    @ExceptionHandler(RecipeGenerationException.class)
    ResponseEntity<ApiError> echecDuLlm(RecipeGenerationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(e.getMessage()));
    }
}
