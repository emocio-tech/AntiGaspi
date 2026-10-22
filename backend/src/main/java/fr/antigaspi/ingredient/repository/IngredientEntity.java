package fr.antigaspi.ingredient.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ligne de la table {@code ingredient}, distincte du record métier {@link Ingredient} qui reste sans dépendance à JPA.
 */
@Entity
@Table(name = "ingredient")
class IngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected IngredientEntity() {
    }

    /**
     * Convertit la ligne en record métier, avec une quantité sans zéros inutiles (2.500 devient 2.5, 500.000 devient
     * 500).
     */
    Ingredient toIngredient() {
        BigDecimal normalized = quantity.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return new Ingredient(id, name, category, normalized, unit, expiryDate, addedAt);
    }
}
