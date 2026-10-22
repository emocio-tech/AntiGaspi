package fr.antigaspi.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduit en 400, avec un message lisible par le front, les requêtes invalides de toutes les API REST.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    /** Valeur refusée par un service (filtre négatif, nom vide, doublon…). */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> valeurRefusee(IllegalArgumentException e) {
        return badRequest(e.getMessage());
    }

    /** Paramètre qui n'a pas le bon type (catégorie inconnue, nombre non numérique). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parametreInvalide(MethodArgumentTypeMismatchException e) {
        return badRequest("Valeur invalide pour le paramètre « " + e.getName() + " » : " + e.getValue());
    }

    /** Corps de requête absent ou qui n'est pas du JSON attendu. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> corpsIllisible(HttpMessageNotReadableException e) {
        return badRequest("Le corps de la requête est absent ou invalide");
    }

    private static ResponseEntity<ApiError> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }
}
