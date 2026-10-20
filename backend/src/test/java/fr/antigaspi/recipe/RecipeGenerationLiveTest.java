package fr.antigaspi.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import fr.antigaspi.ingredient.Ingredient;
import fr.antigaspi.ingredient.IngredientRepository;

/**
 * Appel réel au LLM configuré (Bedrock par défaut), pour valider le prompt de bout en bout. Exclu du build par défaut
 * car il coûte des tokens et nécessite des identifiants : {@code mvn test -pl backend -Pllm}.
 */
@Tag("llm")
@SpringBootTest
class RecipeGenerationLiveTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Test
    void genereUneRecetteAvecLesAlimentsLesPlusUrgents() {
        Recipe recipe = recipeService.generate(RecipeRequest.mostUrgent());

        System.out.println("Recette générée : " + recipe);
        Set<Long> inventoryIds = ingredientRepository.findAll().stream().map(Ingredient::id)
                .collect(Collectors.toSet());
        assertThat(recipe.title()).isNotBlank();
        assertThat(recipe.steps()).isNotEmpty();
        assertThat(recipe.usedIngredients()).isNotEmpty()
                .allSatisfy(used -> assertThat(inventoryIds).contains(used.ingredientId()));
    }
}
