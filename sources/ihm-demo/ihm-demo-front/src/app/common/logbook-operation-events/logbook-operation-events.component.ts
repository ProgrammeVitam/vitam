import {Component, Input, OnInit} from '@angular/core';
import {Event} from './event';

@Component({
  selector: 'vitam-logbook-operation-events',
  templateUrl: './logbook-operation-events.component.html',
  styleUrls: ['./logbook-operation-events.component.css']
})
export class LogbookOperationEventsComponent implements OnInit {
  @Input() operation: any;
  results: any;
  eventData: any;
  events: Event[] = [];

  constructor() { }

  ngOnInit() {
    this.results = this.operation.$results[0];
    this.eventData = this.getEventData(this.results);
    this.events.push(new Event(this.eventData, '', null));
    let eventIndex = 0;
    let subEventIndex = 0;
    let subEvents: Event[] = [];
    let subTask: Event;

    for (const event of this.results.events) {
      this.eventData = this.getEventData(event);

      if (event.evType.startsWith('STP')) {
        if (event.outcome === 'STARTED') {
          this.events.push(new Event(this.eventData, '', event.events));
          eventIndex = this.events.length - 1;
        } else {
          this.events[eventIndex].end = this.eventData;
          this.events[eventIndex].subEvents = subEvents;
          subEvents = [];
        }
      }
      if (event.evType.startsWith('CHECK')) {
        if (event.evType.indexOf('.') > -1) {
          if (event.outcome === 'STARTED') {
            subTask = new Event(this.eventData, '', []);
          } else {
            subTask.end = this.eventData;
            subEvents[subEventIndex].subEvents.push(subTask);
            subTask = null;
          }
        } else {
          if (event.outcome === 'STARTED') {
            subEvents.push(new Event(this.eventData, '', []));
            subEventIndex = subEvents.length - 1;
          } else {
            subEvents[subEventIndex].end = this.eventData;
          }
        }
      }
    }

  }

  private getEventData(event: any) {
    return {
      'evId': event.evId,
      'evType': event.evType,
      'evDateTime': event.evDateTime,
      'evDetData': event.evDetData,
      'outcome': event.outcome,
      'outMessg': event.outMessg
    };
  }
}
