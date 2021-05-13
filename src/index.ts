import { registerPlugin } from '@capacitor/core';

import type { GoogleFitPlugin } from './definitions';

const GoogleFit = registerPlugin<GoogleFitPlugin>('RateApp', {
  web: () => import('./web').then(m => new m.GoogleFitWeb()),
});


export * from './definitions';

export { GoogleFit }
