package fr.antigaspi.utensil.repository;

import java.util.ArrayList;
import java.util.List;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Faux dépôt en mémoire pour les tests du service et du tool.
 */
public class InMemoryUtensilRepository implements UtensilRepository {

    private final List<Utensil> utensils;

    public InMemoryUtensilRepository(Utensil... initial) {
        this.utensils = new ArrayList<>(List.of(initial));
    }

    @Override
    public List<Utensil> findAll() {
        return List.copyOf(utensils);
    }

    @Override
    public Utensil save(String name) {
        long id = utensils.stream().mapToLong(Utensil::id).max().orElse(0L) + 1;
        Utensil utensil = new Utensil(id, name);
        utensils.add(utensil);
        return utensil;
    }

    @Override
    public void deleteById(long id) {
        utensils.removeIf(utensil -> utensil.id() == id);
    }
}
