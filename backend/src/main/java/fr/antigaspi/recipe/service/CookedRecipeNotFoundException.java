package fr.antigaspi.recipe.service;

/** Aucune recette cuisinée ne porte cet identifiant. */
public class CookedRecipeNotFoundException extends IllegalArgumentException {

    public CookedRecipeNotFoundException(long id) {
        super("Aucune recette cuisinée avec l'identifiant " + id);
    }
}
