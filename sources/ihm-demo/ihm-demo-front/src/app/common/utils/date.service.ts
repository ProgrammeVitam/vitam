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

  static handleDateForRules(date: string): string {
    if (date === undefined) {
      return '';
    }
    return new DatePipe('en-US').transform(date, 'yyyy-MM-dd');
  }

  static handleDateWithTime(date: string): string {
    if (date === undefined) {
      return '';
    }
    return new DatePipe('en-US').transform(date, 'dd/MM/yyyy HH:mm:ss');
  }

  //add/remove to a given UTC date (as Date or String object), some hours, minutes, seconds given as argument
  //NB : the milliseconds argument will be set as and will not be shift to the original date milliseconds

  static transformUTCDate(date: any, hours: number, minutes: number, seconds: number, milliseconds: number): Date {
    if (date === undefined) {
          return null;
    }

    let adjustedUTCDate = new Date(date);
    adjustedUTCDate.setUTCHours(adjustedUTCDate.getUTCHours()+hours);
    adjustedUTCDate.setUTCMinutes(adjustedUTCDate.getUTCMinutes()+minutes);
    adjustedUTCDate.setUTCSeconds(adjustedUTCDate.getUTCSeconds()+seconds);
    adjustedUTCDate.setUTCMilliseconds(milliseconds);

    return adjustedUTCDate;
  }

}
