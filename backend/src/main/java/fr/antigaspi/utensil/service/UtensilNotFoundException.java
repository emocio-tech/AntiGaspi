package fr.antigaspi.utensil.service;

/**
 * Aucun ustensile ne porte l'identifiant demandé.
 */
public class UtensilNotFoundException extends IllegalArgumentException {

    public UtensilNotFoundException(long id) {
        super("Aucun ustensile avec l'identifiant " + id);
    }
}
