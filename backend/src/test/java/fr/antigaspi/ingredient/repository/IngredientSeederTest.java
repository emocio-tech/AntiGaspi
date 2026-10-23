package fr.antigaspi.ingredient.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;

class IngredientSeederTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final Resource INITIAL = new ByteArrayResource(
            """
                    [
                      { "id": 10, "name": "Yaourts", "category": "PRODUITS_LAITIERS", "quantity": 2, "unit": "PIECE", "expiresInDays": 3, "addedDaysAgo": 1 },
                      { "id": 20, "name": "Riz", "category": "FECULENTS", "quantity": 500, "unit": "G" }
                    ]"""
                    .getBytes(StandardCharsets.UTF_8));

    @Test
    void tableVide_estRemplieAvecLesDonneesInitiales_etDesIdentifiantsAttribues() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository();

        new IngredientSeeder(repository, INITIAL, CLOCK).run(new DefaultApplicationArguments());

        assertThat(repository.findAll()).containsExactly(
                new Ingredient(1L, "Yaourts", Category.PRODUITS_LAITIERS, new BigDecimal("2"), Unit.PIECE,
                        LocalDate.of(2026, 10, 23), NOW.minusSeconds(24 * 3600)),
                new Ingredient(2L, "Riz", Category.FECULENTS, new BigDecimal("500"), Unit.G, null, NOW));
    }

    @Test
    void tableDejaRemplie_n_estPasTouchee() {
        Ingredient existant = new Ingredient(7L, "Beurre", Category.PRODUITS_LAITIERS, new BigDecimal("250"), Unit.G,
                null, NOW);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(existant);

        new IngredientSeeder(repository, INITIAL, CLOCK).run(new DefaultApplicationArguments());

        assertThat(repository.findAll()).containsExactly(existant);
    }
}
