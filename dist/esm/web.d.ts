import { WebPlugin } from '@capacitor/core';
import { GoogleFitPlugin } from './definitions';
export declare class GoogleFitWeb extends WebPlugin implements GoogleFitPlugin {
    constructor();
    connectToGoogleFit(): Promise<void>;
    isAllowed(): Promise<void>;
    getHistory(): Promise<any>;
    getHistoryActivity(): Promise<any>;
}
