import { WebPlugin } from '@capacitor/core';
import { GoogleFitPlugin } from './definitions';

export class GoogleFitWeb extends WebPlugin implements GoogleFitPlugin {
  constructor() {
    super({
      name: 'GoogleFit',
      platforms: ['web']
    });
  }

  async connectToGoogleFit(options: any): Promise<void> {
    throw new Error("Method not implemented." + options);
  }
  async isAllowed(): Promise<void> {
    throw new Error("Method not implemented.");
  }
  async getHistory(): Promise<any> {
    throw new Error("Method not implemented.");
  }
  async getHistoryActivity(): Promise<any> {
    throw new Error("Method not implemented.");
  }
}
