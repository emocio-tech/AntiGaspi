package fr.antigaspi.recipe.llm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.Resources;

import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.recipe.model.DishType;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.utensil.model.Utensil;

/**
 * Construit les prompts envoyés au LLM : le prompt système (rôle et règles, dans
 * {@code resources/prompts/recipe-system.md}) et le prompt utilisateur (aliments, ustensiles, préférences,
 * contraintes).
 * <p>
 * Pas d'accolades dans les prompts : Spring AI les interpréterait comme des variables de template.
 */
@Component
public class RecipePromptBuilder {

    private final String systemPrompt;

    public RecipePromptBuilder(@Value("classpath:prompts/recipe-system.md") Resource systemPrompt) {
        this.systemPrompt = Resources.readText(systemPrompt);
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userPrompt(RecipeContext context) {
        StringBuilder prompt = new StringBuilder("Propose une recette.\n\n");
        prompt.append("Nombre de personnes : ").append(context.servings()).append('\n');
        prompt.append("Temps maximal : ")
                .append(context.maxMinutes() == null ? "aucune contrainte" : context.maxMinutes() + " minutes")
                .append('\n');
        prompt.append("Demande de l'utilisateur : ")
                .append(context.instructions() == null || context.instructions().isBlank() ? "aucune"
                        : context.instructions().strip())
                .append("\n");
        appendImposed(prompt, context);
        prompt.append('\n');

        prompt.append(
                "Aliments à consommer en priorité, du plus urgent au moins urgent (identifiant entre crochets) :\n");
        context.priorityIngredients().forEach(view -> prompt.append("- ").append(ingredientLine(view)).append('\n'));
        appendList(prompt, "Autres aliments disponibles",
                context.otherIngredients().stream().map(RecipePromptBuilder::ingredientLine).toList(), "aucun");

        appendList(prompt, "Ustensiles disponibles", context.utensils().stream().map(Utensil::name).toList(),
                "non renseignés");
        appendList(prompt, "Préférences", context.profile().preferences(), "aucune");
        return prompt.toString();
    }

    /** Aliment principal et type de plat imposés à une variante ; rien sans variante. */
    private static void appendImposed(StringBuilder prompt, RecipeContext context) {
        DishType imposed = context.imposedDishType();
        if (imposed == null) {
            return;
        }
        IngredientView main = context.imposedMain();
        if (main != null) {
            prompt.append("Aliment principal imposé : [").append(main.ingredient().id()).append("] ")
                    .append(main.ingredient().name()).append('\n');
        }
        prompt.append("Type de plat imposé : ").append(imposed.label()).append('\n');
        String others = Arrays.stream(DishType.values()).filter(type -> type != imposed).map(DishType::label)
                .collect(Collectors.joining(" ; "));
        prompt.append("Les autres propositions sont : ").append(others).append('\n');
    }

    /** Exemple : {@code [3] Courgettes : 2.5 kg, reste 2 jour(s) (date estimée)}. */
    private static String ingredientLine(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        String line = "[%d] %s : %s %s, %s".formatted(ingredient.id(), ingredient.name(),
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
