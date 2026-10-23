import { Recipe } from '../recipes/recipe.model';

/** Avis du cuisinier sur une recette : pouce en haut ou en bas. */
export type Rating = 'UP' | 'DOWN';

/** Recette cuisinée, telle que renvoyée par `GET /api/cooked-recipes`. */
export interface CookedRecipe {
  id: number;
  title: string;
  /** Instant ISO 8601 (UTC). */
  cookedAt: string;
  rating: Rating | null;
  comment: string | null;
  recipe: Recipe;
}

/** Corps de `PUT /api/cooked-recipes/{id}/feedback`. */
export interface FeedbackRequest {
  rating: Rating | null;
  comment: string | null;
}
