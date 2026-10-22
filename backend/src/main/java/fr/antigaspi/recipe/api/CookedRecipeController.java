package fr.antigaspi.recipe.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.service.CookingService;

/** API REST « J'ai cuisiné », destinée au front. */
@RestController
@RequestMapping("/api/cooked-recipes")
public class CookedRecipeController {

    private final CookingService cookingService;

    public CookedRecipeController(CookingService cookingService) {
        this.cookingService = cookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CookedRecipeResponse cook(@RequestBody CookRequest request) {
        CookedRecipe cooked = cookingService.cook(request.recipe(), request.rating(), request.comment(),
                request.consumed());
        return new CookedRecipeResponse(cooked.id(), cooked.recipe().title(), cooked.cookedAt(), cooked.rating(),
                cooked.comment(), cooked.recipe());
    }
}
