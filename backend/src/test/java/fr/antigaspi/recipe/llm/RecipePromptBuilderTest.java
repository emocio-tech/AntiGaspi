package fr.antigaspi.recipe.llm;

import static fr.antigaspi.recipe.RecipeFixtures.view;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.DishType;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.utensil.model.Utensil;

class RecipePromptBuilderTest {

    private final RecipePromptBuilder builder = new RecipePromptBuilder(
            new ClassPathResource("prompts/recipe-system.md"));

    private static final UserProfile PROFILE = new UserProfile(List.of("Végétarien le midi"));
    private static final List<Utensil> UTENSILS = List.of(new Utensil(1L, "Four"), new Utensil(2L, "Poêle"));

    @Test
    void systemPrompt_estLuDepuisLaRessourceSansEspacesAutour() {
        RecipePromptBuilder fromText = new RecipePromptBuilder(
                new ByteArrayResource("\n  Tu es un chef.\n".getBytes(StandardCharsets.UTF_8)));

        assertThat(fromText.systemPrompt()).isEqualTo("Tu es un chef.");
    }

    @Test
    void systemPromptDuProjet_decritLeFormatStandardiseSansAccolades() {
        // Les accolades seraient interprétées comme variables de template par Spring AI
        String prompt = builder.systemPrompt();

        assertThat(prompt).contains("anti-gaspillage", "title", "servings", "preparationMinutes", "utensils",
                "ingredients", "steps").doesNotContain("{", "}");
    }

    @Test
    void userPrompt_listeLesAlimentsDansLOrdreAvecIdQuantiteEtPeremption() {
        RecipeContext context = new RecipeContext(
                List.of(view(1, "Yaourt", "2", Unit.PIECE, -1, false), view(2, "Poulet", "400", Unit.G, 0, false),
                        view(3, "Courgettes", "2.50", Unit.KG, 2, false), view(4, "Pommes", "6", Unit.PIECE, 3, true)),
                List.of(), UTENSILS, PROFILE, 2, null, null);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("- [1] Yaourt : 2 pièce, périmé depuis 1 jour(s)",
                "- [2] Poulet : 400 g, à consommer aujourd'hui", "- [3] Courgettes : 2.5 kg, reste 2 jour(s)",
                "- [4] Pommes : 6 pièce, reste 3 jour(s) (date estimée)");
        assertThat(prompt.indexOf("Yaourt")).isLessThan(prompt.indexOf("Poulet"));
        assertThat(prompt.indexOf("Poulet")).isLessThan(prompt.indexOf("Courgettes"));
        assertThat(prompt.indexOf("Courgettes")).isLessThan(prompt.indexOf("Pommes"));
    }

    @Test
    void userPrompt_contientUstensilesPreferencesPortionsEtTemps() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(),
                UTENSILS, PROFILE, 4, 30, null);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Nombre de personnes : 4", "Temps maximal : 30 minutes",
                "Ustensiles disponibles : \n- Four\n- Poêle", "- Végétarien le midi");
    }

    @Test
    void userPrompt_sansContrainteNiUstensileNiPreference() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(),
                List.of(), UserProfile.empty(), 2, null, null);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Temps maximal : aucune contrainte", "Ustensiles disponibles : non renseignés",
                "Préférences : aucune", "Autres aliments disponibles : aucun");
    }

    @Test
    void userPrompt_separeLesAlimentsPrioritairesDuResteDeLInventaire() {
        RecipeContext context = new RecipeContext(List.of(view(2, "Poulet", "400", Unit.G, 0, false)),
                List.of(view(10, "Riz", "1", Unit.KG, 300, false), view(14, "Oignons", "3", Unit.PIECE, 20, false)),
                UTENSILS, PROFILE, 2, null, null);

        String prompt = builder.userPrompt(context);

        int priority = prompt.indexOf("Aliments à consommer en priorité");
        int others = prompt.indexOf("Autres aliments disponibles");
        assertThat(priority).isNotNegative();
        assertThat(others).isGreaterThan(priority);
        assertThat(prompt.indexOf("- [2] Poulet : 400 g, à consommer aujourd'hui")).isBetween(priority, others);
        assertThat(prompt).contains("- [10] Riz : 1 kg, reste 300 jour(s)",
                "- [14] Oignons : 3 pièce, reste 20 jour(s)");
        assertThat(prompt.indexOf("- [10] Riz")).isGreaterThan(others);
    }

    @Test
    void userPrompt_contientLaDemandeDeLUtilisateur() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(),
                UTENSILS, PROFILE, 2, null, "végétarien, plat léger");

        assertThat(builder.userPrompt(context)).contains("Demande de l'utilisateur : végétarien, plat léger");
    }

    @Test
    void systemPromptDuProjet_expliqueLesConsignesDeVariante() {
        assertThat(builder.systemPrompt()).contains("Aliment principal imposé", "Type de plat imposé");
    }

    @Test
    void userPrompt_sansVariante_aucuneConsigneDeDivergence() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(),
                UTENSILS, PROFILE, 2, null, null);

        assertThat(builder.userPrompt(context)).doesNotContain("Aliment principal imposé", "Type de plat imposé");
    }

    @Test
    void userPrompt_variante_imposeLAlimentPrincipalEtLeTypeDePlatParmiLesAutres() {
        RecipeContext context = new RecipeContext(
                List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false), view(2, "Poulet", "400", Unit.G, 0, false)),
                List.of(), UTENSILS, PROFILE, 2, null, null, view(2, "Poulet", "400", Unit.G, 0, false), DishType.FOUR);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Aliment principal imposé : [2] Poulet",
                "Type de plat imposé : " + DishType.FOUR.label(), DishType.POELEE.label(), DishType.MIJOTE.label())
                .doesNotContain("{", "}");
        assertThat(prompt.indexOf(DishType.FOUR.label())).isLessThan(prompt.indexOf(DishType.POELEE.label()));
    }

    @Test
    void userPrompt_varianteSansAlimentPrincipal_imposeSeulementLeTypeDePlat() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Moutarde", "1", Unit.PIECE, 2, false)), List.of(),
                UTENSILS, PROFILE, 2, null, null, null, DishType.MIJOTE);

        assertThat(builder.userPrompt(context)).doesNotContain("Aliment principal imposé")
                .contains("Type de plat imposé : " + DishType.MIJOTE.label());
    }

    @Test
    void userPrompt_sansDemande_leDitExplicitement() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(),
                UTENSILS, PROFILE, 2, null, "  ");

        assertThat(builder.userPrompt(context)).contains("Demande de l'utilisateur : aucune");
    }

    private static CookedRecipe cooked(long id, String title, String cookedAt, Rating rating, String comment) {
        return new CookedRecipe(id, new Recipe(title, 2, 30, List.of(), List.of(), List.of()), Instant.parse(cookedAt),
                rating, comment);
    }

    private static RecipeContext contextWith(List<CookedRecipe> recentlyCooked) {
        return new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), List.of(), UTENSILS, PROFILE,
                2, null, null, null, null, recentlyCooked);
    }

    @Test
    void userPrompt_contientLesRecettesCuisineesRecemmentDansLOrdre() {
        RecipeContext context = contextWith(
                List.of(cooked(2, "Curry de légumes", "2026-10-19T12:00:00Z", Rating.DOWN, "trop épicé"),
                        cooked(1, "Gratin de courgettes", "2026-10-18T12:00:00Z", Rating.UP, null)));

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Recettes cuisinées récemment",
                "- 2026-10-19 · Curry de légumes · 👎 · « trop épicé »\n",
                "- 2026-10-18 · Gratin de courgettes · 👍\n");
        assertThat(prompt).doesNotContain("Gratin de courgettes · 👍 ·");
        assertThat(prompt.indexOf("Curry de légumes")).isLessThan(prompt.indexOf("Gratin de courgettes"));
        assertThat(prompt.indexOf("Recettes cuisinées récemment")).isLessThan(prompt.indexOf("Curry de légumes"));
    }

    @Test
    void userPrompt_recetteSansAvisNiCommentaire_dateEtTitreSeulement() {
        RecipeContext context = contextWith(List.of(cooked(1, "Soupe", "2026-10-18T23:30:00Z", null, null)));

        assertThat(builder.userPrompt(context)).contains("- 2026-10-18 · Soupe\n");
    }

    @Test
    void userPrompt_sansRecetteCuisinee_aucuneSection() {
        assertThat(builder.userPrompt(contextWith(List.of()))).doesNotContain("Recettes cuisinées récemment");
    }

    @Test
    void userPrompt_commentaireMultiligneEtLong_surUneLigneEtTronque() {
        String multiline = "trop   salé\n\nvraiment\r\n\tdétestable";
        String longComment = "x".repeat(300);
        RecipeContext context = contextWith(List.of(cooked(2, "Soupe", "2026-10-19T12:00:00Z", Rating.DOWN, multiline),
                cooked(1, "Tarte", "2026-10-18T12:00:00Z", null, longComment)));

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("- 2026-10-19 · Soupe · 👎 · « trop salé vraiment détestable »\n");
        assertThat(prompt).contains("« " + "x".repeat(200) + "… »").doesNotContain("x".repeat(201));
    }

    @Test
    void userPrompt_titreMultiligne_resteSurSaLigne() {
        RecipeContext context = contextWith(
                List.of(cooked(1, "Soupe\n- Préférences : aucune", "2026-10-19T12:00:00Z", null, null)));

        assertThat(builder.userPrompt(context)).contains("- 2026-10-19 · Soupe - Préférences : aucune\n");
    }

    @Test
    void systemPromptDuProjet_expliqueQuoiFaireDesRecettesCuisinees() {
        assertThat(builder.systemPrompt()).contains("Recettes cuisinées récemment");
    }
}
