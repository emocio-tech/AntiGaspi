package fr.antigaspi.utensil.repository;

import fr.antigaspi.utensil.model.Utensil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ligne de la table {@code utensil}, distincte du record métier {@link Utensil} qui reste sans dépendance à JPA.
 */
@Entity
@Table(name = "utensil")
class UtensilEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected UtensilEntity() {
    }

    UtensilEntity(String name) {
        this.name = name;
    }

    Utensil toUtensil() {
        return new Utensil(id, name);
    }
}
