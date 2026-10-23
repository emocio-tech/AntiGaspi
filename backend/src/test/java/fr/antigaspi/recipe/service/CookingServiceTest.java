package fr.antigaspi.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.IngredientRepository;
import fr.antigaspi.ingredient.repository.JpaIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.model.Consumption;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.repository.CookedRecipeRepository;
import fr.antigaspi.recipe.repository.JpaCookedRecipeRepository;

@DataJpaTest
@Import({ CookingService.class, IngredientService.class, JpaIngredientRepository.class, JpaCookedRecipeRepository.class,
        CookingServiceTest.FixedClock.class })
class CookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final Recipe RECIPE = new Recipe("Gratin de courgettes", 2, 35, List.of("Four"),
            List.of(new RecipeIngredient(null, "Courgettes", new BigDecimal("1"), "kg")),
            List.of("Couper.", "Enfourner."));

    @TestConfiguration
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private CookingService service;

    @Autowired
    private IngredientRepository ingredients;

    @Autowired
    private DataSource dataSource;

    @MockitoSpyBean
    private CookedRecipeRepository cookedRepository;

    private long add(String name, String quantity, Unit unit) {
        return ingredients.save(new Ingredient(null, name, Category.LEGUMES, new BigDecimal(quantity), unit,
                LocalDate.of(2026, 10, 25), NOW)).id();
    }

    private BigDecimal quantityOf(long id) {
        return ingredients.findById(id).orElseThrow().quantity();
    }

    private long countCooked() {
        return JdbcClient.create(dataSource).sql("select count(*) from cooked_recipe").query(Long.class).single();
    }

    private static Consumption use(long id, String quantity) {
        return new Consumption(id, quantity == null ? null : new BigDecimal(quantity));
    }

    @Test
    void cook_decrementeLesQuantitesUtilisees() {
        long courgettes = add("Courgettes", "2.5", Unit.KG);
        long lait = add("Lait", "1", Unit.L);

        service.cook(RECIPE, null, null, List.of(use(courgettes, "1"), use(lait, "0.25")));

        assertThat(quantityOf(courgettes)).isEqualByComparingTo("1.5");
        assertThat(quantityOf(lait)).isEqualByComparingTo("0.75");
    }

    @Test
    void cook_alimentEpuise_estRetire() {
        long oeufs = add("Œufs", "6", Unit.PIECE);

        service.cook(RECIPE, null, null, List.of(use(oeufs, "6")));

        assertThat(ingredients.findById(oeufs)).isEmpty();
    }

    @Test
    void cook_quantiteSuperieureAuStock_retireSansErreur() {
        long oeufs = add("Œufs", "6", Unit.PIECE);

        service.cook(RECIPE, null, null, List.of(use(oeufs, "10")));

        assertThat(ingredients.findById(oeufs)).isEmpty();
    }

    @Test
    void cook_quantiteNulle_alimentInchange() {
        long lait = add("Lait", "1", Unit.L);

        service.cook(RECIPE, null, null, List.of(use(lait, "0")));

        assertThat(quantityOf(lait)).isEqualByComparingTo("1");
    }

    @Test
    void cook_quantiteNegative_refuseeRienNEstModifie() {
        long courgettes = add("Courgettes", "2.5", Unit.KG);
        long lait = add("Lait", "1", Unit.L);

        assertThatThrownBy(() -> service.cook(RECIPE, Rating.UP, "bon", List.of(use(courgettes, "1"), use(lait, "-1"))))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("La quantité utilisée doit être positive ou nulle : -1");

        assertThat(quantityOf(courgettes)).isEqualByComparingTo("2.5");
        assertThat(quantityOf(lait)).isEqualByComparingTo("1");
        assertThat(countCooked()).isZero();
    }

    @Test
    void cook_alimentDejaRetire_ligneIgnoreeLeResteApplique() {
        long lait = add("Lait", "1", Unit.L);

        service.cook(RECIPE, null, null, List.of(use(9999, "1"), use(lait, "0.5")));

        assertThat(quantityOf(lait)).isEqualByComparingTo("0.5");
        assertThat(countCooked()).isEqualTo(1);
    }

    @Test
    void cook_enregistreRecetteAvisCommentaireEtDate() {
        CookedRecipe cooked = service.cook(RECIPE, Rating.DOWN, "  Trop salé \n", null);

        assertThat(cooked.id()).isNotNull();
        assertThat(cooked.cookedAt()).isEqualTo(NOW);
        assertThat(cooked.comment()).isEqualTo("Trop salé");
        Map<String, Object> row = JdbcClient.create(dataSource)
                .sql("select title, content, cooked_at, rating, comment from cooked_recipe where id = ?")
                .param(cooked.id()).query().singleRow();
        assertThat(row.get("title")).isEqualTo("Gratin de courgettes");
        assertThat((String) row.get("content")).contains("Gratin de courgettes");
        assertThat(((OffsetDateTime) row.get("cooked_at")).toInstant()).isEqualTo(NOW);
        assertThat(row.get("rating")).isEqualTo("DOWN");
        assertThat(row.get("comment")).isEqualTo("Trop salé");
    }

    @Test
    void cook_commentaireBlanc_enregistreCommeNul() {
        CookedRecipe cooked = service.cook(RECIPE, null, "   ", List.of());

        assertThat(cooked.comment()).isNull();
        assertThat(cooked.rating()).isNull();
        assertThat(JdbcClient.create(dataSource).sql("select comment from cooked_recipe where id = ?")
                .param(cooked.id()).query(String.class).optional()).isEmpty();
    }

    @Test
    void cook_recetteVideOuCommentaireTropLong_refuse() {
        Recipe sansTitre = new Recipe(" ", 2, 35, List.of(), List.of(), List.of());

        assertThatThrownBy(() -> service.cook(null, null, null, null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("La recette est vide");
        assertThatThrownBy(() -> service.cook(sansTitre, null, null, null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("La recette est vide");
        assertThatThrownBy(() -> service.cook(RECIPE, null, "x".repeat(1001), null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le commentaire dépasse 1000 caractères");
        assertThat(countCooked()).isZero();
    }

    @Test
    void cook_memeAlimentSurDeuxLignes_quantitesCumulees() {
        long courgettes = add("Courgettes", "2.5", Unit.KG);

        service.cook(RECIPE, null, null, List.of(use(courgettes, "1"), use(courgettes, "0.5")));

        assertThat(quantityOf(courgettes)).isEqualByComparingTo("1");
    }

    /**
     * Transaction réelle (pas celle, annulée, du test) : l'échec de l'enregistrement de la recette cuisinée est simulé
     * par un espion sur le dépôt, ce qui ne dépend d'aucune règle de validation ; la transaction de {@code cook} doit
     * alors annuler la décrémentation déjà faite.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cook_echecDEnregistrement_aucuneQuantiteModifiee() {
        try {
            long lait = add("Lait", "1", Unit.L);
            doThrow(new IllegalStateException("panne")).when(cookedRepository).save(any());

            assertThatThrownBy(() -> service.cook(RECIPE, null, null, List.of(use(lait, "0.4"))))
                    .isInstanceOf(IllegalStateException.class).hasMessage("panne");

            assertThat(quantityOf(lait)).isEqualByComparingTo("1");
        } finally {
            JdbcClient.create(dataSource).sql("delete from ingredient").update();
            JdbcClient.create(dataSource).sql("delete from cooked_recipe").update();
        }
    }

    @Test
    void cook_ligneSansAliment_refuseeRienNEstModifie() {
        long lait = add("Lait", "1", Unit.L);
        List<Consumption> sansId = List.of(use(lait, "0.5"), new Consumption(null, BigDecimal.ONE));
        List<Consumption> ligneNulle = java.util.Arrays.asList(use(lait, "0.5"), null);

        assertThatThrownBy(() -> service.cook(RECIPE, null, null, sansId))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("L'aliment utilisé est obligatoire");
        assertThatThrownBy(() -> service.cook(RECIPE, null, null, ligneNulle))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("L'aliment utilisé est obligatoire");

        assertThat(quantityOf(lait)).isEqualByComparingTo("1");
        assertThat(countCooked()).isZero();
    }

    @Test
    void cook_titreTropLong_refuse() {
        Recipe recipe = new Recipe("x".repeat(201), 2, 35, List.of(), List.of(), List.of());

        assertThatThrownBy(() -> service.cook(recipe, null, null, null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le titre de la recette dépasse 200 caractères");
        assertThat(countCooked()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cook_recetteTropLongue_refuseeStockIntact() {
        try {
            long lait = add("Lait", "1", Unit.L);
            Recipe recipe = new Recipe("Soupe", 2, 35, List.of(), List.of(), List.of("y".repeat(20000)));

            assertThatThrownBy(() -> service.cook(recipe, null, null, List.of(use(lait, "0.4"))))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La recette est trop longue pour être enregistrée");

            assertThat(quantityOf(lait)).isEqualByComparingTo("1");
            assertThat(countCooked()).isZero();
        } finally {
            JdbcClient.create(dataSource).sql("delete from ingredient").update();
            JdbcClient.create(dataSource).sql("delete from cooked_recipe").update();
        }
    }
}
