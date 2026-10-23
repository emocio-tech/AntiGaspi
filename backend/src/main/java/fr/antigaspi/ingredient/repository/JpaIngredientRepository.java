package fr.antigaspi.ingredient.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Inventaire stocké dans la table {@code ingredient} (MySQL).
 */
@Repository
public class JpaIngredientRepository implements MutableIngredientRepository {

    private final SpringDataIngredientJpa jpa;

    public JpaIngredientRepository(SpringDataIngredientJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Ingredient> findAll() {
        return jpa.findAll(Sort.by("id")).stream().map(JpaIngredientRepository::toIngredient).toList();
    }

    @Override
    public List<Ingredient> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(JpaIngredientRepository::toIngredient).toList();
    }

    @Override
    public List<Ingredient> saveAll(List<Ingredient> ingredients) {
        return jpa.saveAll(ingredients.stream().map(JpaIngredientRepository::toEntity).toList()).stream()
                .map(JpaIngredientRepository::toIngredient).toList();
    }

    @Override
    public void deleteAllById(Collection<Long> ids) {
        jpa.deleteAllById(ids);
    }

    private static IngredientEntity toEntity(Ingredient ingredient) {
        IngredientEntity entity = new IngredientEntity();
        entity.id = ingredient.id();
        entity.name = ingredient.name();
        entity.category = ingredient.category();
        entity.quantity = ingredient.quantity();
        entity.unit = ingredient.unit();
        entity.expiryDate = ingredient.expiryDate();
        entity.addedAt = ingredient.addedAt();
        return entity;
    }

    private static Ingredient toIngredient(IngredientEntity entity) {
        return new Ingredient(entity.id, entity.name, entity.category, entity.quantity, entity.unit, entity.expiryDate,
                entity.addedAt);
    }
}
