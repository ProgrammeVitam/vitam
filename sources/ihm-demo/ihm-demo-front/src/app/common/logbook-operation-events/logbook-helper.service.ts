import{Injectable}from'@angular/core';
import {Event}from './event';


@Injectable()
export class LogbookHelperService {
eventData: any;

constructor() { }

  initEventsArray(logbook: any) {
    let events = [];
    this.eventData = this.getEventData(logbook);
    let rootEvent = null; // Step event
    let actionEvent = null; // Action event
    let started = false;
    if (logbook.events.length > 0) {
      for (const event of logbook.events) {
        this.eventData = this.getEventData(event);

        if (!event.evParentId) { //Step event
          rootEvent = new Event(this.eventData, []);

          if (event.evType.endsWith(".STARTED")) {
            events.push(rootEvent);
            started = true;
          } else {
            if (logbook.events.indexOf(event) != logbook.events.length - 1 && started) {
                events.pop();
                started = false;
            }
            events.push(rootEvent);
          }

        } else if (event.evParentId === rootEvent.eventData.evId) { //Action events
            actionEvent = new Event(this.eventData, []);
            rootEvent.subEvents.push(actionEvent);
        } else if (event.evParentId === actionEvent.eventData.evId) { //SubTask events
            actionEvent.subEvents.push(new Event(this.eventData, []));
        }
      }
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
