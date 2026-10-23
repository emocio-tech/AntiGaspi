package fr.antigaspi.ingredient.service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Aucun aliment ne porte l'identifiant demandé.
 */
public class IngredientNotFoundException extends IllegalArgumentException {

    public IngredientNotFoundException(List<Long> ids) {
        super((ids.size() == 1 ? "Aucun aliment avec l'identifiant " : "Aucun aliment avec les identifiants ")
                + ids.stream().map(String::valueOf).collect(Collectors.joining(", ")));
    }

    public IngredientNotFoundException(long id) {
        this(List.of(id));
    }
}
