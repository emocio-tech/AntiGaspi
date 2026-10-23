package fr.antigaspi.utensil.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Ustensiles stockés en base de données (PostgreSQL), convertis vers les records métier.
 */
@Repository
public class JpaUtensilRepository implements UtensilRepository {

    private final UtensilJpaRepository jpa;

    public JpaUtensilRepository(UtensilJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Utensil> findAll() {
        return jpa.findAll(Sort.by("id")).stream().map(UtensilEntity::toUtensil).toList();
    }

    @Override
    public Utensil save(String name) {
        return jpa.save(new UtensilEntity(name)).toUtensil();
    }

    @Override
    public void deleteById(long id) {
        jpa.deleteById(id);
    }
}
