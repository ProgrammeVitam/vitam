import { Injectable } from '@angular/core';
import {Event} from './event';


@Injectable()
export class LogbookHelperService {
  eventData: any;

  constructor() { }

  initEventsArray(logbook: any) {
    let events = [];
    this.eventData = this.getEventData(logbook);
    let eventIndex = 0;
    let tasks: Event[] = [];
    let task: Event;
    let eventsCount = 0;


    if (logbook.events.length > 1) {
      for (const event of logbook.events) {
        eventsCount++;
        this.eventData = this.getEventData(event);

        if (eventsCount >= logbook.events.length) {
          if (task) {
            tasks.push(task);
            task = null;
          }
          if (tasks.length > 0) {
            events[eventIndex].subEvents = tasks;
          }
          tasks = [];
          if (events[eventIndex] && events[eventIndex].end) {
            events[eventIndex].end = this.eventData;
          } else {
            events.push(new Event('', this.eventData, []));
          }
        } else {
          if (!event.evParentId) {
            if (task) {
              tasks.push(task);
              task = null;
            }
            if (tasks.length > 0) {
              events[eventIndex].subEvents = tasks;
            }
            tasks = [];

            if (event.evType.indexOf('.STARTED') > -1) {
              events.push(new Event(this.eventData, '', []));
              eventIndex = events.length - 1;
            } else {
              if (events[eventIndex]) {
                events[eventIndex].end = this.eventData;
              } else {
                events.push(new Event('', this.eventData, []));
                eventIndex = events.length;
              }
            }
          } else {
            if (event.evParentId === events[eventIndex].end.evId) {
              if (task) {
                tasks.push(task);
                task = null;
              }
              task = new Event('', this.eventData, []);
            } else {
              if (event.evParentId === task.end.evId) {
                task.subEvents.push(new Event('', this.eventData, []));
              }
            }
          }
        }
      }
    } else {
      events.push(new Event(logbook, logbook.events[0], []));
    }
    return events;
  }

  private getEventData(event: any) {
    return {
      'evId': event.evId,
      'evParentId': event.evParentId,
      'evType': event.evType,
      'evDateTime': event.evDateTime,
      'evDetData': event.evDetData,
      'outcome': event.outcome,
      'outMessg': event.outMessg
    };
  }
}
