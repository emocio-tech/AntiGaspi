package fr.antigaspi.ingredient.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;

/**
 * Repository JPA sur H2 (mode MySQL) avec le vrai schéma : la migration Flyway {@code V1__ingredient.sql} est
 * appliquée, ce qui la vérifie aussi.
 * <p>
 * Base propre à ce test : celle des autres tests ({@code antigaspi}) est partagée entre contextes Spring et contient
 * les données initiales insérées par {@code IngredientSeeder}.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:jpa-ingredient;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaIngredientRepository.class)
class JpaIngredientRepositoryTest {

    private static final Instant ADDED_AT = Instant.parse("2026-10-20T10:15:30.123456Z");

    @Autowired
    private JpaIngredientRepository repository;

    private static Ingredient nouveau(String name, LocalDate expiryDate) {
        return new Ingredient(null, name, Category.PRODUITS_LAITIERS, new BigDecimal("2.5"), Unit.KG, expiryDate,
                ADDED_AT);
    }

    @Test
    void saveAll_sansIdentifiant_attribueLesIdentifiantsEtConserveTousLesChamps() {
        List<Ingredient> saved = repository
                .saveAll(List.of(nouveau("Yaourts", LocalDate.of(2026, 11, 2)), nouveau("Riz", null)));

        assertThat(saved).extracting(Ingredient::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(saved).extracting(Ingredient::name).containsExactly("Yaourts", "Riz");
        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(saved);
        Ingredient yaourts = repository.findAllById(List.of(saved.get(0).id())).get(0);
        assertThat(yaourts).isEqualTo(new Ingredient(saved.get(0).id(), "Yaourts", Category.PRODUITS_LAITIERS,
                new BigDecimal("2.5"), Unit.KG, LocalDate.of(2026, 11, 2), ADDED_AT));
        assertThat(repository.findAllById(List.of(saved.get(1).id())).get(0).expiryDate()).isNull();
    }

    @Test
    void saveAll_avecIdentifiantConnu_remplaceLAliment() {
        Ingredient saved = repository.saveAll(List.of(nouveau("Yaourts", null))).get(0);

        repository.saveAll(List.of(new Ingredient(saved.id(), "Skyr", Category.OEUFS, BigDecimal.ONE, Unit.G,
                LocalDate.of(2026, 12, 1), ADDED_AT)));

        assertThat(repository.findAll()).containsExactly(new Ingredient(saved.id(), "Skyr", Category.OEUFS,
                BigDecimal.ONE, Unit.G, LocalDate.of(2026, 12, 1), ADDED_AT));
    }

    @Test
    void findAllById_identifiantInconnu_estAbsentDuResultat() {
        Ingredient saved = repository.saveAll(List.of(nouveau("Yaourts", null))).get(0);

        assertThat(repository.findAllById(List.of(saved.id(), 9999L))).containsExactly(saved);
    }

    @Test
    void deleteAllById_supprimeLesAlimentsDemandesEtLeurSeuls() {
        List<Ingredient> saved = repository
                .saveAll(List.of(nouveau("Yaourts", null), nouveau("Riz", null), nouveau("Lait", null)));

        repository.deleteAllById(List.of(saved.get(0).id(), saved.get(2).id()));

        assertThat(repository.findAll()).containsExactly(saved.get(1));
    }
}
