import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideCarrot, LucideChefHat, LucideRefrigerator, LucideUserRound } from '@lucide/angular';
import { InventoryStore } from './inventory/inventory-store';

/** Coquille de l'application : navigation (barre latérale, en-tête sur mobile) et contenu de l'écran courant. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideCarrot, LucideChefHat, LucideRefrigerator, LucideUserRound],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly inventory = inject(InventoryStore);
}
