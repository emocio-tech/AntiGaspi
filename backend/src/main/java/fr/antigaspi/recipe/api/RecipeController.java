package fr.antigaspi.recipe.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeService;

/**
 * API REST de génération de recette, destinée au front. Chaque appel génère une recette (un appel au LLM).
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * @param request
     *            absent : cuisine les aliments les plus urgents
     */
    @PostMapping
    public RecipeResponse generate(@RequestBody(required = false) RecipeRequest request) {
        GeneratedRecipe generated = recipeService.generate(request);
        List<IssueResponse> issues = generated.issues().stream()
                .map(issue -> new IssueResponse(issue.type(), issue.type().label(), issue.message())).toList();
        return new RecipeResponse(generated.recipe(), issues);
    }
}
