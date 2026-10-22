package fr.antigaspi.recipe.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.antigaspi.common.api.ApiError;
import fr.antigaspi.recipe.service.CookedRecipeNotFoundException;

/**
 * Traduit en 404 une recette cuisinée introuvable ; plus précis que le 400 générique des valeurs refusées, il doit donc
 * passer avant le gestionnaire commun.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CookedRecipeController.class)
class CookedRecipeExceptionHandler {

    @ExceptionHandler(CookedRecipeNotFoundException.class)
    ResponseEntity<ApiError> introuvable(CookedRecipeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
    }
}
