package fr.antigaspi.recipe.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Consignes de génération d'une recette, extraites du prompt système {@code prompts/recipe-system.md} : le fichier
 * reste la source unique, utilisée aussi par le prompt de génération.
 * <p>
 * Les sections sont repérées par leurs intitulés ({@value #RULES_START}, {@value #FORMAT_START}, {@value #FORMAT_END})
 * ; si l'un manque, la construction échoue plutôt que de donner des consignes incomplètes au LLM.
 */
@Component
public class RecipeGuidelines {

    private static final String RULES_START = "Méthode, dans cet ordre";
    private static final String FORMAT_START = "Format de la recette";
    private static final String FORMAT_END = "Avant de répondre";

    private final String rules;
    private final String format;

    public RecipeGuidelines(@Value("classpath:prompts/recipe-system.md") Resource source) {
        String text = read(source);
        int rulesStart = indexOf(text, RULES_START, 0);
        int formatStart = indexOf(text, FORMAT_START, rulesStart);
        int formatEnd = indexOf(text, FORMAT_END, formatStart);
        this.rules = text.substring(rulesStart, formatStart).strip();
        this.format = text.substring(formatStart, formatEnd).strip();
    }

    /** Méthode de choix des aliments et contraintes à respecter. */
    public String rules() {
        return rules;
    }

    /** Format JSON standardisé de la recette. */
    public String format() {
        return format;
    }

    /** Règles puis format. */
    public String all() {
        return rules + "\n\n" + format;
    }

    private static int indexOf(String text, String marker, int from) {
        int index = text.indexOf(marker, from);
        if (index < 0) {
            throw new IllegalStateException(
                    "Repère « %s » introuvable dans prompts/recipe-system.md (ou mal placé)".formatted(marker));
        }
        return index;
    }

    private static String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt système introuvable : " + resource.getDescription(), e);
        }
    }
}
