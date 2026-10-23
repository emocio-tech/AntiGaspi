package fr.antigaspi.ingredient.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Unit;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ligne de la table {@code ingredient}. Détail d'implémentation du repository : le reste de l'application manipule le
 * record {@code Ingredient}.
 */
@Entity
@Table(name = "ingredient")
class IngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    Category category;

    BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    Unit unit;

    LocalDate expiryDate;

    Instant addedAt;

    protected IngredientEntity() {
    }
}
