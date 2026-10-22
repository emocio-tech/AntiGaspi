package fr.antigaspi.ingredient.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Inventaire stocké en base de données (PostgreSQL), converti vers les records métier.
 */
@Repository
public class JpaIngredientRepository implements IngredientRepository {

    private final IngredientJpaRepository jpa;

    public JpaIngredientRepository(IngredientJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Ingredient> findAll() {
        return jpa.findAll().stream().map(IngredientEntity::toIngredient).toList();
    }

    @Override
    public Optional<Ingredient> findById(long id) {
        return jpa.findById(id).map(IngredientEntity::toIngredient);
    }

    @Override
    public Ingredient save(Ingredient ingredient) {
        IngredientEntity entity = ingredient.id() == null ? new IngredientEntity() : jpa.findById(ingredient.id())
                .orElseThrow(() -> new IllegalStateException("Aucun aliment avec l'identifiant " + ingredient.id()));
        entity.apply(ingredient);
        return jpa.save(entity).toIngredient();
    }

    @Override
    public void deleteAllById(Collection<Long> ids) {
        jpa.deleteAllById(ids);
    }
}
