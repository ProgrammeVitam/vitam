import { Injectable } from '@angular/core';
import {BehaviorSubject} from "rxjs/BehaviorSubject"
import {Observable} from "rxjs/Observable";

export class BreadcrumbElement {
  label: string;
  routerLink: string;
}

@Injectable()
export class BreadcrumbService {
  private breadCrumbState = new BehaviorSubject<BreadcrumbElement[]>([]);

  constructor() { }

  changeState(myChange: BreadcrumbElement[]) {
    this.breadCrumbState.next(myChange);
  }

  getState(): Observable<BreadcrumbElement[]> {
    return this.breadCrumbState.asObservable();
  }

}
