package fr.antigaspi.ingredient.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.ingredient.tool.IngredientTools.Aliment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class IngredientToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final IngredientTools tools = new IngredientTools(new IngredientService(
            new InMemoryIngredientRepository(
                    new Ingredient(3L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG,
                            TODAY.plusDays(2), NOW),
                    new Ingredient(5L, "Pommes", Category.FRUITS, new BigDecimal("4"), Unit.PIECE, null, NOW)),
            Clock.fixed(NOW, ZoneOffset.UTC)));

    private static ToolCallback callback(IngredientTools tools) {
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        assertThat(callbacks).hasSize(1);
        return callbacks[0];
    }

    @Test
    void listerIngredients_convertitLesAlimentsPourLeLlm() {
        List<Aliment> aliments = tools.listerIngredients(null, null, null);

        assertThat(aliments).containsExactly(
                new Aliment(3L, "Courgettes", "Légumes", new BigDecimal("2.5"), "kg", TODAY.plusDays(2), 2, false,
                        "🟠"),
                new Aliment(5L, "Pommes", "Fruits", new BigDecimal("4"), "pièce",
                        TODAY.plusDays(Category.FRUITS.minShelfDays()), Category.FRUITS.minShelfDays(), true, "🟠"));
    }

    @Test
    void contratExposeAuLlm_nomEtParametresFacultatifs() {
        ToolCallback callback = callback(tools);

        assertThat(callback.getToolDefinition().name()).isEqualTo("listerIngredients");
        assertThat(callback.getToolDefinition().description()).isNotBlank();
        JsonNode schema = JsonMapper.builder().build().readTree(callback.getToolDefinition().inputSchema());
        assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrder("categorie", "joursMax",
                "limite");
        JsonNode required = schema.get("required");
        assertThat(required == null || required.isEmpty()).as("aucun paramètre obligatoire").isTrue();
        assertThat(schema.get("properties").get("categorie").get("enum").toString()).contains("LEGUMES",
                "VIANDES_POISSONS");
    }

    @Test
    void appelParLeLlmEnJson_appliqueLesFiltres() {
        String result = callback(tools).call("{\"categorie\":\"LEGUMES\"}");

        assertThat(result).contains("Courgettes").doesNotContain("Pommes");
    }
}
