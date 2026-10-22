package fr.antigaspi.utensil.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.UtensilRepository;

/**
 * Consultation et modification des ustensiles disponibles.
 */
@Service
public class UtensilService {

    private final UtensilRepository repository;

    public UtensilService(UtensilRepository repository) {
        this.repository = repository;
    }

    /**
     * Tous les ustensiles disponibles, par ordre alphabétique.
     */
    public List<Utensil> listAll() {
        return repository.findAll().stream().sorted(Comparator.comparing(Utensil::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * @throws IllegalArgumentException
     *             si le nom est vide ou si l'ustensile existe déjà (sans tenir compte de la casse)
     */
    public Utensil add(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'ustensile est vide");
        }
        String cleanName = name.strip();
        repository.findAll().stream().filter(utensil -> utensil.name().equalsIgnoreCase(cleanName)).findFirst()
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "L'ustensile « %s » est déjà dans la liste".formatted(existing.name()));
                });
        return repository.save(cleanName);
    }

    /**
     * @return l'ustensile supprimé
     *
     * @throws IllegalArgumentException
     *             si aucun ustensile n'a cet identifiant
     */
    public Utensil remove(long id) {
        Utensil utensil = repository.findAll().stream().filter(candidate -> candidate.id() == id).findFirst()
                .orElseThrow(() -> new UtensilNotFoundException(id));
        repository.deleteById(id);
        return utensil;
    }
}
