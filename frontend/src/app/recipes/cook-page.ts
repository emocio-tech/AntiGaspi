import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { LucideArrowLeft, LucideThumbsDown, LucideThumbsUp } from '@lucide/angular';
import { messageOf } from '../api-error';
import { Ingredient } from '../inventory/ingredient.model';
import { InventoryStore } from '../inventory/inventory-store';
import { formatQuantity } from '../inventory/urgency';
import { RecipeStore } from './recipe-store';
import { toInventoryQuantity } from './unit-conversion';

/** Avis donné à la recette cuisinée. */
type Rating = 'UP' | 'DOWN';

/** Ligne de confirmation : un ingrédient de la recette, avec son aliment en stock s'il existe encore. */
interface CookLine {
  ingredientId: number;
  name: string;
  stock: Ingredient | null;
  /** Quantité saisie (texte du champ). */
  quantity: string;
  /** Unité de la recette, renseignée seulement si elle ne se convertit pas dans celle du stock. */
  recipeUnit: string | null;
}

/** Confirmation « J'ai cuisiné cette recette » : retire du stock ce qui a été utilisé et enregistre la recette. */
@Component({
  selector: 'app-cook-page',
  imports: [RouterLink, LucideArrowLeft, LucideThumbsDown, LucideThumbsUp],
  templateUrl: './cook-page.html',
  styleUrl: './cook-page.css',
})
export class CookPage {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly recipes = inject(RecipeStore);
  private readonly inventory = inject(InventoryStore);

  /** Position de la recette dans la liste proposée (paramètre de route). */
  readonly id = input.required<string>();

  protected readonly formatQuantity = formatQuantity;
  protected readonly generated = computed(() => this.recipes.result(Number(this.id())));
  protected readonly rating = signal<Rating | null>(null);
  protected readonly comment = signal('');
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly inventoryLoading = this.inventory.loading;

  /** Quantités modifiées par l'utilisateur, par identifiant d'aliment. */
  private readonly edits = signal<Record<number, string>>({});

  protected readonly lines = computed<CookLine[]>(() => {
    const recipe = this.generated()?.recipe;
    if (!recipe) return [];
    const edits = this.edits();
    const items = this.inventory.items();
    return recipe.ingredients.flatMap((ingredient) => {
      if (ingredient.ingredientId === null) return [];
      const id = ingredient.ingredientId;
      const stock = items.find((item) => item.id === id) ?? null;
      const converted = stock ? toInventoryQuantity(ingredient.quantity, ingredient.unit, stock.unit) : null;
      const quantity = edits[id] ?? (converted === null ? '' : String(converted));
      const recipeUnit = stock && converted === null ? ingredient.unit : null;
      return [{ ingredientId: id, name: stock?.name ?? ingredient.name, stock, quantity, recipeUnit }];
    });
  });

  /** Vrai si une quantité saisie est négative. */
  protected readonly invalid = computed(() => this.lines().some((line) => line.stock && Number(line.quantity) < 0));

  protected setQuantity(id: number, value: string): void {
    this.edits.update((edits) => ({ ...edits, [id]: value }));
  }

  protected toggleRating(rating: Rating): void {
    this.rating.update((current) => (current === rating ? null : rating));
  }

  protected submit(): void {
    const result = this.generated();
    if (!result || this.sending() || this.invalid() || this.inventoryLoading()) return;
    const consumed = this.lines()
      .filter((line) => line.stock && line.quantity.trim() !== '' && Number(line.quantity) > 0)
      .map((line) => ({ ingredientId: line.ingredientId, quantity: Number(line.quantity) }));
    const comment = this.comment().trim();
    this.sending.set(true);
    this.error.set(null);
    this.http
      .post('/api/cooked-recipes', { recipe: result.recipe, rating: this.rating(), comment: comment || null, consumed })
      .subscribe({
        next: () => {
          this.router.navigateByUrl('/inventaire').then(() => this.inventory.reload());
        },
        error: (error: unknown) => {
          this.error.set(messageOf(error));
          this.sending.set(false);
        },
      });
  }
}
