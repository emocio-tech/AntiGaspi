import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CookedRecipe, Rating } from './history.model';
import { HistoryStore } from './history-store';

/** Historique des recettes cuisinées, avec avis modifiable. */
@Component({
  selector: 'app-history-page',
  imports: [RouterLink],
  templateUrl: './history-page.html',
  styleUrl: './history-page.css',
})
export class HistoryPage {
  protected readonly store = inject(HistoryStore);

  private readonly dateFormat = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeZone: 'UTC' });

  /** Id des recettes dont le détail est déplié. */
  protected readonly opened = signal<number[]>([]);
  /** Id de la recette en cours d'édition, `null` si aucune. */
  protected readonly editingId = signal<number | null>(null);
  protected readonly draftRating = signal<Rating | null>(null);
  protected readonly draftComment = signal('');
  protected readonly saving = signal(false);

  protected date(item: CookedRecipe): string {
    return this.dateFormat.format(new Date(item.cookedAt));
  }

  protected toggle(id: number): void {
    this.opened.update((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
  }

  protected edit(item: CookedRecipe): void {
    this.store.actionError.set(null);
    this.draftRating.set(item.rating);
    this.draftComment.set(item.comment ?? '');
    this.editingId.set(item.id);
  }

  protected cancel(): void {
    this.store.actionError.set(null);
    this.editingId.set(null);
  }

  protected rate(rating: Rating): void {
    this.draftRating.update((current) => (current === rating ? null : rating));
  }

  protected async save(id: number): Promise<void> {
    this.saving.set(true);
    const comment = this.draftComment().trim();
    const ok = await this.store.updateFeedback(id, { rating: this.draftRating(), comment: comment === '' ? null : comment });
    this.saving.set(false);
    if (ok) this.editingId.set(null);
  }
}
