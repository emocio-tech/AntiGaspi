package fr.antigaspi.recipe.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import fr.antigaspi.recipe.model.CookedRecipe;

/**
 * Faux dépôt en mémoire pour les tests du service et de la génération de recettes.
 */
public class InMemoryCookedRecipeRepository implements CookedRecipeRepository {

    private static final Comparator<CookedRecipe> MOST_RECENT_FIRST = Comparator
            .comparing(CookedRecipe::cookedAt, Comparator.reverseOrder())
            .thenComparing(CookedRecipe::id, Comparator.reverseOrder());

    private final List<CookedRecipe> recipes;

    public InMemoryCookedRecipeRepository(CookedRecipe... initial) {
        this.recipes = new ArrayList<>(List.of(initial));
    }

    @Override
    public synchronized CookedRecipe save(CookedRecipe recipe) {
        if (recipe.id() == null) {
            long id = recipes.stream().mapToLong(CookedRecipe::id).max().orElse(0L) + 1;
            CookedRecipe inserted = new CookedRecipe(id, recipe.recipe(), recipe.cookedAt(), recipe.rating(),
                    recipe.comment());
            recipes.add(inserted);
            return inserted;
        }
        recipes.replaceAll(existing -> existing.id().equals(recipe.id()) ? recipe : existing);
        return recipe;
    }

    @Override
    public synchronized Optional<CookedRecipe> findById(long id) {
        return recipes.stream().filter(recipe -> recipe.id() != null && recipe.id() == id).findFirst();
    }

    @Override
    public synchronized List<CookedRecipe> findAllMostRecentFirst() {
        return recipes.stream().sorted(MOST_RECENT_FIRST).toList();
    }

    @Override
    public synchronized List<CookedRecipe> findMostRecent(int limit) {
        return recipes.stream().sorted(MOST_RECENT_FIRST).limit(limit).toList();
    }
}
