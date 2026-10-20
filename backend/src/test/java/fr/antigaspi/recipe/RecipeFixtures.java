package fr.antigaspi.recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import fr.antigaspi.ingredient.Category;
import fr.antigaspi.ingredient.Ingredient;
import fr.antigaspi.ingredient.IngredientView;
import fr.antigaspi.ingredient.Unit;
import fr.antigaspi.ingredient.Urgency;

/** Données de test partagées par les tests de recette. */
final class RecipeFixtures {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private RecipeFixtures() {
    }

    static IngredientView view(long id, String name, String quantity, Unit unit, long daysLeft, boolean estimated) {
        Ingredient ingredient = new Ingredient(id, name, Category.LEGUMES, new BigDecimal(quantity), unit,
                estimated ? null : TODAY.plusDays(daysLeft), NOW);
        return new IngredientView(ingredient, TODAY.plusDays(daysLeft), daysLeft, Urgency.of(daysLeft), estimated);
    }
}
