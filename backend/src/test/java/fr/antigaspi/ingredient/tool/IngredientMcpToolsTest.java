package fr.antigaspi.ingredient.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import fr.antigaspi.common.ChatTools;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class IngredientMcpToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static final Ingredient YAOURTS = new Ingredient(1L, "Yaourts", Category.PRODUITS_LAITIERS,
            new BigDecimal("4"), Unit.PIECE, TODAY.plusDays(10), NOW);
    private static final Ingredient COURGETTES = new Ingredient(2L, "Courgettes", Category.LEGUMES,
            new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2), NOW);

    private final InMemoryIngredientRepository repository = new InMemoryIngredientRepository(YAOURTS, COURGETTES);
    private final IngredientMcpTools tools = new IngredientMcpTools(
            new IngredientService(repository, Clock.fixed(NOW, ZoneOffset.UTC)));

    private ToolCallback callback(String name) {
        return Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals(name)).findFirst().orElseThrow();
    }

    private JsonNode schema(String name) {
        return JsonMapper.shared().readTree(callback(name).getToolDefinition().inputSchema());
    }

    private static List<String> noms(JsonNode array) {
        List<String> noms = new ArrayList<>();
        if (array != null) {
            array.forEach(node -> noms.add(node.asString()));
        }
        return noms;
    }

    @Test
    void contrat_exposeLesTroisToolsAuLlm() {
        List<String> noms = Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name()).toList();

        assertThat(noms).containsExactlyInAnyOrder("ajouterIngredients", "supprimerIngredients", "modifierIngredients");
        for (String nom : noms) {
            assertThat(callback(nom).getToolDefinition().description()).isNotBlank();
        }
    }

    @Test
    void contrat_ajouterIngredients_datePeremptionFacultative() {
        JsonNode schema = schema("ajouterIngredients");

        assertThat(noms(schema.get("required"))).containsExactly("aliments");
        JsonNode aliment = schema.get("properties").get("aliments").get("items");
        assertThat(aliment.get("properties").propertyNames()).containsExactlyInAnyOrder("nom", "categorie", "quantite",
                "unite", "datePeremption");
        assertThat(noms(aliment.get("required"))).containsExactlyInAnyOrder("nom", "categorie", "quantite", "unite");
        assertThat(aliment.get("properties").get("categorie").get("enum").toString()).contains("LEGUMES",
                "VIANDES_POISSONS");
        assertThat(aliment.get("properties").get("unite").get("enum").toString()).contains("KG", "PIECE");
    }

    @Test
    void contrat_supprimerIngredients_listeDIdentifiantsObligatoire() {
        JsonNode schema = schema("supprimerIngredients");

        assertThat(noms(schema.get("required"))).containsExactly("ids");
        assertThat(schema.get("properties").get("ids").get("type").asString()).isEqualTo("array");
    }

    @Test
    void contrat_modifierIngredients_seulIdentifiantObligatoire() {
        JsonNode schema = schema("modifierIngredients");

        assertThat(noms(schema.get("required"))).containsExactly("modifications");
        JsonNode modification = schema.get("properties").get("modifications").get("items");
        assertThat(modification.get("properties").propertyNames()).containsExactlyInAnyOrder("id", "nom", "categorie",
                "quantite", "unite", "datePeremption", "effacerDatePeremption");
        assertThat(noms(modification.get("required"))).containsExactly("id");
    }

    @Test
    void ajouterIngredients_appelEnJson_ajouteEtConfirmeAvecLesIdentifiants() {
        String result = callback("ajouterIngredients").call("""
                {"aliments":[
                  {"nom":"Pommes","categorie":"FRUITS","quantite":6,"unite":"PIECE","datePeremption":"2026-10-30"},
                  {"nom":"Cr\\u00e8me","categorie":"PRODUITS_LAITIERS","quantite":20,"unite":"ML"}]}""");

        assertThat(result).contains("Pommes", "Crème", "3", "4");
        assertThat(repository.findAll()).extracting(Ingredient::name).containsExactly("Yaourts", "Courgettes", "Pommes",
                "Crème");
        assertThat(repository.findAll().get(2).expiryDate()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(repository.findAll().get(3).expiryDate()).isNull();
    }

    @Test
    void ajouterIngredients_alimentRefuse_renvoieLaRaisonSansRienAjouter() {
        String result = callback("ajouterIngredients").call("""
                {"aliments":[
                  {"nom":"Pommes","categorie":"FRUITS","quantite":6,"unite":"PIECE"},
                  {"nom":"Poires","categorie":"FRUITS","quantite":0,"unite":"PIECE"}]}""");

        assertThat(result).contains("Poires");
        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES);
    }

    @Test
    void supprimerIngredients_appelEnJson_supprimeEtConfirme() {
        String result = callback("supprimerIngredients").call("{\"ids\":[1,2]}");

        assertThat(result).contains("Yaourts", "Courgettes");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void supprimerIngredients_identifiantInconnu_renvoieLaRaisonSansRienSupprimer() {
        String result = callback("supprimerIngredients").call("{\"ids\":[1,99]}");

        assertThat(result).contains("99");
        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES);
    }

    @Test
    void supprimerIngredients_listeVide_estRefusee() {
        String result = callback("supprimerIngredients").call("{\"ids\":[]}");

        assertThat(result).isNotBlank();
        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES);
    }

    @Test
    void modifierIngredients_appelEnJson_neChangeQueLesChampsRenseignes() {
        String result = callback("modifierIngredients")
                .call("{\"modifications\":[{\"id\":2,\"quantite\":1,\"datePeremption\":\"2026-10-25\"}]}");

        assertThat(result).contains("Courgettes");
        assertThat(repository.findAll()).containsExactly(YAOURTS, new Ingredient(2L, "Courgettes", Category.LEGUMES,
                BigDecimal.ONE, Unit.KG, LocalDate.of(2026, 10, 25), NOW));
    }

    @Test
    void modifierIngredients_effacerDatePeremption_retireLaDate() {
        callback("modifierIngredients").call("{\"modifications\":[{\"id\":1,\"effacerDatePeremption\":true}]}");

        assertThat(repository.findAll().get(0).expiryDate()).isNull();
    }

    @Test
    void modifierIngredients_identifiantInconnu_renvoieLaRaisonSansRienModifier() {
        String result = callback("modifierIngredients")
                .call("{\"modifications\":[{\"id\":1,\"nom\":\"Skyr\"},{\"id\":99,\"nom\":\"X\"}]}");

        assertThat(result).contains("99");
        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES);
    }

    @Test
    void lesToolsDEcriture_nImplemententPasChatTools_donc_absentsDuChat() {
        assertThat(tools).isNotInstanceOf(ChatTools.class);
    }
}
