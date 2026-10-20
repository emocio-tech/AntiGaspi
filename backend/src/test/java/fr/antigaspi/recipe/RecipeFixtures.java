package fr.antigaspi.recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.model.Urgency;

/** Données de test partagées par les tests de recette. */
public final class RecipeFixtures {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    public static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private RecipeFixtures() {
    }

    public static IngredientView view(long id, String name, String quantity, Unit unit, long daysLeft,
            boolean estimated) {
        Ingredient ingredient = new Ingredient(id, name, Category.LEGUMES, new BigDecimal(quantity), unit,
                estimated ? null : TODAY.plusDays(daysLeft), NOW);
        return new IngredientView(ingredient, TODAY.plusDays(daysLeft), daysLeft, Urgency.of(daysLeft), estimated);
    }
}
