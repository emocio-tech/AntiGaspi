import { httpResource } from '@angular/common/http';
import { computed, Injectable } from '@angular/core';
import { Ingredient } from './ingredient.model';

/** Inventaire chargé depuis l'API, partagé entre la navigation (pastille) et l'écran Inventaire. */
@Injectable({ providedIn: 'root' })
export class InventoryStore {
  private readonly resource = httpResource<Ingredient[]>(() => '/api/ingredients');

  readonly items = computed<Ingredient[]>(() => (this.resource.hasValue() ? this.resource.value() : []));
  readonly loading = computed(() => this.resource.isLoading());
  readonly error = computed(() => this.resource.error() !== undefined);
  /** Nombre d'aliments à consommer aujourd'hui ou dans les 3 jours. */
  readonly urgentCount = computed(
    () => this.items().filter((i) => i.urgency === 'EXPIRED_OR_TODAY' || i.urgency === 'SOON').length,
  );

  reload(): void {
    this.resource.reload();
  }
}
