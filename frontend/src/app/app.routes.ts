import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'inventaire' },
  {
    path: 'inventaire',
    title: 'Inventaire · antigaspi',
    loadComponent: () => import('./inventory/inventory-page').then((m) => m.InventoryPage),
  },
  {
    path: 'profil',
    title: 'Profil · antigaspi',
    loadComponent: () => import('./profile/profile-page').then((m) => m.ProfilePage),
  },
  { path: '**', redirectTo: 'inventaire' },
];
