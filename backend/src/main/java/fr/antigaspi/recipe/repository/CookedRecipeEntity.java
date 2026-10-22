package fr.antigaspi.recipe.repository;

import java.time.Instant;

import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tools.jackson.databind.json.JsonMapper;

/** Ligne de la table {@code cooked_recipe} ; la recette y est stockée en JSON dans {@code content}. */
@Entity
@Table(name = "cooked_recipe")
class CookedRecipeEntity {

    private static final int CONTENT_MAX_LENGTH = 20000;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "cooked_at", nullable = false)
    private Instant cookedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Rating rating;

    @Column(length = 1000)
    private String comment;

    CookedRecipeEntity() {
    }

    /** Ligne à insérer pour cette recette cuisinée ; la recette est sérialisée en JSON. */
    CookedRecipeEntity(CookedRecipe recipe) {
        this.title = recipe.recipe().title();
        this.content = MAPPER.writeValueAsString(recipe.recipe());
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("La recette est trop longue pour être enregistrée");
        }
        this.cookedAt = recipe.cookedAt();
        this.rating = recipe.rating();
        this.comment = recipe.comment();
    }

    /** Remplace l'avis et le commentaire ; la recette et la date sont inchangées. */
    void updateFeedback(Rating rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    /** Recette cuisinée correspondant à cette ligne ; la recette est relue depuis le JSON. */
    CookedRecipe toCookedRecipe() {
        return new CookedRecipe(id, MAPPER.readValue(content, Recipe.class), cookedAt, rating, comment);
    }
}
