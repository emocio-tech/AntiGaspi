package fr.antigaspi.ingredient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;

/**
 * API REST de l'inventaire avec le vrai {@link IngredientService}, une horloge figée et un inventaire en mémoire : ni
 * LLM ni réseau.
 */
@WebMvcTest(IngredientController.class)
@Import(IngredientControllerTest.InventaireDeTest.class)
class IngredientControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    /** Inventaire en mémoire servi au test en cours (ordre d'entrée volontairement différent de l'ordre d'urgence). */
    private static final InMemoryIngredientRepository inventaire = new InMemoryIngredientRepository();

    @TestConfiguration
    static class InventaireDeTest {

        @Bean
        IngredientService ingredientService() {
            return new IngredientService(inventaire, Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void inventaireParDefaut() {
        inventaire.replaceAll(
                new Ingredient(1L, "Yaourts", Category.PRODUITS_LAITIERS, new BigDecimal("4"), Unit.PIECE,
                        TODAY.plusDays(10), NOW),
                new Ingredient(2L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2),
                        NOW),
                new Ingredient(3L, "Lait", Category.PRODUITS_LAITIERS, new BigDecimal("1"), Unit.L, TODAY.minusDays(1),
                        NOW));
    }

    @Test
    void list_sansParametre_renvoieTousLesAlimentsDuPlusUrgentAuMoinsUrgent() throws Exception {
        mvc.perform(get("/api/ingredients")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Lait")).andExpect(jsonPath("$[1].name").value("Courgettes"))
                .andExpect(jsonPath("$[2].name").value("Yaourts"));
    }

    @Test
    void list_alimentPerime_exposeLesChampsPourLeFront() throws Exception {
        mvc.perform(get("/api/ingredients")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].daysLeft").value(-1))
                .andExpect(jsonPath("$[0].urgency").value("EXPIRED_OR_TODAY"))
                .andExpect(jsonPath("$[0].category").value("PRODUITS_LAITIERS"))
                .andExpect(jsonPath("$[0].categoryLabel").value("Produits laitiers"))
                .andExpect(jsonPath("$[0].unit").value("l")).andExpect(jsonPath("$[0].estimated").value(false))
                .andExpect(jsonPath("$[1].quantity").value(2.5)).andExpect(jsonPath("$[1].unit").value("kg"))
                .andExpect(jsonPath("$[1].expiryDate").value("2026-10-22"))
                .andExpect(jsonPath("$[1].urgency").value("SOON"));
    }

    @Test
    void list_sansDatePeremption_renvoieUneDateEstimee() throws Exception {
        inventaire
                .replaceAll(new Ingredient(4L, "Pommes", Category.FRUITS, new BigDecimal("6"), Unit.PIECE, null, NOW));

        mvc.perform(get("/api/ingredients")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimated").value(true))
                .andExpect(jsonPath("$[0].expiryDate").value("2026-10-23"))
                .andExpect(jsonPath("$[0].daysLeft").value(3));
    }

    @Test
    void list_filtreParCategorie() throws Exception {
        mvc.perform(get("/api/ingredients").param("category", "LEGUMES")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("Courgettes"));
    }

    @Test
    void list_filtreParJoursRestants_garde_lesPerimesEtLesUrgents() throws Exception {
        mvc.perform(get("/api/ingredients").param("withinDays", "2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].name").value("Lait"))
                .andExpect(jsonPath("$[1].name").value("Courgettes"));
    }

    @Test
    void list_limite_garde_lesPlusUrgents() throws Exception {
        mvc.perform(get("/api/ingredients").param("limit", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("Lait"));
    }

    @Test
    void list_filtresCombines_limiteAppliqueeApresLesAutresFiltres() throws Exception {
        mvc.perform(get("/api/ingredients").param("category", "LEGUMES").param("withinDays", "5").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Courgettes"));
    }

    @Test
    void list_inventaireVide_renvoieUneListeVide() throws Exception {
        inventaire.replaceAll();

        mvc.perform(get("/api/ingredients")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void list_categorieInconnue_renvoie400AvecUnMessage() throws Exception {
        mvc.perform(get("/api/ingredients").param("category", "INCONNUE")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void list_joursNegatifs_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(get("/api/ingredients").param("withinDays", "-1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le nombre de jours ne peut pas être négatif : -1"));
    }

    @Test
    void list_limiteNulle_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(get("/api/ingredients").param("limit", "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La limite doit être positive : 0"));
    }

    @Test
    void list_joursNonNumeriques_renvoie400AvecUnMessage() throws Exception {
        mvc.perform(get("/api/ingredients").param("withinDays", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void categories_renvoieToutesLesCategoriesAvecCodeEtLibelle() throws Exception {
        mvc.perform(get("/api/ingredients/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Category.values().length))
                .andExpect(jsonPath("$[0].code").value("FRUITS")).andExpect(jsonPath("$[0].label").value("Fruits"))
                .andExpect(jsonPath("$[3].code").value("VIANDES_POISSONS"))
                .andExpect(jsonPath("$[3].label").value("Viandes & poissons"));
    }

    // --- Gestion de l'inventaire ---

    private static final String LAIT_CORPS = """
            {"name":"Lait","category":"PRODUITS_LAITIERS","quantity":2,"unit":"L","expiryDate":"2026-10-25"}""";

    private static String corps(String name, String category, String quantity, String unit, String expiryDate) {
        return "{\"name\":" + name + ",\"category\":" + category + ",\"quantity\":" + quantity + ",\"unit\":" + unit
                + ",\"expiryDate\":" + expiryDate + "}";
    }

    @Test
    void create_renvoie201AvecLAlimentCree_puisIlApparaitDansLaListe() throws Exception {
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON).content(LAIT_CORPS))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Lait")).andExpect(jsonPath("$.unit").value("l"))
                .andExpect(jsonPath("$.expiryDate").value("2026-10-25")).andExpect(jsonPath("$.estimated").value(false))
                .andExpect(jsonPath("$.urgency").value("WEEK"));

        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.id==4)].name").value("Lait"));
    }

    @Test
    void create_sansDate_renvoieUneDateEstimee() throws Exception {
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content(corps("\"Pommes\"", "\"FRUITS\"", "6", "\"PIECE\"", "null"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimated").value(true)).andExpect(jsonPath("$.expiryDate").value("2026-10-23"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "''|PRODUITS_LAITIERS|2|L|Le nom de l'aliment est vide",
            "Lait|PRODUITS_LAITIERS|0|L|La quantité doit être positive : 0",
            "Lait|PRODUITS_LAITIERS|-3|L|La quantité doit être positive : -3",
            "Lait|PRODUITS_LAITIERS||L|La quantité doit être positive : aucune",
            "Lait||2|L|La catégorie est obligatoire", "Lait|PRODUITS_LAITIERS|2||L'unité est obligatoire" })
    void create_valeurRefusee_renvoie400AvecLeMessageDuService(String name, String category, String quantity,
            String unit, String message) throws Exception {
        String body = corps("\"" + name + "\"", category == null ? "null" : "\"" + category + "\"",
                quantity == null ? "null" : quantity, unit == null ? "null" : "\"" + unit + "\"", "null");

        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void create_categorieOuUniteInconnue_ouDateIllisible_renvoie400() throws Exception {
        for (String body : new String[] { corps("\"Lait\"", "\"INCONNUE\"", "2", "\"L\"", "null"),
                corps("\"Lait\"", "\"PRODUITS_LAITIERS\"", "2", "\"LITRES\"", "null"),
                corps("\"Lait\"", "\"PRODUITS_LAITIERS\"", "2", "\"L\"", "\"demain\"") }) {
            mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    @Test
    void update_renvoie200AvecLAlimentModifie() throws Exception {
        mvc.perform(put("/api/ingredients/2").contentType(MediaType.APPLICATION_JSON).content(LAIT_CORPS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Lait"))
                .andExpect(jsonPath("$.category").value("PRODUITS_LAITIERS")).andExpect(jsonPath("$.unit").value("l"));

        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.id==2)].name").value("Lait"));
    }

    @Test
    void update_idInconnu_renvoie404AvecUnMessage() throws Exception {
        mvc.perform(put("/api/ingredients/99").contentType(MediaType.APPLICATION_JSON).content(LAIT_CORPS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun aliment avec l'identifiant 99"));
    }

    @Test
    void delete_renvoie204_puis404QuandLAlimentEstDejaRetire() throws Exception {
        mvc.perform(delete("/api/ingredients/1")).andExpect(status().isNoContent());
        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/api/ingredients/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun aliment avec l'identifiant 1"));
    }

    @Test
    void deleteGroupe_renvoie204_etRetireLesAlimentsDonnes() throws Exception {
        mvc.perform(delete("/api/ingredients").param("ids", "1,3")).andExpect(status().isNoContent());

        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Courgettes"));
    }

    @Test
    void deleteGroupe_avecUnInconnu_renvoie404_etRienNEstRetire() throws Exception {
        mvc.perform(delete("/api/ingredients").param("ids", "1,99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun aliment avec l'identifiant 99"));

        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$.length()").value(3));
    }
}
