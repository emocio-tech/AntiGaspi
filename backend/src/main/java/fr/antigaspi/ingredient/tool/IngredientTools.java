package fr.antigaspi.ingredient.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.service.IngredientService;

/**
 * Tools Spring AI donnant au LLM l'accès à l'inventaire, à la demande, plutôt que de tout mettre dans le prompt.
 * <p>
 * Le LLM reçoit la liste en JSON. Une erreur du service (filtre invalide) lui est renvoyée sous forme de message.
 */
@Component
public class IngredientTools {

    private static final Logger log = LoggerFactory.getLogger(IngredientTools.class);

    private final IngredientService ingredientService;

    public IngredientTools(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Tool(name = "listerIngredients", description = """
            Liste les aliments de l'inventaire de l'utilisateur, du plus urgent au moins urgent à consommer. \
            Pour chaque aliment : identifiant, nom, catégorie, quantité et unité, date de péremption, jours restants \
            (négatif si périmé), date estimée ou non, urgence. \
            À appeler avant de proposer une recette ou de répondre à une question sur le stock ; \
            sans paramètre, renvoie tout l'inventaire.""")
    public List<Aliment> listerIngredients(
            @ToolParam(required = false, description = "Uniquement les aliments de cette catégorie") Category categorie,
            @ToolParam(required = false, description = "Uniquement les aliments périmés ou à consommer sous ce nombre de jours (0 = périmés ou à consommer aujourd'hui)") Integer joursMax,
            @ToolParam(required = false, description = "Nombre maximal d'aliments renvoyés, les plus urgents d'abord") Integer limite) {
        List<Aliment> aliments = ingredientService.search(categorie, joursMax, limite).stream()
                .map(IngredientTools::toAliment).toList();
        log.info("Tool listerIngredients (catégorie={}, joursMax={}, limite={}) : {} aliment(s)", categorie, joursMax,
                limite, aliments.size());
        return aliments;
    }

    private static Aliment toAliment(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        return new Aliment(ingredient.id(), ingredient.name(), ingredient.category().label(), ingredient.quantity(),
                ingredient.unit().symbol(), view.effectiveExpiryDate(), view.daysLeft(), view.estimated(),
                view.urgency().emoji());
    }

    /**
     * Aliment tel que le LLM le reçoit.
     *
     * @param dateEstimee
     *            {@code true} si {@code datePeremption} est estimée d'après la catégorie
     * @param urgence
     *            indicateur coloré (🔴 🟠 🟡 🟢)
     */
    public record Aliment(long id, String nom, String categorie, BigDecimal quantite, String unite,
            LocalDate datePeremption, long joursRestants, boolean dateEstimee, String urgence) {
    }
}
