package fr.antigaspi.recipe;

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

import fr.antigaspi.ingredient.Category;
import fr.antigaspi.ingredient.Ingredient;
import fr.antigaspi.ingredient.IngredientService;
import fr.antigaspi.ingredient.IngredientView;
import fr.antigaspi.ingredient.Unit;
import fr.antigaspi.profile.ProfileService;
import fr.antigaspi.profile.UserProfile;
import fr.antigaspi.utensil.Utensil;
import fr.antigaspi.utensil.UtensilService;

class RecipeServiceTest {

    private static final Clock CLOCK = Clock.fixed(RecipeFixtures.NOW, ZoneOffset.UTC);
    private static final UserProfile PROFILE = new UserProfile(List.of("Végétarien"));
    private static final List<Utensil> UTENSILS = List.of(new Utensil(1L, "Four"));
    private static final Recipe RECIPE = new Recipe("Soupe", 2, 30, List.of("Four"), List.of(), List.of("Mixer"));

    private final AtomicReference<RecipeContext> receivedContext = new AtomicReference<>();
    private final RecipeGenerator generator = context -> {
        receivedContext.set(context);
        return RECIPE;
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
        UtensilService utensilService = new UtensilService(() -> UTENSILS);
        return new RecipeService(ingredientService, utensilService, profileService, generator,
                new RecipeProperties(2, 3));
    }

    private List<String> receivedNames() {
        return receivedContext.get().ingredients().stream().map(IngredientView::ingredient).map(Ingredient::name)
                .toList();
    }

    @Test
    void sansAlimentChoisi_envoieLesPlusUrgentsAvecLesValeursParDefaut() {
        RecipeService service = service(ingredient(1, "Riz", 100), ingredient(2, "Tomates", 1),
                ingredient(3, "Courgettes", 2));

        Recipe recipe = service.generate(RecipeRequest.mostUrgent());

        assertThat(recipe).isSameAs(RECIPE);
        assertThat(receivedNames()).containsExactly("Tomates", "Courgettes");
        assertThat(receivedContext.get().servings()).isEqualTo(3);
        assertThat(receivedContext.get().maxMinutes()).isNull();
        assertThat(receivedContext.get().profile()).isEqualTo(PROFILE);
        assertThat(receivedContext.get().utensils()).isEqualTo(UTENSILS);
    }

    @Test
    void avecAlimentsChoisis_envoieUniquementCeuxLaEtLesContraintes() {
        RecipeService service = service(ingredient(1, "Riz", 100), ingredient(2, "Tomates", 1),
                ingredient(3, "Courgettes", 2));

        service.generate(new RecipeRequest(List.of(1L, 3L), 4, 25));

        assertThat(receivedNames()).containsExactly("Courgettes", "Riz");
        assertThat(receivedContext.get().servings()).isEqualTo(4);
        assertThat(receivedContext.get().maxMinutes()).isEqualTo(25);
    }

    @Test
    void requeteNulleOuIdsNuls_equivautAuxPlusUrgents() {
        RecipeService service = service(ingredient(2, "Tomates", 1));

        service.generate(new RecipeRequest(null, null, null));
        assertThat(receivedNames()).containsExactly("Tomates");

        receivedContext.set(null);
        service.generate(null);
        assertThat(receivedNames()).containsExactly("Tomates");
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
