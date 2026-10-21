package fr.antigaspi.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.recipe.RecipeFixtures;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.utensil.model.Utensil;

class RecipeCheckerTest {

    private static final List<Ingredient> INVENTORY = List.of(ingredient(2, "Blancs de poulet"),
            ingredient(12, "Pois chiches"), ingredient(13, "Œufs"), ingredient(20, "Poivrons rouges"),
            ingredient(25, "Pommes de terre"));

    private static final List<Utensil> UTENSILS = List.of(new Utensil(1L, "Poêle"),
            new Utensil(2L, "Four à chaleur tournante"));

    private final RecipeChecker checker = new RecipeChecker();

    private static Ingredient ingredient(long id, String name) {
        return new Ingredient(id, name, Category.LEGUMES, BigDecimal.ONE, Unit.PIECE, RecipeFixtures.TODAY,
                RecipeFixtures.NOW);
    }

    private static RecipeIngredient item(Long id, String name) {
        return new RecipeIngredient(id, name, BigDecimal.ONE, "pièce");
    }

    private static Recipe recipe(List<RecipeIngredient> ingredients, String... steps) {
        return new Recipe("Plat", 2, 30, List.of("Poêle"), ingredients, List.of(steps));
    }

    private GeneratedRecipe check(Recipe recipe) {
        return checker.check(recipe, INVENTORY, UTENSILS);
    }

    private static List<RecipeIssueType> types(GeneratedRecipe generated) {
        return generated.issues().stream().map(RecipeIssue::type).toList();
    }

    @Test
    void recetteConforme_inchangeeEtSansAnomalie() {
        Recipe recipe = recipe(List.of(item(2L, "Blancs de poulet"), item(null, "Sel")), "Saisir les blancs de poulet.",
                "Ajouter le sel.");

        GeneratedRecipe generated = check(recipe);

        assertThat(generated.recipe()).isEqualTo(recipe);
        assertThat(generated.issues()).isEmpty();
    }

    @Test
    void nomProcheDeCeluiDeLInventaire_nomCorrigeEtSignale() {
        GeneratedRecipe generated = check(recipe(List.of(item(20L, "Pouvrrons rouges")), "Émincer les poivrons."));

        assertThat(generated.recipe().ingredients()).containsExactly(item(20L, "Poivrons rouges"));
        assertThat(generated.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(RecipeIssueType.NOM_CORRIGE);
            assertThat(issue.message()).contains("Pouvrrons rouges", "Poivrons rouges");
        });
    }

    @Test
    void nomPartielOuSansAccent_nomDeLInventaireRemisSansAnomalie() {
        GeneratedRecipe generated = check(
                recipe(List.of(item(2L, "Poulet"), item(13L, "oeufs")), "Cuire le poulet.", "Battre les œufs."));

        assertThat(generated.recipe().ingredients()).containsExactly(item(2L, "Blancs de poulet"), item(13L, "Œufs"));
        assertThat(generated.issues()).isEmpty();
    }

    @Test
    void nomSansRapportAvecLIdentifiant_identifiantRetireEtSignale() {
        GeneratedRecipe generated = check(recipe(List.of(item(12L, "Poivre")), "Ajouter le poivre."));

        assertThat(generated.recipe().ingredients()).containsExactly(item(null, "Poivre"));
        assertThat(generated.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(RecipeIssueType.IDENTIFIANT_INCOHERENT);
            assertThat(issue.message()).contains("Poivre", "12", "Pois chiches");
        });
    }

    @Test
    void identifiantAbsentDeLInventaire_identifiantRetireEtSignale() {
        GeneratedRecipe generated = check(recipe(List.of(item(99L, "Truffe")), "Râper la truffe."));

        assertThat(generated.recipe().ingredients()).containsExactly(item(null, "Truffe"));
        assertThat(generated.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(RecipeIssueType.IDENTIFIANT_INCONNU);
            assertThat(issue.message()).contains("99", "Truffe");
        });
    }

    @Test
    void ingredientCiteDansAucuneEtape_signaleSansModifierLaRecette() {
        Recipe recipe = recipe(List.of(item(2L, "Blancs de poulet"), item(25L, "Pommes de terre"), item(null, "Sel")),
                "Saisir le poulet.");

        GeneratedRecipe generated = check(recipe);

        assertThat(generated.recipe()).isEqualTo(recipe);
        assertThat(types(generated)).containsExactly(RecipeIssueType.INGREDIENT_NON_UTILISE,
                RecipeIssueType.INGREDIENT_NON_UTILISE);
        assertThat(generated.issues().get(0).message()).contains("Pommes de terre");
        assertThat(generated.issues().get(1).message()).contains("Sel");
    }

    @Test
    void citationDansLesEtapes_ignoreCasseAccentsEtPluriel() {
        GeneratedRecipe generated = check(
                recipe(List.of(item(13L, "Œufs"), item(20L, "Poivrons rouges"), item(null, "Crème fraîche")),
                        "Battre un OEUF.", "Ajouter le poivron.", "Napper de creme."));

        assertThat(generated.issues()).isEmpty();
    }

    @Test
    void motCourtNonDetecteDansUnMotPlusLong() {
        GeneratedRecipe generated = check(recipe(List.of(item(null, "Sel")), "Cuire selon le paquet."));

        assertThat(types(generated)).containsExactly(RecipeIssueType.INGREDIENT_NON_UTILISE);
    }

    @Test
    void listesAbsentes_pasDAnomalieNiDErreur() {
        Recipe recipe = new Recipe("Plat", 2, 30, null, null, null);

        GeneratedRecipe generated = check(recipe);

        assertThat(generated.recipe()).isEqualTo(recipe);
        assertThat(generated.issues()).isEmpty();
    }

    private static Recipe withUtensils(String... utensils) {
        return new Recipe("Plat", 2, 30, List.of(utensils), List.of(item(2L, "Blancs de poulet")),
                List.of("Cuire les blancs de poulet."));
    }

    @Test
    void ustensileAbsentDeLaListe_signaleSansModifierLaRecette() {
        Recipe recipe = withUtensils("Poêle", "Cocotte-minute");

        GeneratedRecipe generated = check(recipe);

        assertThat(generated.recipe()).isEqualTo(recipe);
        assertThat(generated.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(RecipeIssueType.USTENSILE_INDISPONIBLE);
            assertThat(issue.message()).contains("Cocotte-minute");
        });
    }

    @Test
    void ustensileSansAccentOuNomPartiel_reconnu() {
        GeneratedRecipe generated = check(withUtensils("poele", "Four"));

        assertThat(generated.issues()).isEmpty();
    }

    @Test
    void aucunUstensileDisponible_ustensilesNonControles() {
        GeneratedRecipe generated = checker.check(withUtensils("Cocotte-minute"), INVENTORY, List.of());

        assertThat(generated.issues()).isEmpty();
    }
}
