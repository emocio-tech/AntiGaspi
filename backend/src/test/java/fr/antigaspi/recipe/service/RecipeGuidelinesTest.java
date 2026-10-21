package fr.antigaspi.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class RecipeGuidelinesTest {

    private final RecipeGuidelines guidelines = new RecipeGuidelines(new ClassPathResource("prompts/recipe-system.md"));

    @Test
    void rules_contientMethodeEtContraintesSansLeFormat() {
        assertThat(guidelines.rules()).startsWith("Méthode, dans cet ordre").contains("Sécurité alimentaire",
                "Ustensiles", "Quantités").doesNotContain("Format de la recette", "preparationMinutes",
                        "Avant de répondre");
    }

    @Test
    void format_contientLeFormatSansLesReglesNiLaVerificationFinale() {
        assertThat(guidelines.format()).startsWith("Format de la recette").contains("title", "servings",
                "preparationMinutes", "utensils", "ingredients", "ingredientId", "steps")
                .doesNotContain("Sécurité alimentaire", "Avant de répondre");
    }

    @Test
    void all_contientLesReglesPuisLeFormat() {
        String all = guidelines.all();

        assertThat(all).contains(guidelines.rules(), guidelines.format());
        assertThat(all.indexOf(guidelines.rules())).isLessThan(all.indexOf(guidelines.format()));
    }

    @Test
    void sourceSansLesReperes_echoueAuDemarrageAvecUnMessageClair() {
        ByteArrayResource source = new ByteArrayResource("Un prompt sans repères".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new RecipeGuidelines(source)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Méthode, dans cet ordre");
    }
}
