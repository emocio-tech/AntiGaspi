package fr.antigaspi.ingredient.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientUpdate;
import fr.antigaspi.ingredient.model.NewIngredient;
import fr.antigaspi.ingredient.repository.MutableIngredientRepository;

/**
 * Ajout, suppression et modification d'aliments de l'inventaire. Chaque opération est atomique : tout est validé avant
 * d'écrire, et rien n'est écrit si un seul élément est refusé.
 */
@Service
public class IngredientEditService {

    private final MutableIngredientRepository repository;
    private final Clock clock;

    public IngredientEditService(MutableIngredientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @return les aliments créés, avec leur identifiant
     *
     * @throws IllegalArgumentException
     *             si la liste est vide, ou si un aliment a un nom vide, une catégorie ou une unité absente, ou une
     *             quantité absente ou non positive
     */
    @Transactional
    public List<Ingredient> add(List<NewIngredient> newIngredients) {
        requireNotEmpty(newIngredients, "Aucun aliment à ajouter");
        List<Ingredient> toSave = new ArrayList<>();
        for (NewIngredient newIngredient : newIngredients) {
            String name = requireName(newIngredient.name());
            if (newIngredient.category() == null) {
                throw new IllegalArgumentException("La catégorie de « %s » est absente".formatted(name));
            }
            if (newIngredient.unit() == null) {
                throw new IllegalArgumentException("L'unité de « %s » est absente".formatted(name));
            }
            requirePositive(name, newIngredient.quantity());
            toSave.add(new Ingredient(null, name, newIngredient.category(), newIngredient.quantity(),
                    newIngredient.unit(), newIngredient.expiryDate(), clock.instant()));
        }
        return repository.saveAll(toSave);
    }

    /**
     * @return les aliments supprimés
     *
     * @throws IllegalArgumentException
     *             si la liste est vide, si un identifiant est en double ou si un identifiant est inconnu
     */
    @Transactional
    public List<Ingredient> remove(List<Long> ids) {
        requireNotEmpty(ids, "Aucun identifiant à supprimer");
        Map<Long, Ingredient> existing = findExisting(ids);
        repository.deleteAllById(ids);
        return ids.stream().map(existing::get).toList();
    }

    /**
     * @return les aliments après modification
     *
     * @throws IllegalArgumentException
     *             si la liste est vide, si un identifiant est en double ou inconnu, si un champ renseigné est invalide
     *             (nom vide, quantité non positive) ou si une date est à la fois donnée et effacée
     */
    @Transactional
    public List<Ingredient> update(List<IngredientUpdate> updates) {
        requireNotEmpty(updates, "Aucune modification à appliquer");
        Map<Long, Ingredient> existing = findExisting(updates.stream().map(IngredientUpdate::id).toList());
        List<Ingredient> toSave = new ArrayList<>();
        for (IngredientUpdate update : updates) {
            toSave.add(apply(update, existing.get(update.id())));
        }
        return repository.saveAll(toSave);
    }

    private static Ingredient apply(IngredientUpdate update, Ingredient current) {
        if (update.clearExpiryDate() && update.expiryDate() != null) {
            throw new IllegalArgumentException(
                    "L'aliment %d : la date de péremption ne peut pas être à la fois donnée et effacée"
                            .formatted(update.id()));
        }
        String name = update.name() == null ? current.name() : requireName(update.name());
        if (update.quantity() != null) {
            requirePositive(name, update.quantity());
        }
        return new Ingredient(current.id(), name, update.category() == null ? current.category() : update.category(),
                update.quantity() == null ? current.quantity() : update.quantity(),
                update.unit() == null ? current.unit() : update.unit(), update.clearExpiryDate() ? null
                        : update.expiryDate() == null ? current.expiryDate() : update.expiryDate(),
                current.addedAt());
    }

    /** Les aliments portant ces identifiants, par identifiant ; refuse les doublons et les identifiants inconnus. */
    private Map<Long, Ingredient> findExisting(List<Long> ids) {
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Identifiant en double dans la demande : " + id);
            }
        }
        Map<Long, Ingredient> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ingredient::id, Function.identity()));
        List<Long> unknown = ids.stream().filter(id -> !existing.containsKey(id)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Aucun aliment avec l'identifiant : "
                    + unknown.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
        return existing;
    }

    private static void requireNotEmpty(List<?> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom d'un aliment est vide");
        }
        return name.strip();
    }

    private static void requirePositive(String name, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La quantité de « %s » doit être positive : %s".formatted(name, quantity));
        }
    }
}
