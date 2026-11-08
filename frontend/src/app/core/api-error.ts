import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from './models';

/** Turns any HTTP error into one readable French sentence (the backend already answers in French) */
export function errorMessage(err: unknown): string {
  if (!(err instanceof HttpErrorResponse)) return 'Erreur inattendue.';
  if (err.status === 0) return 'Le serveur ne répond pas. Le backend est-il démarré ?';
  if (err.status === 403) return "Vous n'avez pas les droits pour cette action.";

  const problem = err.error as ApiProblem | null;
  if (problem?.errors) {
    return Object.values(problem.errors).join(' · ');
  }
  return problem?.detail ?? `Erreur ${err.status}`;
}
