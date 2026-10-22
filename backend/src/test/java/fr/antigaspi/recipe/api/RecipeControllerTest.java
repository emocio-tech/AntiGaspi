package fr.antigaspi.recipe.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.recipe.llm.RecipeGenerationException;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeService;

/**
 * API REST de génération de recette : {@link RecipeService} est simulé, donc aucun appel au LLM.
 */
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    private static final Recipe RECIPE = new Recipe("Gratin de courgettes", 2, 35, List.of("Four", "Poêle"),
            List.of(new RecipeIngredient(2L, "Courgettes", new BigDecimal("2.5"), "kg"),
                    new RecipeIngredient(null, "Sel", new BigDecimal("1"), "pincée")),
            List.of("Couper les courgettes.", "Enfourner 25 minutes."));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecipeService recipeService;

    private static GeneratedRecipe conforme() {
        return new GeneratedRecipe(RECIPE, List.of());
    }

    private RecipeRequest requeteTransmise() {
        ArgumentCaptor<RecipeRequest> captor = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(recipeService).generate(captor.capture());
        return captor.getValue();
    }

    @Test
    void generate_renvoieLaRecetteEtLesAnomaliesAvecLeurLibelle() throws Exception {
        when(recipeService.generate(any())).thenReturn(new GeneratedRecipe(RECIPE,
                List.of(new RecipeIssue(RecipeIssueType.USTENSILE_INDISPONIBLE, "« Four » n'est pas disponible"))));

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recipe.title").value("Gratin de courgettes"))
                .andExpect(jsonPath("$.recipe.servings").value(2))
                .andExpect(jsonPath("$.recipe.preparationMinutes").value(35))
                .andExpect(jsonPath("$.recipe.utensils[1]").value("Poêle"))
                .andExpect(jsonPath("$.recipe.ingredients[0].ingredientId").value(2))
                .andExpect(jsonPath("$.recipe.ingredients[0].name").value("Courgettes"))
                .andExpect(jsonPath("$.recipe.ingredients[0].quantity").value(2.5))
                .andExpect(jsonPath("$.recipe.ingredients[0].unit").value("kg"))
                .andExpect(jsonPath("$.recipe.ingredients[1].ingredientId").doesNotExist())
                .andExpect(jsonPath("$.recipe.steps[1]").value("Enfourner 25 minutes."))
                .andExpect(jsonPath("$.issues[0].type").value("USTENSILE_INDISPONIBLE"))
                .andExpect(jsonPath("$.issues[0].label").value("Ustensile indisponible"))
                .andExpect(jsonPath("$.issues[0].message").value("« Four » n'est pas disponible"));
    }

    @Test
    void generate_recetteConforme_renvoieUneListeDAnomaliesVide() throws Exception {
        when(recipeService.generate(any())).thenReturn(conforme());

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.issues").isEmpty());
    }

    @Test
    void generate_transmetLeCorpsAuService() throws Exception {
        when(recipeService.generate(any())).thenReturn(conforme());

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredientIds\":[3,5],\"servings\":4,\"maxMinutes\":30,\"instructions\":\"sans four\"}"))
                .andExpect(status().isOk());

        RecipeRequest request = requeteTransmise();
        assertThat(request.ingredientIds()).containsExactly(3L, 5L);
        assertThat(request.servings()).isEqualTo(4);
        assertThat(request.maxMinutes()).isEqualTo(30);
        assertThat(request.instructions()).isEqualTo("sans four");
    }

    @Test
    void generate_transmetLaVarianteEtRefuseUneVarianteIllisible() throws Exception {
        when(recipeService.generate(any())).thenReturn(conforme());

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"variant\":1}"))
                .andExpect(status().isOk());
        assertThat(requeteTransmise().variant()).isEqualTo(1);

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"variant\":\"abc\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void generate_corpsVide_cuisineLesPlusUrgents() throws Exception {
        when(recipeService.generate(any())).thenReturn(conforme());

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        RecipeRequest request = requeteTransmise();
        assertThat(request.ingredientIds()).isNull();
        assertThat(request.servings()).isNull();
        assertThat(request.maxMinutes()).isNull();
    }

    @Test
    void generate_sansCorps_estAcceptee() throws Exception {
        when(recipeService.generate(any())).thenReturn(conforme());

        mvc.perform(post("/api/recipes")).andExpect(status().isOk()).andExpect(jsonPath("$.recipe.title").exists());
    }

    @Test
    void generate_demandeRefusee_renvoie400AvecLeMessageDuService() throws Exception {
        when(recipeService.generate(any()))
                .thenThrow(new IllegalArgumentException("Aucun aliment disponible pour générer une recette"));

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Aucun aliment disponible pour générer une recette"));
    }

    @Test
    void generate_echecDuLlm_renvoie502AvecUnMessage() throws Exception {
        when(recipeService.generate(any())).thenThrow(new RecipeGenerationException("Réponse du LLM illisible"));

        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("Réponse du LLM illisible"));
    }
}
