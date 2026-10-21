package fr.antigaspi.recipe.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.ChatTools;
import fr.antigaspi.recipe.llm.RecipeGenerationException;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeService;

/**
 * Tool Spring AI de génération de recette pour le LLM du chat : la recette est rédigée par {@link RecipeService}
 * (prompt dédié) puis toujours contrôlée par le garde-fou, en un seul appel. Le LLM du chat n'a donc rien à vérifier
 * lui-même.
 * <p>
 * Une demande refusée ou un échec du LLM est renvoyé au LLM du chat sous forme de message, sans exception.
 */
@Component
public class RecipeTools implements ChatTools {

    private final RecipeService recipeService;

    public RecipeTools(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Tool(name = "genererRecette", description = """
            Génère une recette anti-gaspillage à partir de l'inventaire, des ustensiles et des préférences de \
            l'utilisateur, puis la contrôle (identifiants et noms des ingrédients, ustensiles disponibles). \
            À appeler pour toute demande de recette ou de plat : ne rédige jamais de recette toi-même. \
            Sans paramètre, cuisine les aliments les plus urgents. Renvoie la recette au format standardisé et les \
            problèmes détectés, ou une erreur.""")
    public Proposition genererRecette(
            @ToolParam(required = false, description = "Identifiants (champ id de listerIngredients) des aliments à cuisiner en priorité ; absent pour les plus urgents") List<Long> alimentIds,
            @ToolParam(required = false, description = "Nombre de personnes ; absent pour la valeur par défaut") Integer personnes,
            @ToolParam(required = false, description = "Temps total maximal en minutes ; absent si aucune contrainte") Integer tempsMax,
            @ToolParam(required = false, description = "Demande de l'utilisateur en texte libre, par exemple végétarien, plat léger, sans four") String demande) {
        try {
            GeneratedRecipe generated = recipeService
                    .generate(new RecipeRequest(alimentIds, personnes, tempsMax, demande));
            return new Proposition(generated.recipe(), generated.issues(), null);
        } catch (IllegalArgumentException | RecipeGenerationException e) {
            return new Proposition(null, List.of(), e.getMessage());
        }
    }

    /**
     * Résultat de la génération, tel que le LLM du chat le reçoit.
     *
     * @param recette
     *            recette contrôlée (noms corrigés, identifiants incohérents retirés), {@code null} en cas d'erreur
     * @param problemes
     *            anomalies détectées, à signaler à l'utilisateur
     * @param erreur
     *            raison de l'échec, {@code null} si la recette a été générée
     */
    public record Proposition(Recipe recette, List<RecipeIssue> problemes, String erreur) {
    }
}
