import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable} from "rxjs/Rx";

@Injectable()
export class AccessContractService {
  private accessContractState = new BehaviorSubject<string>('');

  constructor() { }

  update(myChange: string) {
    this.accessContractState.next(myChange);
  }

  getUpdate(): Observable<string> {
    return this.accessContractState.asObservable();
  }

}
