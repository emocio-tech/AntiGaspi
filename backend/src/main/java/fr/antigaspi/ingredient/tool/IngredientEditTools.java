package fr.antigaspi.ingredient.tool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientUpdate;
import fr.antigaspi.ingredient.model.NewIngredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientEditService;

/**
 * Tools Spring AI de modification de l'inventaire (ajout, suppression, modification), exposés uniquement au serveur MCP
 * : cette classe n'implémente volontairement pas {@code ChatTools}, le chat avec le chef garde un accès en lecture
 * seule.
 * <p>
 * Chaque tool renvoie un message de confirmation, ou la raison du refus (rien n'est alors modifié), plutôt que de lever
 * une exception.
 */
@Component
public class IngredientEditTools {

    private final IngredientEditService editService;

    public IngredientEditTools(IngredientEditService editService) {
        this.editService = editService;
    }

    @Tool(name = "ajouterIngredients", description = """
            Ajoute un ou plusieurs aliments à l'inventaire de l'utilisateur (par exemple après des courses). \
            Renvoie la liste des aliments ajoutés avec leur identifiant, ou la raison du refus : \
            dans ce cas, aucun aliment n'est ajouté.""")
    public String ajouterIngredients(
            @ToolParam(description = "Aliments à ajouter (au moins un)") List<NouvelAliment> aliments) {
        return execute(() -> {
            List<Ingredient> added = editService
                    .add(orEmpty(aliments).stream().map(aliment -> new NewIngredient(aliment.nom(), aliment.categorie(),
                            aliment.quantite(), aliment.unite(), aliment.datePeremption())).toList());
            return "%d aliment(s) ajouté(s) : %s".formatted(added.size(), resume(added));
        });
    }

    @Tool(name = "supprimerIngredients", description = """
            Supprime un ou plusieurs aliments de l'inventaire (par exemple consommés ou jetés), à partir de leur \
            identifiant donné par listerIngredients. À n'appeler que sur demande explicite de l'utilisateur. \
            Renvoie la liste des aliments supprimés, ou la raison du refus (identifiant inconnu ou en double) : \
            dans ce cas, aucun aliment n'est supprimé.""")
    public String supprimerIngredients(
            @ToolParam(description = "Identifiants des aliments, tels que renvoyés par listerIngredients") List<Long> ids) {
        return execute(() -> {
            List<Ingredient> removed = editService.remove(ids);
            return "%d aliment(s) supprimé(s) : %s".formatted(removed.size(), resume(removed));
        });
    }

    @Tool(name = "modifierIngredients", description = """
            Modifie un ou plusieurs aliments de l'inventaire (quantité restante, date de péremption, nom…), à partir \
            de leur identifiant donné par listerIngredients. Seuls les champs renseignés changent. \
            Renvoie la liste des aliments modifiés, ou la raison du refus : dans ce cas, aucun aliment n'est modifié.""")
    public String modifierIngredients(
            @ToolParam(description = "Modifications à appliquer (au moins une)") List<ModificationAliment> modifications) {
        return execute(() -> {
            List<Ingredient> updated = editService.update(orEmpty(modifications).stream()
                    .map(modification -> new IngredientUpdate(modification.id(), modification.nom(),
                            modification.categorie(), modification.quantite(), modification.unite(),
                            modification.datePeremption(), Boolean.TRUE.equals(modification.effacerDatePeremption())))
                    .toList());
            return "%d aliment(s) modifié(s) : %s".formatted(updated.size(), resume(updated));
        });
    }

    /** Renvoie la raison du refus (message du service) plutôt que de lever : le LLM peut la lire et corriger. */
    private static String execute(Supplier<String> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String resume(List<Ingredient> ingredients) {
        return ingredients.stream()
                .map(ingredient -> "%s (identifiant %d)".formatted(ingredient.name(), ingredient.id()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Aliment à ajouter, tel que le LLM le fournit.
     *
     * @param datePeremption
     *            date de péremption (AAAA-MM-JJ) ; absente, elle est estimée d'après la catégorie
     */
    public record NouvelAliment(String nom, Category categorie, BigDecimal quantite, Unit unite,
            @JsonProperty(required = false) LocalDate datePeremption) {
    }

    /**
     * Modification d'un aliment, telle que le LLM la fournit : seuls les champs renseignés changent.
     *
     * @param effacerDatePeremption
     *            {@code true} pour retirer la date de péremption (elle sera de nouveau estimée d'après la catégorie)
     */
    public record ModificationAliment(long id, @JsonProperty(required = false) String nom,
            @JsonProperty(required = false) Category categorie, @JsonProperty(required = false) BigDecimal quantite,
            @JsonProperty(required = false) Unit unite, @JsonProperty(required = false) LocalDate datePeremption,
            @JsonProperty(required = false) Boolean effacerDatePeremption) {
    }
}
