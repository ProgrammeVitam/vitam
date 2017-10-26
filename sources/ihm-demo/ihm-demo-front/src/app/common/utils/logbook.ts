export abstract class LogbookEvent {
    public evId: string;
    public evType: string;
    public evDateTime: string;
    public evDetData: any;
    public evIdProc: string;
    public evTypeProc: string;
    public outcome: string;
    public outDetail: string;
    public outMessg: string;
    public agId: any;
    public evIdReq: string;
    public obId: string;
}

// #id and #tenant not included ('#' usage forbidden)
// Use logbook['#id'] or logbook['#tenant'] where needed
export class Logbook {
    public evId: string;
    public evType: string;
    public evDateTime: string;
    public evDetData: any;
    public evIdProc: string;
    public evTypeProc: string;
    public outcome: string;
    public outDetail: string;
    public outMessg: string;
    public agId: any;
    public agIdApp: string;
    public agIdAppSession: string;
    public evIdReq: string;
    public agIdSubm: string;
    public agIdOrig: string;
    public obId: string;
    public obIdReq: string;
    public obIdIn: string;
    public events: LogbookEvent[];
}