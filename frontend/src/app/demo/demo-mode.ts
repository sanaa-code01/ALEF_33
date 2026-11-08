/**
 * Demo mode = the app runs without any backend, with fake data kept in the browser.
 * It is switched on automatically on GitHub Pages (*.github.io),
 * or on your PC by opening http://localhost:4200/?demo
 */
export function isDemoMode(): boolean {
  try {
    if (new URLSearchParams(location.search).has('demo')) {
      sessionStorage.setItem('alef33.demo', '1');
    }
    return location.hostname.endsWith('github.io') || sessionStorage.getItem('alef33.demo') === '1';
  } catch {
    return false;
  }
}

export const DEMO_ACCOUNTS = {
  admin: { email: 'admin@alef33.ma', password: 'demo' },
  employee: { email: 'employe@alef33.ma', password: 'demo' },
};
