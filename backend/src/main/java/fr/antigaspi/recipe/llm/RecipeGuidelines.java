package fr.antigaspi.recipe.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.Resources;

/**
 * Consignes de génération d'une recette, lues dans {@code prompts/recipe-system.md} : le fichier entier est le prompt
 * système de la génération ({@link RecipePromptBuilder}), et ses sections sont les consignes données au LLM du chat.
 * <p>
 * Les sections sont repérées par leurs intitulés ({@value #RULES_START}, {@value #FORMAT_START}, {@value #FORMAT_END})
 * ; si l'un manque, la construction échoue plutôt que de donner des consignes incomplètes au LLM.
 */
@Component
public class RecipeGuidelines {

    private static final String RULES_START = "Méthode, dans cet ordre";
    private static final String FORMAT_START = "Format de la recette";
    private static final String FORMAT_END = "Avant de répondre";

    private final String systemPrompt;
    private final String rules;
    private final String format;

    public RecipeGuidelines(@Value("classpath:prompts/recipe-system.md") Resource source) {
        String text = Resources.readText(source);
        this.systemPrompt = text;
        int rulesStart = indexOf(text, RULES_START, 0);
        int formatStart = indexOf(text, FORMAT_START, rulesStart);
        int formatEnd = indexOf(text, FORMAT_END, formatStart);
        this.rules = text.substring(rulesStart, formatStart).strip();
        this.format = text.substring(formatStart, formatEnd).strip();
    }

    /** Le fichier entier : rôle, règles, format et vérification finale. */
    public String systemPrompt() {
        return systemPrompt;
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

}
