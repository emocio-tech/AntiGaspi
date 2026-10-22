package fr.antigaspi.recipe.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;

@DataJpaTest
@Import(JpaCookedRecipeRepository.class)
class JpaCookedRecipeRepositoryTest {

    @Autowired
    private CookedRecipeRepository repository;

    private CookedRecipe save(String title, String cookedAt) {
        return repository.save(new CookedRecipe(null, new Recipe(title, 2, 30, List.of(), List.of(), List.of("Cuire.")),
                Instant.parse(cookedAt), Rating.UP, null));
    }

    @Test
    void findAllMostRecentFirst_ordreDecroissant() {
        save("Ancienne", "2026-10-17T12:00:00Z");
        save("Recente", "2026-10-19T12:00:00Z");
        save("MemeJourInseree", "2026-10-19T12:00:00Z");

        assertThat(repository.findAllMostRecentFirst()).extracting(item -> item.recipe().title())
                .containsExactly("MemeJourInseree", "Recente", "Ancienne");
    }

    @Test
    void findMostRecent_limite() {
        save("A", "2026-10-17T12:00:00Z");
        save("B", "2026-10-18T12:00:00Z");
        save("C", "2026-10-19T12:00:00Z");

        assertThat(repository.findMostRecent(2)).extracting(item -> item.recipe().title()).containsExactly("C", "B");
        assertThat(repository.findMostRecent(0)).isEmpty();
    }

    @Test
    void findById_presentOuVide() {
        CookedRecipe saved = save("Soupe", "2026-10-17T12:00:00Z");

        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findById(saved.id() + 1000)).isEmpty();
    }
}
