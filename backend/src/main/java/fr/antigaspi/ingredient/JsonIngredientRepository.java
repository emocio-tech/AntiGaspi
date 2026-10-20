package fr.antigaspi.ingredient;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

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
        this.ingredients = load(resource).stream().map(entry -> toIngredient(entry, clock)).toList();
    }

    @Override
    public List<Ingredient> findAll() {
        return ingredients;
    }

    private static List<Entry> load(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return JsonMapper.shared().readValue(in, new TypeReference<List<Entry>>() {
            });
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Impossible de charger l'inventaire depuis " + resource.getDescription(),
                    e);
        }
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
