package fr.antigaspi.ingredient.repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Inventaire modifiable en mémoire pour les tests : remplace la base, avec la même attribution d'identifiants.
 */
public class InMemoryIngredientRepository implements MutableIngredientRepository {

    private final Map<Long, Ingredient> byId = new LinkedHashMap<>();
    private long lastId;

    public InMemoryIngredientRepository(Ingredient... initial) {
        saveAll(List.of(initial));
    }

    @Override
    public synchronized List<Ingredient> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public synchronized List<Ingredient> findAllById(Collection<Long> ids) {
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public synchronized List<Ingredient> saveAll(List<Ingredient> ingredients) {
        return ingredients.stream().map(ingredient -> {
            long id = ingredient.id() == null ? ++lastId : ingredient.id();
            lastId = Math.max(lastId, id);
            Ingredient saved = new Ingredient(id, ingredient.name(), ingredient.category(), ingredient.quantity(),
                    ingredient.unit(), ingredient.expiryDate(), ingredient.addedAt());
            byId.put(id, saved);
            return saved;
        }).toList();
    }

    @Override
    public synchronized void deleteAllById(Collection<Long> ids) {
        ids.forEach(byId::remove);
    }
}
