package fr.antigaspi.ingredient.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.antigaspi.common.api.ApiError;
import fr.antigaspi.ingredient.service.IngredientNotFoundException;

/**
 * Traduit en 404 un aliment introuvable, avant le gestionnaire commun qui en ferait un 400.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = IngredientController.class)
class IngredientExceptionHandler {

    @ExceptionHandler(IngredientNotFoundException.class)
    ResponseEntity<ApiError> introuvable(IngredientNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
    }
}
