package fr.antigaspi.ingredient.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class IngredientEditToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
            new Ingredient(3L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2), NOW),
            new Ingredient(5L, "Pommes", Category.FRUITS, new BigDecimal("4"), Unit.PIECE, null, NOW));

    private final IngredientEditTools tools = new IngredientEditTools(
            new IngredientService(repository, Clock.fixed(NOW, ZoneOffset.UTC)));

    private ToolCallback callback(String name) {
        return Arrays.stream(ToolCallbacks.from(tools)).filter(c -> c.getToolDefinition().name().equals(name))
                .findFirst().orElseThrow();
    }

    @Test
    void contratExposeAuLlm_troisToolsEtParametresObligatoires() {
        assertThat(ToolCallbacks.from(tools)).extracting(c -> c.getToolDefinition().name())
                .containsExactlyInAnyOrder("ajouterAliment", "modifierQuantite", "retirerAliments");

        JsonNode schema = JsonMapper.shared().readTree(callback("ajouterAliment").getToolDefinition().inputSchema());
        assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrder("nom", "categorie", "quantite",
                "unite", "datePeremption");
        assertThat(schema.get("required").valueStream().map(JsonNode::asString)).containsExactlyInAnyOrder("nom",
                "categorie", "quantite", "unite");
    }

    @Test
    void appelParLeLlmEnJson_ajouteLAlimentAvecAccentsEtDateIso() {
        String result = callback("ajouterAliment").call(
                "{\"nom\":\"Cr\\u00e8me\",\"categorie\":\"PRODUITS_LAITIERS\",\"quantite\":20,\"unite\":\"ML\",\"datePeremption\":\"2026-10-25\"}");

        assertThat(result).contains("Crème", "6");
        assertThat(repository.findAll()).filteredOn(i -> i.id() == 6L).singleElement().satisfies(ingredient -> {
            assertThat(ingredient.name()).isEqualTo("Crème");
            assertThat(ingredient.unit()).isEqualTo(Unit.ML);
            assertThat(ingredient.quantity()).isEqualByComparingTo("20");
            assertThat(ingredient.expiryDate()).isEqualTo(LocalDate.of(2026, 10, 25));
        });
    }

    @Test
    void ajouterAliment_valeurRefusee_renvoieLeMessageDuService_etLInventaireResteInchange() {
        String result = tools.ajouterAliment("Lait", Category.PRODUITS_LAITIERS, BigDecimal.ZERO, Unit.L, null);

        assertThat(result).contains("La quantité doit être positive : 0");
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void modifierQuantite_confirmeAvecLeNomEtLIdentifiant() {
        String result = tools.modifierQuantite(3L, new BigDecimal("1"));

        assertThat(result).contains("Courgettes", "3");
        assertThat(repository.findById(3L)).get().extracting(Ingredient::quantity).isEqualTo(new BigDecimal("1"));
    }

    @Test
    void modifierQuantite_idInconnu_estRenvoyeAuLlmSousFormeDeMessage() {
        assertThat(tools.modifierQuantite(99L, BigDecimal.ONE)).contains("Aucun aliment avec l'identifiant 99");
        assertThat(tools.modifierQuantite(3L, BigDecimal.ZERO)).contains("La quantité doit être positive : 0");
    }

    @Test
    void parametresAbsentsEnvoyesParLeLlm_messageSansExceptionEtInventaireInchange() {
        assertThat(tools.retirerAliments(null)).contains("Aucun aliment à retirer");
        List<Long> avecNul = new ArrayList<>();
        avecNul.add(null);
        assertThat(tools.retirerAliments(avecNul)).contains("Aucun aliment à retirer");
        assertThat(tools.modifierQuantite(null, BigDecimal.ONE)).contains("identifiant");
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void retirerAliments_confirmeAvecLesNomsRetires() {
        String result = tools.retirerAliments(List.of(3L, 5L));

        assertThat(result).contains("Courgettes", "Pommes");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void retirerAliments_unIdentifiantInconnu_renvoieLeMessage_etRienNEstRetire() {
        assertThat(tools.retirerAliments(List.of(3L, 99L))).contains("Aucun aliment avec l'identifiant 99");
        assertThat(repository.findAll()).hasSize(2);
    }
}
