export class Event {
  start: any;
  end: any;
  subEvents: Event[];

  constructor(start: any, end: any, subEvents: Event[]) {
    this.start = start;
    this.end = end;
    this.subEvents = subEvents;
  }
}
