package fr.antigaspi.utensil.api;

/**
 * Corps de {@code POST /api/utensils}.
 *
 * @param name
 *            nom de l'ustensile à ajouter
 */
public record UtensilRequest(String name) {
}
