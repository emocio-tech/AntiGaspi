package fr.antigaspi.recipe.repository;

import org.springframework.stereotype.Repository;

import fr.antigaspi.recipe.model.CookedRecipe;

/** Recettes cuisinées stockées en base de données (PostgreSQL). */
@Repository
public class JpaCookedRecipeRepository implements CookedRecipeRepository {

    private final CookedRecipeJpaRepository jpa;

    public JpaCookedRecipeRepository(CookedRecipeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CookedRecipe save(CookedRecipe recipe) {
        return jpa.save(new CookedRecipeEntity(recipe)).toCookedRecipe();
    }
}
