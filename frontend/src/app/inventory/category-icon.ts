import { Component, input } from '@angular/core';
import {
  LucideApple,
  LucideBeef,
  LucideCarrot,
  LucideCupSoda,
  LucideDroplet,
  LucideEgg,
  LucideMilk,
  LucidePackage,
  LucideSnowflake,
  LucideSoup,
  LucideWheat,
} from '@lucide/angular';

/** Icône de la catégorie d'un aliment (code renvoyé par l'API). */
@Component({
  selector: 'app-category-icon',
  imports: [
    LucideApple,
    LucideBeef,
    LucideCarrot,
    LucideCupSoda,
    LucideDroplet,
    LucideEgg,
    LucideMilk,
    LucidePackage,
    LucideSnowflake,
    LucideSoup,
    LucideWheat,
  ],
  template: `
    @switch (category()) {
      @case ('FRUITS') {
        <svg lucideApple [size]="size()"></svg>
      }
      @case ('LEGUMES') {
        <svg lucideCarrot [size]="size()"></svg>
      }
      @case ('PRODUITS_LAITIERS') {
        <svg lucideMilk [size]="size()"></svg>
      }
      @case ('VIANDES_POISSONS') {
        <svg lucideBeef [size]="size()"></svg>
      }
      @case ('OEUFS') {
        <svg lucideEgg [size]="size()"></svg>
      }
      @case ('FECULENTS') {
        <svg lucideWheat [size]="size()"></svg>
      }
      @case ('CONSERVES') {
        <svg lucideSoup [size]="size()"></svg>
      }
      @case ('SURGELES') {
        <svg lucideSnowflake [size]="size()"></svg>
      }
      @case ('BOISSONS') {
        <svg lucideCupSoda [size]="size()"></svg>
      }
      @case ('CONDIMENTS') {
        <svg lucideDroplet [size]="size()"></svg>
      }
      @default {
        <svg lucidePackage [size]="size()"></svg>
      }
    }
  `,
  styles: ':host { display: inline-flex; }',
})
export class CategoryIcon {
  readonly category = input.required<string>();
  readonly size = input(18);
}
