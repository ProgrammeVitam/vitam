import { Injectable } from '@angular/core';

@Injectable()
export class EggService {
  private vitamEgg: boolean = false;

  constructor() { }

  changeState(myChange: boolean) {
    this.vitamEgg = myChange;
  }

  getState(): boolean {
    return this.vitamEgg;
  }

}
