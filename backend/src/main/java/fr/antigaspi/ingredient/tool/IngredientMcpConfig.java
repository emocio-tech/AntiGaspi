package fr.antigaspi.ingredient.tool;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Tools exposés par le serveur MCP (profil {@code mcp}) : la consultation et la modification de l'inventaire.
 * <p>
 * Le serveur MCP est servi en Streamable HTTP sur {@code /mcp}. Seuls {@link IngredientTools}
 * ({@code listerIngredients}) et {@link IngredientMcpTools} ({@code ajouterIngredients}, {@code supprimerIngredients},
 * {@code modifierIngredients}) sont injectés : on n'injecte volontairement pas {@code List<ChatTools>}, pour ne pas
 * exposer les tools du chat (dont {@link IngredientEditTools}), les ustensiles, les préférences ni la génération de
 * recette.
 */
@Configuration
@Profile("mcp")
public class IngredientMcpConfig {

    /**
     * Déclare au serveur MCP les tools à exposer.
     *
     * @param ingredientTools
     *            tool de consultation de l'inventaire, dont seul {@code listerIngredients} est publié
     * @param ingredientMcpTools
     *            tools d'ajout, de suppression et de modification de l'inventaire
     *
     * @return le fournisseur de tools collecté par l'auto-configuration du serveur MCP
     */
    @Bean
    public ToolCallbackProvider ingredientToolCallbackProvider(IngredientTools ingredientTools,
            IngredientMcpTools ingredientMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(ingredientTools, ingredientMcpTools).build();
    }
}
