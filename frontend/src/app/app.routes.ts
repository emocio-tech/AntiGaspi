import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'inventaire' },
  {
    path: 'inventaire',
    title: 'Inventaire · antigaspi',
    loadComponent: () => import('./inventory/inventory-page').then((m) => m.InventoryPage),
  },
  { path: '**', redirectTo: 'inventaire' },
];
