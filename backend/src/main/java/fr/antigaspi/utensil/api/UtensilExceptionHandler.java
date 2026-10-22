package fr.antigaspi.utensil.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.antigaspi.common.api.ApiError;
import fr.antigaspi.utensil.service.UtensilNotFoundException;

/**
 * Traduit en 404 un ustensile introuvable ; plus précis que le 400 générique des valeurs refusées, il doit donc passer
 * avant le gestionnaire commun, qui accepterait aussi cette exception (sous-classe de {@code IllegalArgumentException}).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = UtensilController.class)
class UtensilExceptionHandler {

    @ExceptionHandler(UtensilNotFoundException.class)
    ResponseEntity<ApiError> introuvable(UtensilNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
    }
}
