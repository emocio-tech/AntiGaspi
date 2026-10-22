import { Component, inject, signal } from '@angular/core';
import { LucideX } from '@lucide/angular';
import { ProfileStore } from './profile-store';

/** Écran Profil : préférences alimentaires et équipement, lus par le LLM à chaque génération de recettes. */
@Component({
  selector: 'app-profile-page',
  imports: [LucideX],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage {
  protected readonly store = inject(ProfileStore);
  protected readonly newPreference = signal('');
  protected readonly newUtensil = signal('');

  constructor() {
    this.store.load();
  }

  protected async addPreference(event: Event): Promise<void> {
    event.preventDefault();
    if (await this.store.addPreference(this.newPreference())) this.newPreference.set('');
  }

  protected async addUtensil(event: Event): Promise<void> {
    event.preventDefault();
    if (await this.store.addUtensil(this.newUtensil())) this.newUtensil.set('');
  }

  protected valueOf(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}
