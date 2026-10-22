/** Profil tel que `GET /api/profile` le renvoie. */
export interface Profile {
  preferences: string[];
}

/** Ustensile tel que `GET /api/utensils` le renvoie. */
export interface Utensil {
  id: number;
  name: string;
}
