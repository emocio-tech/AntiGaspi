package fr.antigaspi.ingredient.repository;

import java.util.List;

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
}
