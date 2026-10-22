package fr.antigaspi.ingredient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.ingredient.model.Category;
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

    /** Inventaire servi au test en cours (ordre d'entrée volontairement différent de l'ordre d'urgence). */
    private static volatile List<Ingredient> inventaire;

    @TestConfiguration
    static class InventaireDeTest {

        @Bean
        IngredientService ingredientService() {
            return new IngredientService(() -> inventaire, Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void inventaireParDefaut() {
        inventaire = List.of(
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
        inventaire = List.of(new Ingredient(4L, "Pommes", Category.FRUITS, new BigDecimal("6"), Unit.PIECE, null, NOW));

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
        inventaire = List.of();

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
}
