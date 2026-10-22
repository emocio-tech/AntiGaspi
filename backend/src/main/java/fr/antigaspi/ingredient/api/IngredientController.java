package fr.antigaspi.ingredient.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.service.IngredientService;

/**
 * API REST de l'inventaire, destinée au front.
 */
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredientResponse> list(@RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer withinDays, @RequestParam(required = false) Integer limit) {
        return ingredientService.search(category, withinDays, limit).stream().map(IngredientController::toResponse)
                .toList();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return Arrays.stream(Category.values()).map(category -> new CategoryResponse(category, category.label()))
                .toList();
    }

    private static IngredientResponse toResponse(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        return new IngredientResponse(ingredient.id(), ingredient.name(), ingredient.category(),
                ingredient.category().label(), ingredient.quantity(), ingredient.unit().symbol(),
                view.effectiveExpiryDate(), view.daysLeft(), view.estimated(), view.urgency());
    }
}
