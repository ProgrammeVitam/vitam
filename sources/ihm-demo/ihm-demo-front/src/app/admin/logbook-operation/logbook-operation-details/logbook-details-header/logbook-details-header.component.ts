import { Component, Input, OnInit } from '@angular/core';
import { Event } from '../../../../common/logbook-operation-events/event';
import { LogbookService } from "../../../../ingest/logbook.service";
import { LogbookHelperService } from "../../../../common/logbook-operation-events/logbook-helper.service";

@Component({
  selector: 'vitam-logbook-details-header',
  templateUrl: './logbook-details-header.component.html'
})
export class LogbookDetailsHeaderComponent implements OnInit {
  @Input() operation: any;
  @Input() operationId: string;
  results: any;
  lastEvent: any;
  data: any;
  warningNumber = 0;
  warningMessages: String[] = [];
  errorMessages: String[] = [];
  errorNumber = 0;
  events: Event[] = [];

  constructor(private logbookService: LogbookService, private logbookHelper: LogbookHelperService) { }

  ngOnInit() {
    this.initDoughnut();

    this.logbookService.getDetails(this.operationId).subscribe(
        (data) => {this.results = data.$results[0];
          this.events = this.logbookHelper.initEventsArray(this.results);
          this.lastEvent = this.results.events[this.results.events.length - 1];
          this.getWarnings(this.events);
        }
    );
  }

  private getWarnings(eventsArray: Event[]) {
    this.warningMessages = [];
    this.errorMessages = [];
    for (const event of eventsArray) {
      if (event.subEvents && event.end) {
        if (event.subEvents.length > 0) {
          this.getSubEventsWarnings(event);
        } else {
          if (event.end.evType !== 'PROCESS_SIP_UNITARY') {
            this.countWarningsAndErrors(event);
          }
        }
      } else {
        event.end = event.start;
      }
    }
  }

  private getSubEventsWarnings(event: Event) {
    for (const subEvent of event.subEvents) {
      if (subEvent.subEvents.length > 0) {
        this.getSubEventsWarnings(subEvent);
      } else {
        this.countWarningsAndErrors(subEvent);
      }
    }
  }

  private countWarningsAndErrors (event: Event) {
    switch (event.end.outcome.toUpperCase()) {
      case 'WARNING':
        this.warningNumber++;
        this.warningMessages.push(event.end.outMessg);
        break;
      case 'KO':
      case 'FATAL':
        this.errorNumber++;
        this.errorMessages.push(event.end.outMessg);
        break;
      default:
        break;
    }
  }

  // TODO: Doughnut needs to be initialized with steps done out of total steps (if possible)
  private initDoughnut() {
    this.data = {
      labels: ['A', 'B', 'C'],
      datasets: [
        {
          data: [300, 50, 100],
          backgroundColor: [
            '#FF6384',
            '#36A2EB',
            '#FFCE56'
          ],
          hoverBackgroundColor: [
            '#FF6384',
            '#36A2EB',
            '#FFCE56'
          ]
        }]
    };
  }

}
