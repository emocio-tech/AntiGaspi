package fr.antigaspi.ingredient.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;

@DataJpaTest
@Import(JpaIngredientRepository.class)
class JpaIngredientRepositoryTest {

    private static final Instant ADDED_AT = Instant.parse("2026-10-17T10:00:00Z");

    @Autowired
    private IngredientRepository repository;

    @Autowired
    private DataSource dataSource;

    private void insert(String name, String category, String quantity, String unit, LocalDate expiryDate) {
        JdbcClient.create(dataSource)
                .sql("insert into ingredient (name, category, quantity, unit, expiry_date, added_at)"
                        + " values (?, ?, ?, ?, ?, ?)")
                .params(name, category, new BigDecimal(quantity), unit, expiryDate,
                        OffsetDateTime.ofInstant(ADDED_AT, ZoneOffset.UTC))
                .update();
    }

    @Test
    void findAll_relitTousLesChampsDuModele() {
        insert("Crème fraîche", "PRODUITS_LAITIERS", "2.5", "KG", LocalDate.of(2026, 10, 22));

        Ingredient ingredient = repository.findAll().getFirst();

        assertThat(ingredient.id()).isNotNull();
        assertThat(ingredient.name()).isEqualTo("Crème fraîche");
        assertThat(ingredient.category()).isEqualTo(Category.PRODUITS_LAITIERS);
        assertThat(ingredient.unit()).isEqualTo(Unit.KG);
        assertThat(ingredient.expiryDate()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(ingredient.addedAt()).isEqualTo(ADDED_AT);
        // Échelle normalisée : 2.5 et non 2.500 (numeric(10,3) en base), pour le front, le LLM et equals
        assertThat(ingredient.quantity()).isEqualTo(new BigDecimal("2.5"));
    }

    @Test
    void datePeremptionAbsente_resteNulle_etQuantiteEntiereSansNotationScientifique() {
        insert("Tomates", "LEGUMES", "500", "G", null);

        Ingredient ingredient = repository.findAll().getFirst();

        assertThat(ingredient.expiryDate()).isNull();
        assertThat(ingredient.quantity()).isEqualTo(new BigDecimal("500"));
    }
}
