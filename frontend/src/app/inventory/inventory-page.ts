import { Component, computed, inject, signal } from '@angular/core';
import { LucideChefHat } from '@lucide/angular';
import { CategoryIcon } from './category-icon';
import { InventoryStore } from './inventory-store';
import { formatQuantity, groupByUrgency, URGENCY_STYLE, urgencyLabel } from './urgency';
import { Urgency } from './ingredient.model';

/** Écran Inventaire : aliments regroupés par urgence de péremption. */
@Component({
  selector: 'app-inventory-page',
  imports: [CategoryIcon, LucideChefHat],
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
}
