import { GeneratedRecipe } from './recipe.model';

/** Réponse de génération de test : un gratin avec un aliment de l'inventaire (id 2) et du sel du placard. */
export function generated(overrides: Partial<GeneratedRecipe['recipe']> = {}, issues: GeneratedRecipe['issues'] = []): GeneratedRecipe {
  return {
    recipe: {
      title: 'Gratin de courgettes',
      servings: 2,
      preparationMinutes: 35,
      utensils: ['Four', 'Poêle'],
      ingredients: [
        { ingredientId: 2, name: 'Courgettes', quantity: 2.5, unit: 'kg' },
        { ingredientId: null, name: 'Sel', quantity: 1, unit: 'pincée' },
      ],
      steps: ['Couper les courgettes.', 'Enfourner 25 minutes.'],
      ...overrides,
    },
    issues,
  };
}
