package fr.antigaspi.recipe.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.utensil.model.Utensil;

/**
 * Garde-fou appliqué à la recette renvoyée par le LLM : vérifie les identifiants et les noms des ingrédients par
 * rapport à l'inventaire, signale les ingrédients cités dans aucune étape et les ustensiles absents des ustensiles
 * disponibles (non contrôlés si aucun n'est renseigné : le LLM se limite alors aux ustensiles de base).
 * <p>
 * Les noms sont comparés sans casse, sans accents et sans pluriel. Un identifiant est conservé si le nom est égal à
 * celui de l'inventaire, s'il en reprend les mots (« Poulet » pour « Blancs de poulet ») ou s'il en est proche (faute
 * de frappe) ; sinon il est retiré et l'ingrédient est traité comme hors inventaire.
 */
@Component
public class RecipeChecker {

    /** Similarité minimale (1 − distance d'édition / longueur) pour considérer un nom comme une faute de frappe. */
    private static final double MIN_SIMILARITY = 0.75;

    private static final Set<String> STOP_WORDS = Set.of("a", "au", "aux", "avec", "d", "de", "des", "du", "en", "et",
            "l", "la", "le", "les", "un", "une");

    public GeneratedRecipe check(Recipe recipe, List<Ingredient> inventory, List<Utensil> utensils) {
        if (recipe.ingredients() == null) {
            return new GeneratedRecipe(recipe, List.of());
        }
        Map<Long, Ingredient> byId = inventory.stream()
                .collect(Collectors.toMap(Ingredient::id, Function.identity(), (first, second) -> first));
        List<RecipeIssue> issues = new ArrayList<>();
        List<RecipeIngredient> checked = recipe.ingredients().stream()
                .map(ingredient -> checkIdentifier(ingredient, byId, issues)).toList();

        checkUtensils(recipe.utensils(), utensils, issues);

        Set<String> stepWords = recipe.steps() == null ? Set.of()
                : recipe.steps().stream().flatMap(step -> words(step).stream()).collect(Collectors.toSet());
        checked.stream().filter(ingredient -> words(ingredient.name()).stream().noneMatch(stepWords::contains))
                .forEach(ingredient -> issues.add(new RecipeIssue(RecipeIssueType.INGREDIENT_NON_UTILISE,
                        "« %s » cité dans aucune étape".formatted(ingredient.name()))));

        Recipe checkedRecipe = checked.equals(recipe.ingredients()) ? recipe : new Recipe(recipe.title(),
                recipe.servings(), recipe.preparationMinutes(), recipe.utensils(), checked, recipe.steps());
        return new GeneratedRecipe(checkedRecipe, List.copyOf(issues));
    }

    private static RecipeIngredient checkIdentifier(RecipeIngredient ingredient, Map<Long, Ingredient> byId,
            List<RecipeIssue> issues) {
        Long id = ingredient.ingredientId();
        if (id == null) {
            return ingredient;
        }
        Ingredient stock = byId.get(id);
        if (stock == null) {
            issues.add(new RecipeIssue(RecipeIssueType.IDENTIFIANT_INCONNU,
                    "Identifiant %d inconnu pour « %s » : identifiant retiré".formatted(id, ingredient.name())));
            return withId(ingredient, null, ingredient.name());
        }
        if (sameWords(ingredient.name(), stock.name())) {
            return withId(ingredient, id, stock.name());
        }
        if (similarity(normalize(ingredient.name()), normalize(stock.name())) >= MIN_SIMILARITY) {
            issues.add(new RecipeIssue(RecipeIssueType.NOM_CORRIGE,
                    "« %s » renommé « %s » (identifiant %d)".formatted(ingredient.name(), stock.name(), id)));
            return withId(ingredient, id, stock.name());
        }
        issues.add(new RecipeIssue(RecipeIssueType.IDENTIFIANT_INCOHERENT,
                "« %s » ne correspond pas à l'identifiant %d (%s) : identifiant retiré".formatted(ingredient.name(), id,
                        stock.name())));
        return withId(ingredient, null, ingredient.name());
    }

    private static void checkUtensils(List<String> used, List<Utensil> available, List<RecipeIssue> issues) {
        if (used == null || available.isEmpty()) {
            return;
        }
        used.stream().filter(name -> available.stream().noneMatch(utensil -> sameWords(name, utensil.name())))
                .forEach(name -> issues.add(new RecipeIssue(RecipeIssueType.USTENSILE_INDISPONIBLE,
                        "« %s » absent des ustensiles disponibles".formatted(name))));
    }

    private static RecipeIngredient withId(RecipeIngredient ingredient, Long id, String name) {
        return new RecipeIngredient(id, name, ingredient.quantity(), ingredient.unit());
    }

    /** Vrai si les mots de l'un des noms sont tous présents dans l'autre. */
    private static boolean sameWords(String name, String stockName) {
        Set<String> nameWords = words(name);
        Set<String> stockWords = words(stockName);
        return !nameWords.isEmpty() && (stockWords.containsAll(nameWords) || nameWords.containsAll(stockWords));
    }

    /** Mots significatifs normalisés, au singulier (sans s ou x final). */
    private static Set<String> words(String text) {
        return Arrays.stream(normalize(text).split(" ")).filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word))
                .map(word -> word.length() > 3 && (word.endsWith("s") || word.endsWith("x"))
                        ? word.substring(0, word.length() - 1) : word)
                .collect(Collectors.toSet());
    }

    /** Minuscules, sans accents ni ligatures, ponctuation remplacée par des espaces. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase().replace("œ", "oe").replace("æ", "ae");
        String withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("[^a-z0-9]+", " ").strip();
    }

    private static double similarity(String a, String b) {
        int maxLength = Math.max(a.length(), b.length());
        return maxLength == 0 ? 1 : 1 - (double) levenshtein(a, b) / maxLength;
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j], current[j - 1]) + 1);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
