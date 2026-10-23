package fr.antigaspi.ingredient.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientDraft;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(@RequestBody IngredientRequest request) {
        return toResponse(ingredientService.add(toDraft(request)));
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable long id, @RequestBody IngredientRequest request) {
        return toResponse(ingredientService.update(id, toDraft(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        ingredientService.remove(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(@RequestParam List<Long> ids) {
        ingredientService.removeAll(ids);
    }

    private static IngredientDraft toDraft(IngredientRequest request) {
        return new IngredientDraft(request.name(), request.category(), request.quantity(), request.unit(),
                request.expiryDate());
    }

    private static IngredientResponse toResponse(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        return new IngredientResponse(ingredient.id(), ingredient.name(), ingredient.category(),
                ingredient.category().label(), ingredient.quantity(), ingredient.unit().symbol(),
                view.effectiveExpiryDate(), view.daysLeft(), view.estimated(), view.urgency());
    }
}
