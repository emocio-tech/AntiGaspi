import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'inventaire' },
  {
    path: 'inventaire',
    title: 'Inventaire · antigaspi',
    loadComponent: () => import('./inventory/inventory-page').then((m) => m.InventoryPage),
  },
  {
    path: 'recettes',
    title: 'Recettes · antigaspi',
    loadComponent: () => import('./recipes/recipes-page').then((m) => m.RecipesPage),
  },
  {
    path: 'recettes/:id/cuisine',
    title: 'Cuisiner · antigaspi',
    loadComponent: () => import('./recipes/cook-page').then((m) => m.CookPage),
  },
  {
    path: 'recettes/:id',
    title: 'Recette · antigaspi',
    loadComponent: () => import('./recipes/recipe-detail-page').then((m) => m.RecipeDetailPage),
  },
  {
    path: 'profil',
    title: 'Profil · antigaspi',
    loadComponent: () => import('./profile/profile-page').then((m) => m.ProfilePage),
  },
  { path: '**', redirectTo: 'inventaire' },
];
