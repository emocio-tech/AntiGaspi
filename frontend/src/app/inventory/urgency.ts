import { Ingredient, Urgency } from './ingredient.model';

/** Du plus pressant au moins pressant. */
export const URGENCY_ORDER: readonly Urgency[] = ['EXPIRED_OR_TODAY', 'SOON', 'WEEK', 'LATER'];

export interface UrgencyStyle {
  /** Couleur de la pastille. */
  dot: string;
  /** Couleur du texte du badge. */
  text: string;
  /** Fond du badge. */
  tint: string;
  /** Titre de la section. */
  title: string;
}

export const URGENCY_STYLE: Record<Urgency, UrgencyStyle> = {
  EXPIRED_OR_TODAY: { dot: '#F4364C', text: '#C2373C', tint: '#FEEEEE', title: "À consommer aujourd'hui" },
  SOON: { dot: '#F7941D', text: '#A05E18', tint: '#FFF3E4', title: 'Dans les 3 jours' },
  WEEK: { dot: '#F2C230', text: '#7A5D00', tint: '#FFF8DA', title: 'Cette semaine' },
  LATER: { dot: '#6CC24A', text: '#007D63', tint: '#EAFAF7', title: 'Plus tard' },
};

export interface UrgencyGroup {
  urgency: Urgency;
  items: Ingredient[];
}

/** Libellé de délai d'un aliment : « Périmé », « Demain », « 5 jours », « ≈ 3 sem. » si la date est estimée. */
export function urgencyLabel(ingredient: Pick<Ingredient, 'daysLeft' | 'estimated'>): string {
  const { daysLeft, estimated } = ingredient;
  if (daysLeft < 0) return 'Périmé';
  if (daysLeft === 0) return "Aujourd'hui";
  if (daysLeft === 1) return 'Demain';
  const duration = formatDuration(daysLeft);
  return estimated ? `≈ ${duration}` : duration;
}

function formatDuration(days: number): string {
  if (days <= 14) return `${days} jours`;
  if (days <= 60) return `${Math.round(days / 7)} sem.`;
  return `${Math.round(days / 30)} mois`;
}

/** Quantité lisible : « 2,5 kg », « 4 pièces », « 250 g » pour 0,25 kg. */
export function formatQuantity(quantity: number, unit: string): string {
  const rounded = Math.round(quantity * 100) / 100;
  if (unit === 'pièce') return `${rounded} pièce${rounded > 1 ? 's' : ''}`;
  if (unit === 'kg' && rounded < 1) return `${Math.round(rounded * 1000)} g`;
  if (unit === 'l' && rounded < 1) return `${Math.round(rounded * 1000)} ml`;
  return `${String(rounded).replace('.', ',')} ${unit}`;
}

/** Regroupe par urgence, dans l'ordre d'urgence, sans les groupes vides ; l'ordre de l'API est conservé dans un groupe. */
export function groupByUrgency(ingredients: Ingredient[]): UrgencyGroup[] {
  return URGENCY_ORDER.map((urgency) => ({
    urgency,
    items: ingredients.filter((ingredient) => ingredient.urgency === urgency),
  })).filter((group) => group.items.length > 0);
}
