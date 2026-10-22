import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChefHat, LucideListChecks, LucidePlus, LucideTrash2, LucideUndo2 } from '@lucide/angular';
import { CategoryIcon } from './category-icon';
import { IngredientForm } from './ingredient-form';
import { InventoryStore } from './inventory-store';
import { formatQuantity, groupByUrgency, URGENCY_STYLE, urgencyLabel } from './urgency';
import { Ingredient, Urgency } from './ingredient.model';

/** Écran Inventaire : aliments regroupés par urgence de péremption. */
@Component({
  selector: 'app-inventory-page',
  imports: [CategoryIcon, IngredientForm, LucideChefHat, LucideListChecks, LucidePlus, LucideTrash2, LucideUndo2, RouterLink],
  templateUrl: './inventory-page.html',
  styleUrl: './inventory-page.css',
})
export class InventoryPage {
  protected readonly store = inject(InventoryStore);
  protected readonly style = URGENCY_STYLE;
  protected readonly formatQuantity = formatQuantity;
  protected readonly urgencyLabel = urgencyLabel;

  protected readonly groups = computed(() => groupByUrgency(this.store.items()));
  protected readonly urgentItems = computed(() =>
    this.store.items().filter((i) => i.urgency === 'EXPIRED_OR_TODAY' || i.urgency === 'SOON'),
  );
  protected readonly summary = computed(() => {
    const n = this.store.items().length;
    return n === 0 ? 'Aucun produit' : `${n} produit${n > 1 ? 's' : ''} · triés par urgence`;
  });

  /** « Plus tard » est replié tant que l'utilisateur ne le déplie pas. */
  private readonly laterOpen = signal(false);

  protected isOpen(urgency: Urgency): boolean {
    return urgency !== 'LATER' || this.laterOpen();
  }

  protected toggleLater(): void {
    this.laterOpen.update((open) => !open);
  }

  /** Formulaire ouvert : `null` fermé, sinon l'aliment à modifier (`null` dans `ingredient` pour un ajout). */
  protected readonly form = signal<{ ingredient: Ingredient | null } | null>(null);
  protected readonly selecting = signal(false);
  protected readonly selected = signal<number[]>([]);

  protected openForm(ingredient: Ingredient | null): void {
    this.store.actionError.set(null);
    this.form.set({ ingredient });
  }

  protected editByKey(event: Event, ingredient: Ingredient): void {
    // Espace sur la case à cocher du mode sélection ne doit pas être détourné
    if (event.target !== event.currentTarget || this.selecting()) return;
    event.preventDefault();
    this.openForm(ingredient);
  }

  protected edit(ingredient: Ingredient): void {
    if (!this.selecting()) this.openForm(ingredient);
  }

  protected toggleSelecting(): void {
    this.selecting.update((on) => !on);
    this.selected.set([]);
  }

  protected toggleSelected(id: number): void {
    this.selected.update((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
  }

  protected removeSelected(): void {
    this.store.scheduleRemoval(this.selected());
    this.toggleSelecting();
  }
}
