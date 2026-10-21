package fr.antigaspi.recipe.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.service.RecipeService;
import fr.antigaspi.recipe.llm.RecipeGuidelines;

/**
 * Tools Spring AI pour la rédaction d'une recette par le LLM : consignes à respecter et garde-fou sur la recette
 * rédigée.
 * <p>
 * Ils délèguent à {@link RecipeGuidelines} et {@link RecipeService}, sans logique métier.
 */
@Component
public class RecipeTools {

    private static final String LIMITES = """
            Cette vérification ne contrôle que les identifiants et les noms des ingrédients par rapport à l'inventaire, \
            et les ingrédients cités dans aucune étape. Elle ne contrôle ni les quantités, ni les ustensiles, \
            ni la sécurité alimentaire, ni le nombre de viandes ou poissons : respecte toi-même les règles.""";

    private final RecipeGuidelines guidelines;
    private final RecipeService recipeService;

    public RecipeTools(RecipeGuidelines guidelines, RecipeService recipeService) {
        this.guidelines = guidelines;
        this.recipeService = recipeService;
    }

    @Tool(name = "consignesRecette", description = """
            Donne les consignes pour rédiger une recette anti-gaspillage : les règles à respecter (choix des aliments, \
            sécurité alimentaire, ustensiles, quantités) et le format JSON standardisé de la recette. \
            À appeler avant de rédiger une recette ; sans paramètre, renvoie les règles et le format.""")
    public String consignesRecette(
            @ToolParam(required = false, description = "REGLES, FORMAT ou TOUT (par défaut)") Consigne consigne) {
        return switch (consigne == null ? Consigne.TOUT : consigne) {
        case REGLES -> guidelines.rules();
        case FORMAT -> guidelines.format();
        case TOUT -> guidelines.all();
        };
    }

    @Tool(name = "verifierRecette", description = """
            Garde-fou à appeler une fois la recette rédigée, au format standardisé : compare ses ingrédients à \
            l'inventaire complet, corrige les noms et retire les identifiants incohérents. \
            Renvoie la recette corrigée, à utiliser pour la réponse finale, et les anomalies détectées.""")
    public Verification verifierRecette(
            @ToolParam(description = "Recette rédigée, au format standardisé") Recipe recette) {
        GeneratedRecipe checked = recipeService.check(recette);
        return new Verification(checked.recipe(), checked.issues(), LIMITES);
    }

    /** Consignes demandables. */
    public enum Consigne {
        REGLES, FORMAT, TOUT
    }

    /**
     * Résultat de la vérification d'une recette.
     *
     * @param recette
     *            recette corrigée
     * @param problemes
     *            anomalies détectées
     * @param limites
     *            ce que la vérification ne couvre pas
     */
    public record Verification(Recipe recette, List<RecipeIssue> problemes, String limites) {
    }
}
