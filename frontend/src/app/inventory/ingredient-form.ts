import { httpResource } from '@angular/common/http';
import { Component, computed, effect, ElementRef, inject, input, output, signal, untracked, viewChild } from '@angular/core';
import { LucideX } from '@lucide/angular';
import { Ingredient, IngredientRequest, UNITS, UnitCode } from './ingredient.model';
import { InventoryStore } from './inventory-store';

interface CategoryOption {
  code: string;
  label: string;
}

/** Formulaire d'ajout (ingrédient absent) ou de modification d'un aliment. */
@Component({
  selector: 'app-ingredient-form',
  imports: [LucideX],
  templateUrl: './ingredient-form.html',
  styleUrl: './ingredient-form.css',
})
export class IngredientForm {
  private readonly store = inject(InventoryStore);
  private readonly nameField = viewChild.required<ElementRef<HTMLInputElement>>('nameField');

  readonly ingredient = input<Ingredient | null>(null);
  readonly closed = output<void>();

  protected readonly units = UNITS;
  private readonly categoriesResource = httpResource<CategoryOption[]>(() => '/api/ingredients/categories');
  protected readonly categories = computed(() =>
    this.categoriesResource.hasValue() ? this.categoriesResource.value() : [],
  );

  protected readonly name = signal('');
  protected readonly category = signal('');
  protected readonly quantity = signal('');
  protected readonly unit = signal('');
  protected readonly expiry = signal('');
  protected readonly saving = signal(false);
  protected readonly error = this.store.actionError;

  protected readonly valid = computed(
    () => this.name().trim() !== '' && Number(this.quantity()) > 0 && this.category() !== '' && this.unit() !== '',
  );

  constructor() {
    // Repeuple les champs à chaque changement d'aliment, y compris sans fermer le formulaire
    effect(() => {
      const item = this.ingredient();
      untracked(() => {
        this.store.actionError.set(null);
        if (!item) return;
        this.name.set(item.name);
        this.category.set(item.category);
        this.quantity.set(String(item.quantity));
        this.unit.set(UNITS.find((u) => u.symbol === item.unit)?.code ?? '');
        this.expiry.set(item.estimated ? '' : item.expiryDate);
      });
    });
  }

  protected async submit(): Promise<void> {
    if (!this.valid() || this.saving()) return;
    const request: IngredientRequest = {
      name: this.name().trim(),
      category: this.category(),
      quantity: Number(this.quantity()),
      unit: this.unit() as UnitCode,
      expiryDate: this.expiry() || null,
    };
    const item = this.ingredient();
    this.saving.set(true);
    const ok = item ? await this.store.update(item.id, request) : await this.store.add(request);
    this.saving.set(false);
    if (!ok) return;
    if (item) {
      this.closed.emit();
    } else {
      // Ajout en rafale : on garde catégorie et unité, on repart du nom
      this.name.set('');
      this.quantity.set('');
      this.expiry.set('');
      this.nameField().nativeElement.focus();
    }
  }
}
