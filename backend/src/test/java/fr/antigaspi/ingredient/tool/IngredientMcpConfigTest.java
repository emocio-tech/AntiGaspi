package fr.antigaspi.ingredient.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.service.IngredientService;

class IngredientMcpConfigTest {

    @Test
    void serveurMcp_exposeLesToolsDeConsultationEtDeModificationDeLInventaire() {
        IngredientService service = new IngredientService(new InMemoryIngredientRepository(),
                Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC));

        ToolCallbackProvider provider = new IngredientMcpConfig()
                .ingredientToolCallbackProvider(new IngredientTools(service), new IngredientMcpTools(service));

        assertThat(provider.getToolCallbacks()).extracting(ToolCallback::getToolDefinition)
                .extracting(definition -> definition.name()).containsExactlyInAnyOrder("listerIngredients",
                        "ajouterIngredients", "supprimerIngredients", "modifierIngredients");
    }
}
