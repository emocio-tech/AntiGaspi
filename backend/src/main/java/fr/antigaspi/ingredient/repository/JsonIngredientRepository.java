package fr.antigaspi.ingredient.repository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import fr.antigaspi.common.JsonResources;

import tools.jackson.core.type.TypeReference;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;

/**
 * Inventaire statique chargé depuis un fichier JSON au démarrage. Les dates y sont exprimées en jours relatifs à
 * aujourd'hui ({@code expiresInDays}, {@code addedDaysAgo}) pour que les données de test ne se périment pas.
 */
@Repository
public class JsonIngredientRepository implements IngredientRepository {

    /**
     * Entrée du fichier JSON : {@code expiresInDays} absent = pas de date, {@code addedDaysAgo} absent = aujourd'hui.
     */
    private record Entry(Long id, String name, Category category, BigDecimal quantity, Unit unit, Integer expiresInDays,
            Integer addedDaysAgo) {
    }

    private final List<Ingredient> ingredients;

    public JsonIngredientRepository(@Value("classpath:data/ingredients.json") Resource resource, Clock clock) {
        this.ingredients = JsonResources.readList(resource, new TypeReference<List<Entry>>() {
        }).stream().map(entry -> toIngredient(entry, clock)).toList();
    }

    @Override
    public List<Ingredient> findAll() {
        return ingredients;
    }

    private static Ingredient toIngredient(Entry entry, Clock clock) {
        LocalDate expiryDate = entry.expiresInDays() == null ? null
                : LocalDate.now(clock).plusDays(entry.expiresInDays());
        int addedDaysAgo = entry.addedDaysAgo() == null ? 0 : entry.addedDaysAgo();
        Instant addedAt = clock.instant().minus(Duration.ofDays(addedDaysAgo));
        return new Ingredient(entry.id(), entry.name(), entry.category(), entry.quantity(), entry.unit(), expiryDate,
                addedAt);
    }
}
