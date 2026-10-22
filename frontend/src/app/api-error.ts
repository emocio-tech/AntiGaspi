import { HttpErrorResponse } from '@angular/common/http';

export const DEFAULT_ERROR = 'Une erreur est survenue, réessayez.';

/** Message d'une réponse d'erreur de l'API (`{"message": ...}`), ou un message par défaut si le serveur ne répond pas. */
export function messageOf(error: unknown): string {
  return (error instanceof HttpErrorResponse && error.error?.message) || DEFAULT_ERROR;
}
