package fr.antigaspi.utensil.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.ChatTools;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.service.UtensilService;

/**
 * Tools Spring AI permettant au LLM de consulter et de modifier la liste des ustensiles.
 */
@Component
public class UtensilTools implements ChatTools {

    private final UtensilService utensilService;

    public UtensilTools(UtensilService utensilService) {
        this.utensilService = utensilService;
    }

    @Tool(name = "listUtensils", description = """
            Liste les ustensiles de cuisine disponibles chez l'utilisateur, par ordre alphabétique, avec leur \
            identifiant. À appeler avant de supprimer un ustensile, pour retrouver son identifiant.""")
    public List<Utensil> listUtensils() {
        return utensilService.listAll();
    }

    @Tool(name = "addUtensil", description = """
            Ajoute un ustensile de cuisine à la liste de l'utilisateur (par exemple après un achat). \
            Renvoie une confirmation avec l'identifiant attribué, ou la raison du refus (nom vide, déjà présent).""")
    public String addUtensil(@ToolParam(description = "Nom de l'ustensile, par exemple Mixeur plongeant") String name) {
        try {
            Utensil added = utensilService.add(name);
            return "Ustensile ajouté : %s (identifiant %d)".formatted(added.name(), added.id());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Tool(name = "removeUtensil", description = """
            Supprime un ustensile de la liste de l'utilisateur (par exemple s'il est cassé ou jeté), à partir de son \
            identifiant donné par listUtensils. À n'appeler que sur demande explicite de l'utilisateur. \
            Renvoie une confirmation, ou la raison du refus (identifiant inconnu).""")
    public String removeUtensil(
            @ToolParam(description = "Identifiant de l'ustensile, tel que renvoyé par listUtensils") long id) {
        try {
            Utensil removed = utensilService.remove(id);
            return "Ustensile supprimé : %s (identifiant %d)".formatted(removed.name(), removed.id());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
