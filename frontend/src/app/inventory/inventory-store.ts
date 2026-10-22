import { HttpClient, httpResource } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { messageOf } from '../api-error';
import { Ingredient, IngredientRequest } from './ingredient.model';

/** Délai pendant lequel un retrait peut être annulé avant d'être envoyé au serveur. */
export const REMOVAL_DELAY_MS = 5000;

/** Inventaire chargé depuis l'API, partagé entre la navigation (pastille) et l'écran Inventaire. */
@Injectable({ providedIn: 'root' })
export class InventoryStore {
  private readonly http = inject(HttpClient);
  private readonly resource = httpResource<Ingredient[]>(() => '/api/ingredients');

  readonly items = computed<Ingredient[]>(() => {
    if (!this.resource.hasValue()) return [];
    const hidden = [...this.pending(), ...this.sent()];
    return this.resource.value().filter((i) => !hidden.includes(i.id));
  });
  readonly loading = computed(() => this.resource.isLoading());
  readonly error = computed(() => this.resource.error() !== undefined);
  /** Nombre d'aliments à consommer aujourd'hui ou dans les 3 jours. */
  readonly urgentCount = computed(
    () => this.items().filter((i) => i.urgency === 'EXPIRED_OR_TODAY' || i.urgency === 'SOON').length,
  );

  reload(): void {
    this.resource.reload();
  }

  /** Message du serveur pour la dernière écriture refusée, `null` sinon. */
  readonly actionError = signal<string | null>(null);

  /** Ids dont le retrait peut encore être annulé. */
  private readonly pending = signal<number[]>([]);
  /** Ids déjà envoyés au serveur : masqués jusqu'au rechargement de l'inventaire. */
  private readonly sent = signal<number[]>([]);
  private timer: ReturnType<typeof setTimeout> | undefined;

  /** Nombre d'aliments dont le retrait est en attente (annulable). */
  readonly pendingRemoval = computed(() => this.pending().length);

  /** Ajoute un aliment puis recharge l'inventaire ; `false` si le serveur refuse (message dans `actionError`). */
  add(request: IngredientRequest): Promise<boolean> {
    return this.write(this.http.post<Ingredient>('/api/ingredients', request));
  }

  /** Remplace un aliment puis recharge l'inventaire ; `false` si le serveur refuse (message dans `actionError`). */
  update(id: number, request: IngredientRequest): Promise<boolean> {
    return this.write(this.http.put<Ingredient>(`/api/ingredients/${id}`, request));
  }

  /**
   * Masque les aliments tout de suite et ne les supprime côté serveur qu'après `REMOVAL_DELAY_MS`.
   * Un retrait déjà en attente est envoyé immédiatement. Un rechargement ou une fermeture de l'onglet pendant le
   * délai annule le retrait : rien n'est envoyé.
   */
  scheduleRemoval(ids: number[]): void {
    this.flushRemoval();
    if (ids.length === 0) return;
    this.pending.set(ids);
    this.timer = setTimeout(() => this.flushRemoval(), REMOVAL_DELAY_MS);
  }

  /** Annule le retrait en attente : les aliments réapparaissent. */
  undo(): void {
    clearTimeout(this.timer);
    this.timer = undefined;
    this.pending.set([]);
  }

  private flushRemoval(): void {
    clearTimeout(this.timer);
    this.timer = undefined;
    const ids = this.pending();
    if (ids.length === 0) return;
    this.pending.set([]);
    this.sent.update((list) => [...list, ...ids]);
    this.actionError.set(null);
    this.http.delete<void>(`/api/ingredients?ids=${ids.join(',')}`).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => {
        this.sent.update((list) => list.filter((id) => !ids.includes(id)));
        this.actionError.set(messageOf(error));
        this.reload();
      },
    });
  }

  private write(request: Observable<Ingredient>): Promise<boolean> {
    this.actionError.set(null);
    return new Promise((resolve) => {
      request.subscribe({
        next: () => {
          // Rechargement différé d'une microtâche : la promesse doit être résolue avant que le GET de rechargement ne rende l'application instable, sinon `whenStable` des composants qui attendent la promesse resterait bloqué
          queueMicrotask(() => this.reload());
          resolve(true);
        },
        error: (error: unknown) => {
          this.actionError.set(messageOf(error));
          resolve(false);
        },
      });
    });
  }
}
