package fr.antigaspi.ingredient.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;

class JsonIngredientRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static JsonIngredientRepository repository(String json) {
        return new JsonIngredientRepository(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), CLOCK);
    }

    @Test
    void convertitLesJoursRelatifsEnDates() {
        JsonIngredientRepository repository = repository("""
                [{"id": 1, "name": "Courgette", "category": "LEGUMES", "quantity": 2.5, "unit": "KG",
                  "expiresInDays": 2, "addedDaysAgo": 3}]
                """);

        assertThat(repository.findAll()).containsExactly(new Ingredient(1L, "Courgette", Category.LEGUMES,
                new BigDecimal("2.5"), Unit.KG, LocalDate.of(2026, 10, 22), NOW.minus(Duration.ofDays(3))));
    }

    @Test
    void champsDeDateAbsents_pasDeDateDePeremptionEtAjoutAujourdhui() {
        JsonIngredientRepository repository = repository("""
                [{"id": 7, "name": "Pommes", "category": "FRUITS", "quantity": 6, "unit": "PIECE"}]
                """);

        Ingredient pommes = repository.findAll().getFirst();
        assertThat(pommes.expiryDate()).isNull();
        assertThat(pommes.addedAt()).isEqualTo(NOW);
    }

    @Test
    void expiresInDaysNegatif_alimentPerime() {
        JsonIngredientRepository repository = repository("""
                [{"id": 1, "name": "Yaourt", "category": "PRODUITS_LAITIERS", "quantity": 1, "unit": "PIECE",
                  "expiresInDays": -1}]
                """);

        assertThat(repository.findAll().getFirst().expiryDate()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void fichierInvalide_echoueAuChargement() {
        assertThatThrownBy(() -> repository("pas du json")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> repository("""
                [{"id": 1, "name": "X", "category": "INCONNUE", "quantity": 1, "unit": "PIECE"}]
                """)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fichierDeDonneesDuProjet_seChargeAvecDesIdsUniques() {
        List<Ingredient> ingredients = new JsonIngredientRepository(new ClassPathResource("data/ingredients.json"),
                CLOCK).findAll();

        assertThat(ingredients).hasSizeGreaterThanOrEqualTo(10);
        assertThat(ingredients).extracting(Ingredient::id).doesNotHaveDuplicates();
        assertThat(ingredients).anyMatch(i -> i.expiryDate() == null);
        assertThat(ingredients)
                .anyMatch(i -> i.expiryDate() != null && i.expiryDate().isBefore(LocalDate.of(2026, 10, 21)));
    }
}
