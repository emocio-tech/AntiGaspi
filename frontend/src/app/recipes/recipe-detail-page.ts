import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowLeft, LucideClock, LucideCookingPot, LucideTriangleAlert, LucideUsers } from '@lucide/angular';
import { formatQuantity } from '../inventory/urgency';
import { RecipeStore } from './recipe-store';

/** Détail d'une recette proposée : ingrédients, ustensiles et étapes. */
@Component({
  selector: 'app-recipe-detail-page',
  imports: [RouterLink, LucideArrowLeft, LucideClock, LucideCookingPot, LucideTriangleAlert, LucideUsers],
  templateUrl: './recipe-detail-page.html',
  styleUrl: './recipe-detail-page.css',
})
export class RecipeDetailPage {
  private readonly store = inject(RecipeStore);

  /** Position de la recette dans la liste proposée (paramètre de route). */
  readonly id = input.required<string>();

  protected readonly formatQuantity = formatQuantity;
  protected readonly generated = computed(() => this.store.result(Number(this.id())));
}
