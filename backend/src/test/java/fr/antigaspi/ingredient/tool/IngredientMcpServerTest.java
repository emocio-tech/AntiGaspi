package fr.antigaspi.ingredient.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.repository.MutableIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientEditService;
import fr.antigaspi.ingredient.service.IngredientService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serveur MCP réel (profil {@code mcp}, Streamable HTTP, port aléatoire) interrogé en JSON-RPC, sans réseau externe ni
 * appel au LLM.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mcp")
@Import(IngredientMcpServerTest.InventaireDeTest.class)
class IngredientMcpServerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    /** Inventaire servi au test en cours, à la place de la base (la lecture et l'écriture passent par lui). */
    private static final InMemoryIngredientRepository REPOSITORY = new InMemoryIngredientRepository();

    @TestConfiguration
    static class InventaireDeTest {

        @Bean
        @Primary
        MutableIngredientRepository repositoryDeTest() {
            return REPOSITORY;
        }

        @Bean
        @Primary
        IngredientService ingredientServiceDeTest() {
            return new IngredientService(REPOSITORY, CLOCK);
        }

        @Bean
        @Primary
        IngredientEditService ingredientEditServiceDeTest() {
            return new IngredientEditService(REPOSITORY, CLOCK);
        }
    }

    /** Remplace tout l'inventaire (ordre d'entrée volontairement différent de l'ordre d'urgence). */
    private static void remplacerInventaire(Ingredient... ingredients) {
        REPOSITORY.deleteAllById(REPOSITORY.findAll().stream().map(Ingredient::id).toList());
        REPOSITORY.saveAll(List.of(ingredients));
    }

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String sessionId;

    @BeforeEach
    void inventaireParDefaut() {
        remplacerInventaire(
                new Ingredient(1L, "Yaourts", Category.PRODUITS_LAITIERS, new BigDecimal("4"), Unit.PIECE,
                        TODAY.plusDays(10), NOW),
                new Ingredient(2L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2),
                        NOW),
                new Ingredient(3L, "Lait", Category.PRODUITS_LAITIERS, new BigDecimal("1"), Unit.L, TODAY.minusDays(1),
                        NOW));
    }

    @Test
    void toolsList_neContientQueLesToolsDeLInventaire() throws Exception {
        initialiserSession();

        JsonNode result = rpc("tools/list", "{}").get("result");

        List<String> noms = new ArrayList<>();
        result.get("tools").forEach(tool -> noms.add(tool.get("name").asString()));
        assertThat(noms).containsExactlyInAnyOrder("listerIngredients", "ajouterIngredients", "supprimerIngredients",
                "modifierIngredients");
    }

    @Test
    void toolsCall_ajouterModifierSupprimer_sontVisiblesDansListerIngredients() throws Exception {
        initialiserSession();

        JsonNode ajout = appeler("ajouterIngredients", """
                {"aliments":[
                  {"nom":"Pommes","categorie":"FRUITS","quantite":6,"unite":"PIECE","datePeremption":"2026-10-21"},
                  {"nom":"Riz","categorie":"FECULENTS","quantite":500,"unite":"G"}]}""");
        assertThat(estErreur(ajout)).isFalse();
        assertThat(nomsListes()).containsExactly("Lait", "Pommes", "Courgettes", "Yaourts", "Riz");

        JsonNode modification = appeler("modifierIngredients",
                "{\"modifications\":[{\"id\":2,\"datePeremption\":\"2026-12-01\",\"quantite\":1}]}");
        assertThat(estErreur(modification)).isFalse();
        assertThat(nomsListes()).containsExactly("Lait", "Pommes", "Yaourts", "Courgettes", "Riz");

        JsonNode suppression = appeler("supprimerIngredients", "{\"ids\":[3,1]}");
        assertThat(estErreur(suppression)).isFalse();
        assertThat(nomsListes()).containsExactly("Pommes", "Courgettes", "Riz");
    }

    @Test
    void toolsCall_suppressionRefusee_renvoieLaRaisonSansRienSupprimer() throws Exception {
        initialiserSession();

        JsonNode result = appeler("supprimerIngredients", "{\"ids\":[1,99]}");

        assertThat(result.get("content").get(0).get("text").asString()).contains("99");
        assertThat(nomsListes()).containsExactly("Lait", "Courgettes", "Yaourts");
    }

    @Test
    void toolsCall_renvoieLesAlimentsDuPlusUrgentAuMoinsUrgent() throws Exception {
        initialiserSession();

        JsonNode result = appelerLister("{}");

        assertThat(estErreur(result)).isFalse();
        JsonNode aliments = MAPPER.readTree(result.get("content").get(0).get("text").asString());
        List<String> noms = new ArrayList<>();
        aliments.forEach(aliment -> noms.add(aliment.get("nom").asString()));
        assertThat(noms).containsExactly("Lait", "Courgettes", "Yaourts");
        assertThat(aliments.get(0).get("joursRestants").asInt()).isEqualTo(-1);
    }

    @Test
    void toolsCall_inventaireVide_renvoieUneListeVideSansErreur() throws Exception {
        remplacerInventaire();
        initialiserSession();

        JsonNode result = appelerLister("{}");

        assertThat(estErreur(result)).isFalse();
        assertThat(MAPPER.readTree(result.get("content").get(0).get("text").asString())).isEmpty();
    }

    @Test
    void toolsCall_categorieInvalide_renvoieUneErreurSousFormeDeMessage() throws Exception {
        initialiserSession();

        JsonNode result = appelerLister("{\"categorie\":\"INCONNUE\"}");

        assertThat(estErreur(result)).isTrue();
        assertThat(result.get("content").get(0).get("text").asString()).isNotBlank();
    }

    @Test
    void toolsCall_toolNonExpose_estRefuse() throws Exception {
        initialiserSession();

        JsonNode reponse = rpc("tools/call", "{\"name\":\"genererRecette\",\"arguments\":{}}");

        boolean refuse = reponse.has("error") || reponse.path("result").path("isError").asBoolean(false);
        assertThat(refuse).isTrue();
    }

    private static boolean estErreur(JsonNode result) {
        return result.path("isError").asBoolean(false);
    }

    private JsonNode appelerLister(String arguments) throws Exception {
        return appeler("listerIngredients", arguments);
    }

    private JsonNode appeler(String tool, String arguments) throws Exception {
        return rpc("tools/call", "{\"name\":\"" + tool + "\",\"arguments\":" + arguments + "}").get("result");
    }

    /** Noms de l'inventaire dans l'ordre renvoyé par listerIngredients (du plus urgent au moins urgent). */
    private List<String> nomsListes() throws Exception {
        JsonNode aliments = MAPPER.readTree(appelerLister("{}").get("content").get(0).get("text").asString());
        List<String> noms = new ArrayList<>();
        aliments.forEach(aliment -> noms.add(aliment.get("nom").asString()));
        return noms;
    }

    private void initialiserSession() throws Exception {
        String params = """
                {"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}""";
        HttpResponse<String> response = post(
                "{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\",\"params\":" + params + "}");
        assertThat(response.statusCode()).isEqualTo(200);
        sessionId = response.headers().firstValue("Mcp-Session-Id").orElseThrow();
        HttpResponse<String> notification = post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        assertThat(notification.statusCode()).isIn(200, 202);
    }

    private JsonNode rpc(String method, String params) throws Exception {
        HttpResponse<String> response = post(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}");
        assertThat(response.statusCode()).isEqualTo(200);
        return MAPPER.readTree(corpsJson(response.body()));
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) {
            request.header("Mcp-Session-Id", sessionId);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** La réponse est soit du JSON, soit un flux SSE dont la ligne {@code data:} porte le message JSON-RPC. */
    private static String corpsJson(String body) {
        return body.lines().filter(line -> line.startsWith("data:")).map(line -> line.substring(5).trim()).findFirst()
                .orElse(body);
    }
}
