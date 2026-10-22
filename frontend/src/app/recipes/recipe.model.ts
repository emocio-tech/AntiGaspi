/** Ingrédient d'une recette ; `ingredientId` est `null` s'il ne vient pas de l'inventaire (placard, courses). */
export interface RecipeIngredient {
  ingredientId: number | null;
  name: string;
  quantity: number;
  unit: string;
}

/** Recette au format standardisé (`docs/format-recette.md`). */
export interface Recipe {
  title: string;
  servings: number;
  preparationMinutes: number;
  utensils: string[];
  ingredients: RecipeIngredient[];
  steps: string[];
}

/** Anomalie détectée par le garde-fou du backend. */
export interface RecipeIssue {
  type: string;
  label: string;
  message: string;
}

/** Réponse de `POST /api/recipes`. */
export interface GeneratedRecipe {
  recipe: Recipe;
  issues: RecipeIssue[];
}

/** Une des recettes demandées : en cours, reçue ou en échec. */
export type RecipeSlot =
  | { status: 'loading' }
  | { status: 'done'; result: GeneratedRecipe }
  | { status: 'error'; message: string };

/** Choix de temps maximum ; `null` : aucune contrainte. */
export interface TimeOption {
  label: string;
  value: number | null;
}
