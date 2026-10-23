package fr.antigaspi.ingredient.repository;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Au démarrage, remplit la table {@code ingredient} avec les données initiales de {@code data/ingredients.json} si elle
 * est vide. Une table déjà remplie n'est jamais touchée ; un inventaire entièrement vidé est donc rempli de nouveau au
 * prochain démarrage.
 * <p>
 * S'exécute avant les autres runners (démos en terminal) pour qu'ils trouvent l'inventaire rempli.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IngredientSeeder implements ApplicationRunner {

    private final MutableIngredientRepository repository;
    private final Resource initialData;
    private final Clock clock;

    public IngredientSeeder(MutableIngredientRepository repository,
            @Value("classpath:data/ingredients.json") Resource initialData, Clock clock) {
        this.repository = repository;
        this.initialData = initialData;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        // Les identifiants du fichier sont ignorés : la base attribue les siens (1, 2, 3… sur une table neuve)
        List<Ingredient> initial = new JsonIngredientRepository(initialData, clock)
                .findAll().stream().map(ingredient -> new Ingredient(null, ingredient.name(), ingredient.category(),
                        ingredient.quantity(), ingredient.unit(), ingredient.expiryDate(), ingredient.addedAt()))
                .toList();
        repository.saveAll(initial);
    }
}
