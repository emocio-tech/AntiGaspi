package fr.antigaspi.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.RecipeFixtures;
import fr.antigaspi.recipe.llm.RecipeGenerator;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;
import fr.antigaspi.utensil.service.UtensilService;

class RecipeServiceTest {

    private static final Clock CLOCK = Clock.fixed(RecipeFixtures.NOW, ZoneOffset.UTC);
    private static final UserProfile PROFILE = new UserProfile(List.of("Végétarien"));
    private static final List<Utensil> UTENSILS = List.of(new Utensil(1L, "Four"));
    private static final Recipe RECIPE = new Recipe("Soupe", 2, 30, List.of("Four"), List.of(), List.of("Mixer"));

    private final AtomicReference<RecipeContext> receivedContext = new AtomicReference<>();
    private final AtomicReference<Recipe> llmAnswer = new AtomicReference<>(RECIPE);
    private final RecipeGenerator generator = context -> {
        receivedContext.set(context);
        return llmAnswer.get();
    };
    private final ProfileService profileService = mock(ProfileService.class);

    @BeforeEach
    void setUp() {
        when(profileService.getProfile()).thenReturn(PROFILE);
    }

    private static Ingredient ingredient(long id, String name, int expiresInDays) {
        return new Ingredient(id, name, Category.LEGUMES, BigDecimal.ONE, Unit.PIECE,
                RecipeFixtures.TODAY.plusDays(expiresInDays), RecipeFixtures.NOW);
    }

    private RecipeService service(Ingredient... ingredients) {
        IngredientService ingredientService = new IngredientService(() -> List.of(ingredients), CLOCK);
        UtensilService utensilService = new UtensilService(
                new InMemoryUtensilRepository(UTENSILS.toArray(Utensil[]::new)));
        return new RecipeService(ingredientService, utensilService, profileService, generator, new RecipeChecker(),
                new RecipeProperties(2, 3));
    }

    private List<String> priorityNames() {
        return names(receivedContext.get().priorityIngredients());
    }

    private List<String> otherNames() {
        return names(receivedContext.get().otherIngredients());
    }

    private static List<String> names(List<IngredientView> views) {
        return views.stream().map(IngredientView::ingredient).map(Ingredient::name).toList();
    }

    @Test
    void sansAlimentChoisi_lesPlusUrgentsSontPrioritairesEtLeResteDeLInventaireEstFourni() {
        RecipeService service = service(ingredient(1, "Riz", 100), ingredient(2, "Tomates", 1),
                ingredient(3, "Courgettes", 2), ingredient(4, "Pâtes", 50));

        GeneratedRecipe generated = service.generate(RecipeRequest.mostUrgent());

        assertThat(generated.recipe()).isEqualTo(RECIPE);
        assertThat(generated.issues()).isEmpty();
        assertThat(priorityNames()).containsExactly("Tomates", "Courgettes");
        assertThat(otherNames()).containsExactly("Pâtes", "Riz");
        assertThat(receivedContext.get().servings()).isEqualTo(3);
        assertThat(receivedContext.get().maxMinutes()).isNull();
        assertThat(receivedContext.get().profile()).isEqualTo(PROFILE);
        assertThat(receivedContext.get().utensils()).isEqualTo(UTENSILS);
    }

    @Test
    void avecAlimentsChoisis_ceuxLaSontPrioritairesEtLeResteDeLInventaireEstFourni() {
        RecipeService service = service(ingredient(1, "Riz", 100), ingredient(2, "Tomates", 1),
                ingredient(3, "Courgettes", 2));

        service.generate(new RecipeRequest(List.of(1L, 3L), 4, 25));

        assertThat(priorityNames()).containsExactly("Courgettes", "Riz");
        assertThat(otherNames()).containsExactly("Tomates");
        assertThat(receivedContext.get().servings()).isEqualTo(4);
        assertThat(receivedContext.get().maxMinutes()).isEqualTo(25);
    }

    @Test
    void requeteNulleOuIdsNuls_equivautAuxPlusUrgents() {
        RecipeService service = service(ingredient(2, "Tomates", 1));

        service.generate(new RecipeRequest(null, null, null));
        assertThat(priorityNames()).containsExactly("Tomates");
        assertThat(otherNames()).isEmpty();

        receivedContext.set(null);
        service.generate(null);
        assertThat(priorityNames()).containsExactly("Tomates");
    }

    @Test
    void recetteDuLlm_controleeContreToutLInventaire() {
        RecipeService service = service(ingredient(1, "Riz", 100), ingredient(2, "Tomates", 1),
                ingredient(3, "Courgettes", 2));
        llmAnswer.set(new Recipe("Riz sauté", 2, 20, List.of(),
                List.of(new RecipeIngredient(1L, "Riz", BigDecimal.ONE, "pièce"),
                        new RecipeIngredient(99L, "Truffe", BigDecimal.ONE, "pièce")),
                List.of("Cuire le riz et la truffe.")));

        GeneratedRecipe generated = service.generate(RecipeRequest.mostUrgent());

        // Riz n'est pas prioritaire mais fait partie de l'inventaire : son identifiant est conservé
        assertThat(generated.recipe().ingredients()).extracting(RecipeIngredient::ingredientId).containsExactly(1L,
                null);
        assertThat(generated.issues()).extracting(RecipeIssue::type)
                .containsExactly(RecipeIssueType.IDENTIFIANT_INCONNU);
    }

    @Test
    void aucunAlimentDisponible_refuseSansAppelerLeLlm() {
        assertThatThrownBy(() -> service().generate(RecipeRequest.mostUrgent()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> service(ingredient(1, "Riz", 100)).generate(new RecipeRequest(List.of(99L), null, null)))
                        .isInstanceOf(IllegalArgumentException.class);
        assertThat(receivedContext.get()).isNull();
    }

    @Test
    void portionsOuTempsInvalides_refusesSansAppelerLeLlm() {
        RecipeService service = service(ingredient(1, "Riz", 100));

        assertThatThrownBy(() -> service.generate(new RecipeRequest(null, 0, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generate(new RecipeRequest(null, null, -5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(receivedContext.get()).isNull();
    }
}
