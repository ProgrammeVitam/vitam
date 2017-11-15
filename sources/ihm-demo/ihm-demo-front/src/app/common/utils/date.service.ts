import { Injectable } from '@angular/core';
import {DatePipe} from '@angular/common';
import {isUndefined} from "util";
import {ObjectsService} from "./objects.service";

@Injectable()
export class DateService {

  constructor() { }

  static handleDate(date: string): string {
    if (date === undefined) {
      return '';
    }
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy');
  }

  static handleDateWithTime(date: string): string {
    if (date === undefined) {
      return '';
    }
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy HH:mm:ss');
  }


}
