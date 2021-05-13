import { registerPlugin } from '@capacitor/core';
{
    GoogleFitPlugin;
}
from;
'./definitions';
const GoogleFit = registerPlugin('RateApp', {
    web: () => import('./web').then(m => new m.GoogleFitWeb()),
});
export { GoogleFit };
//# sourceMappingURL=index.js.map