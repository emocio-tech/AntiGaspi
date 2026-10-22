package fr.antigaspi.ingredient.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Faux dépôt en mémoire pour les tests du service, des tools et de l'API.
 */
public class InMemoryIngredientRepository implements IngredientRepository {

    private final List<Ingredient> ingredients;

    public InMemoryIngredientRepository(Ingredient... initial) {
        this.ingredients = new ArrayList<>(List.of(initial));
    }

    /** Remplace tout le contenu, pour les tests qui partagent un dépôt entre plusieurs cas. */
    public synchronized void replaceAll(Ingredient... initial) {
        ingredients.clear();
        ingredients.addAll(List.of(initial));
    }

    @Override
    public synchronized List<Ingredient> findAll() {
        return List.copyOf(ingredients);
    }

    @Override
    public synchronized Optional<Ingredient> findById(long id) {
        return ingredients.stream().filter(ingredient -> ingredient.id() != null && ingredient.id() == id).findFirst();
    }

    @Override
    public synchronized Ingredient save(Ingredient ingredient) {
        if (ingredient.id() == null) {
            long id = ingredients.stream().mapToLong(Ingredient::id).max().orElse(0L) + 1;
            Ingredient inserted = new Ingredient(id, ingredient.name(), ingredient.category(), ingredient.quantity(),
                    ingredient.unit(), ingredient.expiryDate(), ingredient.addedAt());
            ingredients.add(inserted);
            return inserted;
        }
        ingredients.replaceAll(existing -> existing.id().equals(ingredient.id()) ? ingredient : existing);
        return ingredient;
    }

    @Override
    public synchronized void deleteAllById(Collection<Long> ids) {
        ingredients.removeIf(ingredient -> ids.contains(ingredient.id()));
    }
}
