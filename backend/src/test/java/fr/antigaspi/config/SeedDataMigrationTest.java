package fr.antigaspi.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.IngredientRepository;
import fr.antigaspi.ingredient.repository.JpaIngredientRepository;
import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.JpaUtensilRepository;
import fr.antigaspi.utensil.repository.UtensilRepository;

@DataJpaTest
@Import({ SeedDataMigration.class, JpaIngredientRepository.class, JpaUtensilRepository.class,
        SeedDataMigrationTest.FixedClock.class })
class SeedDataMigrationTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @TestConfiguration
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private IngredientRepository ingredients;

    @Autowired
    private UtensilRepository utensils;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    private Ingredient named(String name) {
        return ingredients.findAll().stream().filter(ingredient -> ingredient.name().equals(name)).findFirst()
                .orElseThrow();
    }

    @Test
    void baseVide_amorceLesAlimentsDuProjetEnJoursRelatifsALaDateDuJour() {
        assertThat(ingredients.findAll()).hasSize(28).extracting(Ingredient::id).doesNotContainNull()
                .doesNotHaveDuplicates();

        Ingredient courgettes = named("Courgettes");
        assertThat(courgettes.expiryDate()).isEqualTo(TODAY.plusDays(2));
        assertThat(courgettes.addedAt()).isEqualTo(NOW.minus(Duration.ofDays(4)));
        assertThat(courgettes.quantity()).isEqualTo(new BigDecimal("3"));
        assertThat(courgettes.unit()).isEqualTo(Unit.PIECE);
        // Sans expiresInDays : pas de date (estimée par catégorie) ; sans addedDaysAgo : ajouté maintenant
        assertThat(named("Tomates").expiryDate()).isNull();
        assertThat(named("Tomates").addedAt()).isEqualTo(NOW.minus(Duration.ofDays(3)));
        assertThat(named("Lait demi-écrémé").addedAt()).isEqualTo(NOW);
    }

    @Test
    void baseVide_amorceLesUstensilesDuProjet() {
        assertThat(utensils.findAll()).hasSize(14).extracting(Utensil::name).contains("Four", "Passoire");
    }

    @Test
    void migrationRelancee_neReamorcePas() {
        flyway.migrate();

        assertThat(ingredients.findAll()).hasSize(28);
        assertThat(utensils.findAll()).hasSize(14);
    }

    @Test
    void apresAmorcage_lesNouvellesLignesObtiennentUnIdentifiantLibre() {
        utensils.save("Wok");
        JdbcClient.create(dataSource)
                .sql("insert into ingredient (name, category, quantity, unit, added_at) values (?, ?, ?, ?, ?)")
                .params("Riz", "FECULENTS", BigDecimal.ONE, "KG", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
                .update();

        assertThat(utensils.findAll()).hasSize(15).extracting(Utensil::id).doesNotHaveDuplicates();
        assertThat(ingredients.findAll()).hasSize(29).extracting(Ingredient::id).doesNotHaveDuplicates();
    }
}
