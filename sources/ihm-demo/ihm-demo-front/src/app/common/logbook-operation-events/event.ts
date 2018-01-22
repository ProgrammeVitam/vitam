export class Event {
  eventData: any;
  subEvents: Event[];

  constructor(eventData: any, subEvents: Event[]) {
    this.eventData = eventData;
    this.subEvents = subEvents;
  }
}
