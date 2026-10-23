package fr.antigaspi.config;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.Resources;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.utensil.model.Utensil;
import tools.jackson.core.type.TypeReference;

/**
 * Amorçage de la base avec les données du projet ({@code data/ingredients.json} et {@code data/utensils.json}). Les
 * dates des aliments y sont en jours relatifs à aujourd'hui, calculés avec l'horloge de l'application.
 * <p>
 * Migration versionnée : exécutée une seule fois, donc pas de réamorçage si l'inventaire est vidé ensuite. Les
 * identifiants du fichier sont ignorés : la base les attribue.
 */
@Component
public class SeedDataMigration implements JavaMigration {

    /**
     * Entrée du fichier JSON : {@code expiresInDays} absent = pas de date, {@code addedDaysAgo} absent = maintenant.
     */
    private record Entry(String name, Category category, BigDecimal quantity, Unit unit, Integer expiresInDays,
            Integer addedDaysAgo) {
    }

    private final Clock clock;
    private final Resource ingredients;
    private final Resource utensils;

    public SeedDataMigration(Clock clock, @Value("classpath:data/ingredients.json") Resource ingredients,
            @Value("classpath:data/utensils.json") Resource utensils) {
        this.clock = clock;
        this.ingredients = ingredients;
        this.utensils = utensils;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("2");
    }

    @Override
    public String getDescription() {
        return "Amorcage des donnees";
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(Context context) throws Exception {
        List<Entry> entries = Resources.readList(ingredients, new TypeReference<List<Entry>>() {
        });
        try (PreparedStatement statement = context.getConnection().prepareStatement(
                "insert into ingredient (name, category, quantity, unit, expiry_date, added_at) values (?, ?, ?, ?, ?, ?)")) {
            for (Entry entry : entries) {
                statement.setString(1, entry.name());
                statement.setString(2, entry.category().name());
                statement.setBigDecimal(3, entry.quantity());
                statement.setString(4, entry.unit().name());
                statement.setObject(5,
                        entry.expiresInDays() == null ? null : LocalDate.now(clock).plusDays(entry.expiresInDays()));
                int addedDaysAgo = entry.addedDaysAgo() == null ? 0 : entry.addedDaysAgo();
                statement.setObject(6,
                        OffsetDateTime.ofInstant(clock.instant().minus(Duration.ofDays(addedDaysAgo)), ZoneOffset.UTC));
                statement.addBatch();
            }
            statement.executeBatch();
        }
        List<Utensil> utensilList = Resources.readList(utensils, new TypeReference<List<Utensil>>() {
        });
        try (PreparedStatement statement = context.getConnection()
                .prepareStatement("insert into utensil (name) values (?)")) {
            for (Utensil utensil : utensilList) {
                statement.setString(1, utensil.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
