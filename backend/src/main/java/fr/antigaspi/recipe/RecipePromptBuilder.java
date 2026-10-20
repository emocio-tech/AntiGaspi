package fr.antigaspi.recipe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.Ingredient;
import fr.antigaspi.ingredient.IngredientView;

/**
 * Construit les prompts envoyés au LLM : le prompt système (rôle et règles, dans
 * {@code resources/prompts/recipe-system.md}) et le prompt utilisateur (aliments, profil, contraintes).
 * <p>
 * Pas d'accolades dans les prompts : Spring AI les interpréterait comme des variables de template.
 */
@Component
public class RecipePromptBuilder {

    private final String systemPrompt;

    public RecipePromptBuilder(@Value("classpath:prompts/recipe-system.md") Resource systemPrompt) {
        try {
            this.systemPrompt = systemPrompt.getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt système introuvable : " + systemPrompt.getDescription(), e);
        }
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userPrompt(RecipeContext context) {
        StringBuilder prompt = new StringBuilder("Propose une recette.\n\n");
        prompt.append("Nombre de portions : ").append(context.servings()).append('\n');
        prompt.append("Temps maximal : ")
                .append(context.maxMinutes() == null ? "aucune contrainte" : context.maxMinutes() + " minutes")
                .append("\n\n");

        prompt.append("Aliments disponibles, du plus urgent au moins urgent (identifiant entre crochets) :\n");
        context.ingredients().forEach(view -> prompt.append(ingredientLine(view)).append('\n'));

        appendList(prompt, "Équipement disponible", context.profile().equipment(), "non renseigné");
        appendList(prompt, "Préférences", context.profile().preferences(), "aucune");
        return prompt.toString();
    }

    /** Exemple : {@code - [3] Courgettes : 2.5 kg, reste 2 jour(s) (date estimée)}. */
    private static String ingredientLine(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        String line = "- [%d] %s : %s %s, %s".formatted(ingredient.id(), ingredient.name(),
                ingredient.quantity().stripTrailingZeros().toPlainString(), ingredient.unit().symbol(),
                expiryText(view.daysLeft()));
        return view.estimated() ? line + " (date estimée)" : line;
    }

    private static String expiryText(long daysLeft) {
        if (daysLeft < 0) {
            return "périmé depuis " + -daysLeft + " jour(s)";
        }
        if (daysLeft == 0) {
            return "à consommer aujourd'hui";
        }
        return "reste " + daysLeft + " jour(s)";
    }

    private static void appendList(StringBuilder prompt, String title, List<String> items, String emptyText) {
        prompt.append('\n').append(title).append(" : ");
        if (items.isEmpty()) {
            prompt.append(emptyText).append('\n');
            return;
        }
        prompt.append('\n');
        items.forEach(item -> prompt.append("- ").append(item).append('\n'));
    }
}
