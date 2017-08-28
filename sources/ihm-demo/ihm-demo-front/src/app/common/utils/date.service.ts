import { Injectable } from '@angular/core';
import {DatePipe} from '@angular/common';

@Injectable()
export class DateService {

  constructor() { }

  static handleDate(date): string {
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy');
  }

  static handleDateWithTime(date): string {
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy HH:mm:ss');
  }


}
