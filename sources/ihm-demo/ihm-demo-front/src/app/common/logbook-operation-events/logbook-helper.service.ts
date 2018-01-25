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
      for (let evt of logbook.events) {
        this.eventData = this.getEventData(evt);
        if (!evt.evParentId) { //Step event
          rootEvent = new Event(this.eventData, []);
          if (evt.evType.endsWith(".STARTED")) {
            events.push(rootEvent);
            started = true;
          } else {
            if ((logbook.events.indexOf(evt) != logbook.events.length - 1 || evt.outcome === "FATAL") && started) {
                events.pop();
                started = false;
            }
            events.push(rootEvent);
          }
        } else {
            if (!rootEvent) {
                console.log("Error, step events should have a null parent id");
            }
            if (evt.evParentId === rootEvent.eventData.evId) { //Action events
                actionEvent = new Event(this.eventData, []);
                rootEvent.subEvents.push(actionEvent);
            } else {
                if (!actionEvent) {
                    console.log("Error, to have treatemnt event, task event should not be null");
                }
                if (evt.evParentId === actionEvent.eventData.evId) { //SubTask events
                    actionEvent.subEvents.push(new Event(this.eventData, []));
                }
            }
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