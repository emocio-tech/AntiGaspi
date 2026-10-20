package fr.antigaspi.recipe;

import java.util.List;

/**
 * Recette générée par le LLM.
 *
 * @param usedIngredients
 *            aliments de l'inventaire utilisés, avec les quantités consommées
 * @param missingIngredients
 *            ingrédients à acheter ou à prendre dans le placard
 * @param requiredEquipment
 *            équipement nécessaire (doit faire partie de l'équipement du profil)
 */
public record Recipe(String title, int servings, int totalMinutes, List<RecipeIngredient> usedIngredients,
        List<RecipeIngredient> missingIngredients, List<String> steps, List<String> requiredEquipment) {
}
