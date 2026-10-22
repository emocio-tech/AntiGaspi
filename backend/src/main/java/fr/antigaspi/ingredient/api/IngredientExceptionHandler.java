package fr.antigaspi.ingredient.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduit en 400 les filtres invalides de l'API de l'inventaire, avec un message lisible par le front.
 */
@RestControllerAdvice(assignableTypes = IngredientController.class)
class IngredientExceptionHandler {

    /** Filtre refusé par le service (jours négatifs, limite non positive). */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> filtreInvalide(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(e.getMessage()));
    }

    /** Paramètre qui n'a pas le bon type (catégorie inconnue, nombre non numérique). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parametreInvalide(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Valeur invalide pour le paramètre « " + e.getName() + " » : " + e.getValue()));
    }
}
