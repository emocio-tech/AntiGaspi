package fr.antigaspi.recipe;

import static fr.antigaspi.recipe.RecipeFixtures.view;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import fr.antigaspi.ingredient.Unit;
import fr.antigaspi.profile.UserProfile;

class RecipePromptBuilderTest {

    private final RecipePromptBuilder builder = new RecipePromptBuilder(
            new ByteArrayResource("Tu es un chef.".getBytes(StandardCharsets.UTF_8)));

    private static final UserProfile PROFILE = new UserProfile(List.of("Végétarien le midi"),
            List.of("Four", "Pas de friteuse"));

    @Test
    void systemPrompt_estLuDepuisLaRessource() {
        assertThat(builder.systemPrompt()).isEqualTo("Tu es un chef.");
    }

    @Test
    void systemPromptDuProjet_existeEtNeContientPasDAccolades() {
        // Les accolades seraient interprétées comme variables de template par Spring AI
        String prompt = new RecipePromptBuilder(new ClassPathResource("prompts/recipe-system.md")).systemPrompt();

        assertThat(prompt).contains("anti-gaspillage").doesNotContain("{", "}");
    }

    @Test
    void userPrompt_listeLesAlimentsDansLOrdreAvecIdQuantiteEtPeremption() {
        RecipeContext context = new RecipeContext(
                List.of(view(1, "Yaourt", "2", Unit.PIECE, -1, false), view(2, "Poulet", "400", Unit.G, 0, false),
                        view(3, "Courgettes", "2.50", Unit.KG, 2, false), view(4, "Pommes", "6", Unit.PIECE, 3, true)),
                PROFILE, 2, null);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("- [1] Yaourt : 2 pièce, périmé depuis 1 jour(s)",
                "- [2] Poulet : 400 g, à consommer aujourd'hui", "- [3] Courgettes : 2.5 kg, reste 2 jour(s)",
                "- [4] Pommes : 6 pièce, reste 3 jour(s) (date estimée)");
        assertThat(prompt.indexOf("Yaourt")).isLessThan(prompt.indexOf("Poulet"));
        assertThat(prompt.indexOf("Poulet")).isLessThan(prompt.indexOf("Courgettes"));
        assertThat(prompt.indexOf("Courgettes")).isLessThan(prompt.indexOf("Pommes"));
    }

    @Test
    void userPrompt_contientProfilPortionsEtTemps() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)), PROFILE, 4,
                30);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Nombre de portions : 4", "Temps maximal : 30 minutes", "- Four",
                "- Pas de friteuse", "- Végétarien le midi");
    }

    @Test
    void userPrompt_sansContrainteNiProfil() {
        RecipeContext context = new RecipeContext(List.of(view(1, "Tomates", "3", Unit.PIECE, 2, false)),
                UserProfile.empty(), 2, null);

        String prompt = builder.userPrompt(context);

        assertThat(prompt).contains("Temps maximal : aucune contrainte", "Équipement disponible : non renseigné",
                "Préférences : aucune");
    }
}
