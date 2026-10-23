import { generated } from '../recipes/recipe.fixtures';
import { CookedRecipe } from './history.model';

/** Recette cuisinée de test : gratin de courgettes, cuisiné le 19 octobre 2026, sans avis. */
export function cooked(overrides: Partial<CookedRecipe> = {}): CookedRecipe {
  return {
    id: 1,
    title: 'Gratin de courgettes',
    cookedAt: '2026-10-19T12:00:00Z',
    rating: null,
    comment: null,
    recipe: generated().recipe,
    ...overrides,
  };
}
