package fr.antigaspi.ingredient.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.ChatTools;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientDraft;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;

/**
 * Tools du chat qui modifient l'inventaire ; séparés d'{@link IngredientTools} pour ne pas être exposés au serveur MCP.
 */
@Component
public class IngredientEditTools implements ChatTools {

    private final IngredientService ingredientService;

    public IngredientEditTools(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Tool(name = "ajouterAliment", description = "Ajoute un aliment à l'inventaire de l'utilisateur.")
    public String ajouterAliment(@ToolParam(description = "Nom de l'aliment") String nom,
            @ToolParam(description = "Catégorie de l'aliment") Category categorie,
            @ToolParam(description = "Quantité, strictement positive") BigDecimal quantite,
            @ToolParam(description = "Unité de la quantité") Unit unite,
            @ToolParam(required = false, description = "Date de péremption au format AAAA-MM-JJ, si connue") LocalDate datePeremption) {
        try {
            Ingredient added = ingredientService
                    .add(new IngredientDraft(nom, categorie, quantite, unite, datePeremption)).ingredient();
            return "Ajouté : [%d] %s, %s %s%s".formatted(added.id(), added.name(), plain(added.quantity()),
                    added.unit().symbol(), added.expiryDate() == null ? "" : ", péremption " + added.expiryDate());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(name = "modifierQuantite", description = "Change la quantité d'un aliment de l'inventaire.")
    public String modifierQuantite(@ToolParam(description = "Identifiant de l'aliment") Long id,
            @ToolParam(description = "Nouvelle quantité, strictement positive") BigDecimal quantite) {
        if (id == null) {
            return "L'identifiant de l'aliment est obligatoire";
        }
        try {
            Ingredient updated = ingredientService.updateQuantity(id, quantite).ingredient();
            return "Quantité de [%d] %s : %s %s".formatted(updated.id(), updated.name(), plain(updated.quantity()),
                    updated.unit().symbol());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(name = "retirerAliments", description = "Retire un ou plusieurs aliments de l'inventaire.")
    public String retirerAliments(@ToolParam(description = "Identifiants des aliments à retirer") List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(java.util.Objects::isNull)) {
            return "Aucun aliment à retirer";
        }
        try {
            String names = ids.stream().map(id -> ingredientService.get(id).ingredient().name())
                    .collect(Collectors.joining(", "));
            ingredientService.removeAll(ids);
            return "Retirés : " + names;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static String plain(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }
}
