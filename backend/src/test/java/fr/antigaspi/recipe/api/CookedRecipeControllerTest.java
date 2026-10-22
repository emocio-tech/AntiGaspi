package fr.antigaspi.recipe.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.recipe.model.Consumption;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.service.CookedRecipeNotFoundException;
import fr.antigaspi.recipe.service.CookingService;

/**
 * API REST « J'ai cuisiné » : {@link CookingService} est simulé.
 */
@WebMvcTest(CookedRecipeController.class)
class CookedRecipeControllerTest {

    private static final Recipe RECIPE = new Recipe("Gratin de courgettes", 2, 35, List.of("Four"),
            List.of(new RecipeIngredient(3L, "Courgettes", new BigDecimal("2.5"), "kg")),
            List.of("Couper les courgettes.", "Enfourner 25 minutes."));

    private static final String BODY = """
            {"recipe":{"title":"Gratin de courgettes","servings":2,"preparationMinutes":35,"utensils":["Four"],
              "ingredients":[{"ingredientId":3,"name":"Courgettes","quantity":2.5,"unit":"kg"}],
              "steps":["Couper les courgettes.","Enfourner 25 minutes."]},
             "rating":"DOWN","comment":"Trop salé","consumed":[{"ingredientId":3,"quantity":2.5}]}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CookingService cookingService;

    @Test
    void cook_renvoie201AvecLaRecetteCuisinee() throws Exception {
        when(cookingService.cook(any(), any(), any(), any())).thenReturn(
                new CookedRecipe(7L, RECIPE, Instant.parse("2026-10-20T10:00:00Z"), Rating.DOWN, "Trop salé"));

        mvc.perform(post("/api/cooked-recipes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Gratin de courgettes"))
                .andExpect(jsonPath("$.cookedAt").value("2026-10-20T10:00:00Z"))
                .andExpect(jsonPath("$.rating").value("DOWN")).andExpect(jsonPath("$.comment").value("Trop salé"))
                .andExpect(jsonPath("$.recipe.title").value("Gratin de courgettes"));

        ArgumentCaptor<Recipe> recipe = ArgumentCaptor.forClass(Recipe.class);
        ArgumentCaptor<List<Consumption>> consumed = ArgumentCaptor.captor();
        verify(cookingService).cook(recipe.capture(), org.mockito.ArgumentMatchers.eq(Rating.DOWN),
                org.mockito.ArgumentMatchers.eq("Trop salé"), consumed.capture());
        assertThat(recipe.getValue()).isEqualTo(RECIPE);
        assertThat(consumed.getValue()).containsExactly(new Consumption(3L, new BigDecimal("2.5")));
    }

    @Test
    void cook_avisInconnu_renvoie400() throws Exception {
        mvc.perform(post("/api/cooked-recipes").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("DOWN", "BOF"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void cook_valeurRefuseeParLeService_renvoie400AvecLeMessage() throws Exception {
        when(cookingService.cook(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("La quantité utilisée doit être positive ou nulle : -1"));

        mvc.perform(post("/api/cooked-recipes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La quantité utilisée doit être positive ou nulle : -1"));
    }

    @Test
    void cook_recetteAbsente_renvoie400() throws Exception {
        when(cookingService.cook(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("La recette est vide"));

        mvc.perform(post("/api/cooked-recipes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"ok\",\"consumed\":[]}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La recette est vide"));
    }

    @Test
    void list_renvoieLHistoriqueAvecSesChamps() throws Exception {
        when(cookingService.history()).thenReturn(
                List.of(new CookedRecipe(7L, RECIPE, Instant.parse("2026-10-20T10:00:00Z"), Rating.DOWN, "Trop salé"),
                        new CookedRecipe(5L, RECIPE, Instant.parse("2026-10-18T10:00:00Z"), null, null)));

        mvc.perform(get("/api/cooked-recipes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].title").value("Gratin de courgettes"))
                .andExpect(jsonPath("$[0].cookedAt").value("2026-10-20T10:00:00Z"))
                .andExpect(jsonPath("$[0].rating").value("DOWN")).andExpect(jsonPath("$[0].comment").value("Trop salé"))
                .andExpect(jsonPath("$[0].recipe.title").value("Gratin de courgettes"))
                .andExpect(jsonPath("$[1].id").value(5)).andExpect(jsonPath("$[1].rating").doesNotExist())
                .andExpect(jsonPath("$[1].comment").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void updateFeedback_renvoie200() throws Exception {
        when(cookingService.updateFeedback(7L, Rating.UP, "Délicieux")).thenReturn(
                new CookedRecipe(7L, RECIPE, Instant.parse("2026-10-20T10:00:00Z"), Rating.UP, "Délicieux"));

        mvc.perform(put("/api/cooked-recipes/7/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":\"UP\",\"comment\":\"Délicieux\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.rating").value("UP"))
                .andExpect(jsonPath("$.comment").value("Délicieux"))
                .andExpect(jsonPath("$.cookedAt").value("2026-10-20T10:00:00Z"));
    }

    @Test
    void updateFeedback_idInconnu_renvoie404AvecMessage() throws Exception {
        when(cookingService.updateFeedback(99L, null, null)).thenThrow(new CookedRecipeNotFoundException(99));

        mvc.perform(put("/api/cooked-recipes/99/feedback").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucune recette cuisinée avec l'identifiant 99"));
    }

    @Test
    void updateFeedback_avisInconnu_renvoie400() throws Exception {
        mvc.perform(put("/api/cooked-recipes/7/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":\"BOF\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
