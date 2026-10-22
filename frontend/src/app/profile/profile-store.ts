import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { messageOf } from '../api-error';
import { Profile, Utensil } from './profile.model';

/**
 * Préférences alimentaires et ustensiles, chargés depuis l'API et modifiés via elle.
 * Aucune mise à jour optimiste : la liste affichée est toujours celle que le serveur a acceptée.
 * Les modifications renvoient `true` si le serveur les a acceptées, `false` sinon (le message est dans `actionError`).
 */
@Injectable({ providedIn: 'root' })
export class ProfileStore {
  private readonly http = inject(HttpClient);

  readonly preferences = signal<string[]>([]);
  readonly utensils = signal<Utensil[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  /** Message du serveur pour la dernière modification refusée, `null` si elle a réussi. */
  readonly actionError = signal<string | null>(null);

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    let pending = 2;
    let failed = false;
    const done = () => {
      if (--pending === 0) {
        this.loading.set(false);
        this.loadError.set(failed);
      }
    };
    const fail = () => {
      failed = true;
      done();
    };
    this.http.get<Profile>('/api/profile').subscribe({
      next: (profile) => this.preferences.set(profile.preferences),
      error: fail,
      complete: done,
    });
    this.http.get<Utensil[]>('/api/utensils').subscribe({
      next: (utensils) => this.utensils.set(utensils),
      error: fail,
      complete: done,
    });
  }

  addPreference(text: string): Promise<boolean> {
    const preference = text.trim();
    if (!preference) return Promise.resolve(false);
    return this.savePreferences([...this.preferences(), preference]);
  }

  removePreference(text: string): Promise<boolean> {
    return this.savePreferences(this.preferences().filter((preference) => preference !== text));
  }

  addUtensil(name: string): Promise<boolean> {
    const trimmed = name.trim();
    if (!trimmed) return Promise.resolve(false);
    return this.send(this.http.post<Utensil>('/api/utensils', { name: trimmed }), (utensil) =>
      this.utensils.update((list) => [...list, utensil].sort(byName)),
    );
  }

  removeUtensil(id: number): Promise<boolean> {
    return this.send(this.http.delete<void>(`/api/utensils/${id}`), () =>
      this.utensils.update((list) => list.filter((utensil) => utensil.id !== id)),
    );
  }

  private savePreferences(preferences: string[]): Promise<boolean> {
    return this.send(this.http.put<Profile>('/api/profile', { preferences }), (profile) =>
      this.preferences.set(profile.preferences),
    );
  }

  /** Envoie la requête tout de suite ; `onSuccess` met à jour l'état dès la réponse. */
  private send<T>(request: Observable<T>, onSuccess: (body: T) => void): Promise<boolean> {
    this.actionError.set(null);
    return new Promise((resolve) => {
      request.subscribe({
        next: (body) => {
          onSuccess(body);
          resolve(true);
        },
        error: (error: unknown) => {
          this.actionError.set(messageOf(error));
          resolve(false);
        },
      });
    });
  }
}

const byName = (a: Utensil, b: Utensil) => a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' });
