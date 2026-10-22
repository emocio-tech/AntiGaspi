import { HttpClient, httpResource } from '@angular/common/http';
import { ApplicationRef, computed, inject, Injectable, signal } from '@angular/core';
import { messageOf } from '../api-error';
import { CookedRecipe, FeedbackRequest } from './history.model';

/** Recettes cuisinées, chargées depuis l'API ; l'avis se modifie via `updateFeedback`. */
@Injectable({ providedIn: 'root' })
export class HistoryStore {
  private readonly http = inject(HttpClient);
  private readonly app = inject(ApplicationRef);
  private readonly resource = httpResource<CookedRecipe[]>(() => '/api/cooked-recipes');

  readonly items = computed<CookedRecipe[]>(() => (this.resource.hasValue() ? this.resource.value() : []));
  readonly loading = computed(() => this.resource.isLoading());
  readonly error = computed(() => this.resource.error() !== undefined);
  /** Message du serveur pour le dernier avis refusé, `null` sinon. */
  readonly actionError = signal<string | null>(null);

  reload(): void {
    this.resource.reload();
  }

  /** Enregistre l'avis puis recharge l'historique ; `false` si le serveur refuse (message dans `actionError`). */
  updateFeedback(id: number, request: FeedbackRequest): Promise<boolean> {
    this.actionError.set(null);
    return new Promise((resolve) => {
      this.http.put<CookedRecipe>(`/api/cooked-recipes/${id}/feedback`, request).subscribe({
        next: () => {
          // Rechargement différé d'une microtâche : la promesse doit être résolue avant que le GET de rechargement ne rende l'application instable
          resolve(true);
          queueMicrotask(() => {
            this.reload();
            // Lance tout de suite le GET de rechargement (effet de la ressource) sans attendre le prochain rendu
            this.app.tick();
          });
        },
        error: (error: unknown) => {
          this.actionError.set(messageOf(error));
          resolve(false);
        },
      });
    });
  }
}
