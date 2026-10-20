package fr.antigaspi.recipe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Paramètres métier de la génération de recettes ({@code antigaspi.recipe.*}).
 *
 * @param maxIngredients
 *            nombre d'aliments les plus urgents envoyés au LLM quand aucun n'est choisi
 * @param defaultServings
 *            nombre de portions par défaut
 */
@ConfigurationProperties("antigaspi.recipe")
public record RecipeProperties(@DefaultValue("8") int maxIngredients, @DefaultValue("2") int defaultServings) {
}
