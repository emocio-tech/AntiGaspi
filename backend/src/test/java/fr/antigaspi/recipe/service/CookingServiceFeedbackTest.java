package fr.antigaspi.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.repository.InMemoryCookedRecipeRepository;

/** Historique et retours de {@link CookingService}, sur un dépôt en mémoire. */
class CookingServiceFeedbackTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private static CookedRecipe cooked(long id, String title, String cookedAt, Rating rating, String comment) {
        return new CookedRecipe(id, new Recipe(title, 2, 30, List.of("Four"), List.of(), List.of("Cuire.")),
                Instant.parse(cookedAt), rating, comment);
    }

    private static CookingService service(CookedRecipe... initial) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new CookingService(new IngredientService(new InMemoryIngredientRepository(), clock),
                new InMemoryCookedRecipeRepository(initial), clock);
    }

    private final CookedRecipe soupe = cooked(1, "Soupe", "2026-10-17T12:00:00Z", Rating.UP, "bonne");
    private final CookedRecipe curry = cooked(2, "Curry", "2026-10-19T12:00:00Z", null, null);
    private final CookedRecipe gratin = cooked(3, "Gratin", "2026-10-18T12:00:00Z", Rating.DOWN, "fade");

    @Test
    void history_plusRecentesDAbord_videSiAucune() {
        assertThat(service().history()).isEmpty();
        assertThat(service(soupe, curry, gratin).history()).containsExactly(curry, gratin, soupe);
    }

    @Test
    void updateFeedback_modifieAvisEtCommentaireSansToucherDateNiRecette() {
        CookingService service = service(soupe, curry);

        CookedRecipe updated = service.updateFeedback(1, Rating.DOWN, "  Trop salé \n");

        assertThat(updated).isEqualTo(new CookedRecipe(1L, soupe.recipe(), soupe.cookedAt(), Rating.DOWN, "Trop salé"));
        assertThat(service.history()).containsExactly(curry, updated);
    }

    @Test
    void updateFeedback_avisNulEtCommentaireBlanc_efface() {
        CookedRecipe updated = service(soupe).updateFeedback(1, null, "   ");

        assertThat(updated.rating()).isNull();
        assertThat(updated.comment()).isNull();
        assertThat(updated.cookedAt()).isEqualTo(soupe.cookedAt());
    }

    @Test
    void updateFeedback_idInconnuOuCommentaireTropLong_refuse() {
        CookingService service = service(soupe);

        assertThatThrownBy(() -> service.updateFeedback(99, Rating.UP, "ok"))
                .isExactlyInstanceOf(CookedRecipeNotFoundException.class)
                .hasMessage("Aucune recette cuisinée avec l'identifiant 99");
        assertThatThrownBy(() -> service.updateFeedback(1, Rating.UP, "x".repeat(1001)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le commentaire dépasse 1000 caractères");
        assertThat(service.history()).containsExactly(soupe);
    }

    @Test
    void recent_lesNPlusRecentes() {
        CookingService service = service(soupe, curry, gratin);

        assertThat(service.recent(2)).containsExactly(curry, gratin);
        assertThat(service.recent(10)).containsExactly(curry, gratin, soupe);
        assertThat(service.recent(0)).isEmpty();
    }
}
