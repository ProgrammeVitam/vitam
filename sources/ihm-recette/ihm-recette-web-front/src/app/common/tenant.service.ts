import { Injectable } from '@angular/core';
import { BehaviorSubject } from "rxjs/BehaviorSubject"
import { Observable } from "rxjs/Observable";

@Injectable()
export class TenantService {
  private tenantState = new BehaviorSubject<string>('');

  constructor() { }

  changeState(myChange: string) {
    this.tenantState.next(myChange);
  }

  getState(): Observable<string> {
    return this.tenantState.asObservable();
  }

}
