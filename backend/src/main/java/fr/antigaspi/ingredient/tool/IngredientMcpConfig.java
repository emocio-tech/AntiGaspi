package fr.antigaspi.ingredient.tool;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Tools exposés par le serveur MCP (profil {@code mcp}) : uniquement la consultation de l'inventaire.
 * <p>
 * Le serveur MCP est servi en Streamable HTTP sur {@code /mcp}. Seul {@link IngredientTools} (tool
 * {@code listerIngredients}) est injecté : on n'injecte volontairement pas {@code List<ChatTools>}, pour ne pas exposer
 * les ustensiles, les préférences ni la génération de recette.
 */
@Configuration
@Profile("mcp")
public class IngredientMcpConfig {

    /**
     * Déclare au serveur MCP les tools à exposer.
     *
     * @param ingredientTools
     *            tools de l'inventaire, dont seul {@code listerIngredients} est publié
     *
     * @return le fournisseur de tools collecté par l'auto-configuration du serveur MCP
     */
    @Bean
    public ToolCallbackProvider ingredientToolCallbackProvider(IngredientTools ingredientTools) {
        return MethodToolCallbackProvider.builder().toolObjects(ingredientTools).build();
    }
}
