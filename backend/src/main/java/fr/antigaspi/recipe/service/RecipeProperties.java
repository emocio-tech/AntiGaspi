package fr.antigaspi.recipe.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Paramètres métier de la génération de recettes ({@code antigaspi.recipe.*}).
 *
 * @param maxIngredients
 *            nombre d'aliments les plus urgents présentés au LLM comme prioritaires quand aucun n'est choisi ; le reste
 *            de l'inventaire lui est aussi fourni, comme complément facultatif
 * @param defaultServings
 *            nombre de portions par défaut
 */
@ConfigurationProperties("antigaspi.recipe")
public record RecipeProperties(@DefaultValue("8") int maxIngredients, @DefaultValue("2") int defaultServings) {
}
