package fr.antigaspi.recipe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import fr.antigaspi.recipe.model.CookedRecipe;

/** Recettes cuisinées stockées en base de données (PostgreSQL). */
@Repository
public class JpaCookedRecipeRepository implements CookedRecipeRepository {

    private final CookedRecipeJpaRepository jpa;

    public JpaCookedRecipeRepository(CookedRecipeJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Insère la recette si elle n'a pas d'identifiant ; sinon met à jour l'avis et le commentaire de la ligne. */
    @Override
    public CookedRecipe save(CookedRecipe recipe) {
        if (recipe.id() == null) {
            return jpa.save(new CookedRecipeEntity(recipe)).toCookedRecipe();
        }
        CookedRecipeEntity entity = jpa.findById(recipe.id()).orElseThrow(
                () -> new IllegalArgumentException("Aucune recette cuisinée avec l'identifiant " + recipe.id()));
        entity.updateFeedback(recipe.rating(), recipe.comment());
        return jpa.save(entity).toCookedRecipe();
    }

    @Override
    public Optional<CookedRecipe> findById(long id) {
        return jpa.findById(id).map(CookedRecipeEntity::toCookedRecipe);
    }

    @Override
    public List<CookedRecipe> findAllMostRecentFirst() {
        return jpa.findAllByOrderByCookedAtDescIdDesc().stream().map(CookedRecipeEntity::toCookedRecipe).toList();
    }

    @Override
    public List<CookedRecipe> findMostRecent(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jpa.findAllByOrderByCookedAtDescIdDesc(PageRequest.of(0, limit)).stream()
                .map(CookedRecipeEntity::toCookedRecipe).toList();
    }
}
