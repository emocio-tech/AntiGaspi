package fr.antigaspi.utensil.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.UtensilRepository;

/**
 * Consultation des ustensiles disponibles.
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
}
