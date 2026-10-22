import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { messageOf } from '../api-error';
import { InventoryStore } from '../inventory/inventory-store';
import { Ingredient } from '../inventory/ingredient.model';
import { GeneratedRecipe, Recipe, RecipeSlot, TimeOption } from './recipe.model';

export const MIN_SERVINGS = 1;
export const MAX_SERVINGS = 12;
/** Nombre de recettes demandées en parallèle à chaque génération. */
export const RECIPE_COUNT = 3;
/** Nombre maximal d'aliments urgents présélectionnés. */
export const MAX_PRESELECTED = 5;

export const TIME_OPTIONS: readonly TimeOption[] = [
  { label: '15 min', value: 15 },
  { label: '30 min', value: 30 },
  { label: '45 min', value: 45 },
  { label: '1 h +', value: null },
];

const isUrgent = (ingredient: Ingredient) => ingredient.urgency === 'EXPIRED_OR_TODAY' || ingredient.urgency === 'SOON';

/**
 * Réglages de la génération et recettes reçues. Chaque génération lance {@link RECIPE_COUNT} appels en parallèle ;
 * une recette en échec n'empêche pas les autres. Les recettes ne sont pas conservées au-delà de la session.
 */
@Injectable({ providedIn: 'root' })
export class RecipeStore {
  private readonly http = inject(HttpClient);
  private readonly inventory = inject(InventoryStore);

  /** Choix de l'utilisateur, `null` tant qu'il n'a rien touché. */
  private readonly userSelection = signal<number[] | null>(null);
  /** Numéro de la génération en cours : les réponses d'une génération remplacée sont ignorées. */
  private generation = 0;

  readonly servings = signal(2);
  readonly maxMinutes = signal<number | null>(null);
  /** Identifiants des aliments à inclure : les plus urgents tant que l'utilisateur n'a rien choisi. */
  readonly selectedIds = computed<number[]>(
    () =>
      this.userSelection() ??
      this.inventory
        .items()
        .filter(isUrgent)
        .map((ingredient) => ingredient.id)
        .slice(0, MAX_PRESELECTED),
  );
  readonly slots = signal<RecipeSlot[]>([]);
  readonly generating = computed(() => this.slots().some((slot) => slot.status === 'loading'));

  changeServings(delta: number): void {
    this.servings.update((servings) => Math.min(MAX_SERVINGS, Math.max(MIN_SERVINGS, servings + delta)));
  }

  setMaxMinutes(minutes: number | null): void {
    this.maxMinutes.set(minutes);
  }

  toggleIngredient(id: number): void {
    const selected = this.selectedIds();
    this.userSelection.set(selected.includes(id) ? selected.filter((other) => other !== id) : [...selected, id]);
  }

  generate(): void {
    const run = ++this.generation;
    const body = { ingredientIds: this.selectedIds(), servings: this.servings(), maxMinutes: this.maxMinutes() };
    this.slots.set(Array.from({ length: RECIPE_COUNT }, () => ({ status: 'loading' }) as RecipeSlot));
    for (let index = 0; index < RECIPE_COUNT; index++) {
      this.http.post<GeneratedRecipe>('/api/recipes', body).subscribe({
        next: (result) => this.setSlot(run, index, { status: 'done', result }),
        error: (error: unknown) => this.setSlot(run, index, { status: 'error', message: messageOf(error) }),
      });
    }
  }

  /** La recette reçue à cette position, `null` si elle est absente, en cours ou en échec. */
  result(index: number): GeneratedRecipe | null {
    const slot = this.slots()[index];
    return slot?.status === 'done' ? slot.result : null;
  }

  /** Nombre d'aliments urgents (aujourd'hui ou sous 3 jours) utilisés par la recette. */
  urgentSavedCount(recipe: Recipe): number {
    const urgentIds = new Set(this.inventory.items().filter(isUrgent).map((ingredient) => ingredient.id));
    return recipe.ingredients.filter((i) => i.ingredientId !== null && urgentIds.has(i.ingredientId)).length;
  }

  private setSlot(run: number, index: number, slot: RecipeSlot): void {
    if (run !== this.generation) return;
    this.slots.update((slots) => slots.map((current, i) => (i === index ? slot : current)));
  }
}
