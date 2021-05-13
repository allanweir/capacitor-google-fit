import { WebPlugin } from '@capacitor/core';
export class GoogleFitWeb extends WebPlugin {
    constructor() {
        super({
            name: 'GoogleFit',
            platforms: ['web']
        });
    }
    async connectToGoogleFit() {
        throw new Error("Method not implemented.");
    }
    async isAllowed() {
        throw new Error("Method not implemented.");
    }
    async getHistory() {
        throw new Error("Method not implemented.");
    }
    async getHistoryActivity() {
        throw new Error("Method not implemented.");
    }
}
//# sourceMappingURL=web.js.map