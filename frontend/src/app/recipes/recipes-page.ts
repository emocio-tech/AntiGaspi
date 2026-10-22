import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChefHat, LucideClock, LucideCookingPot, LucideMinus, LucidePlus, LucideSparkles, LucideUsers } from '@lucide/angular';
import { ChatPanel } from '../chat/chat-panel';
import { InventoryStore } from '../inventory/inventory-store';
import { URGENCY_STYLE } from '../inventory/urgency';
import { MAX_SERVINGS, MIN_SERVINGS, RecipeStore, TIME_OPTIONS } from './recipe-store';
import { RecipeIngredient } from './recipe.model';

/** Nombre d'ingrédients montrés sur une carte avant « +n ». */
const CHIPS_PER_CARD = 4;
const NOT_IN_INVENTORY = '#9AA1AC';

/** Écran Recettes : réglages de la génération et cartes des recettes proposées. */
@Component({
  selector: 'app-recipes-page',
  imports: [ChatPanel, RouterLink, LucideChefHat, LucideClock, LucideCookingPot, LucideMinus, LucidePlus, LucideSparkles, LucideUsers],
  templateUrl: './recipes-page.html',
  styleUrl: './recipes-page.css',
})
export class RecipesPage {
  protected readonly store = inject(RecipeStore);
  protected readonly inventory = inject(InventoryStore);
  protected readonly timeOptions = TIME_OPTIONS;
  protected readonly minServings = MIN_SERVINGS;
  protected readonly maxServings = MAX_SERVINGS;
  protected readonly chipsPerCard = CHIPS_PER_CARD;
  /** Volet affiché sur écran étroit ; sur grand écran, générateur et chat sont côte à côte. */
  protected readonly tab = signal<'generate' | 'chat'>('generate');

  protected readonly canGenerate = computed(() => this.inventory.items().length > 0 && !this.store.generating());

  protected dotOf(id: number): string {
    return this.urgencyDot(id);
  }

  protected ingredientDot(ingredient: RecipeIngredient): string {
    return ingredient.ingredientId === null ? NOT_IN_INVENTORY : this.urgencyDot(ingredient.ingredientId);
  }

  protected savedLabel(count: number): string {
    return count > 0 ? `Sauve ${count} produit${count > 1 ? 's' : ''} urgent${count > 1 ? 's' : ''}` : 'Utilise votre stock';
  }

  private urgencyDot(id: number): string {
    const found = this.inventory.items().find((ingredient) => ingredient.id === id);
    return found ? URGENCY_STYLE[found.urgency].dot : NOT_IN_INVENTORY;
  }
}
