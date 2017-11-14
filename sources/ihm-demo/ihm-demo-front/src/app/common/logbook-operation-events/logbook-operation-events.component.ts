import {Component, Input, OnInit} from '@angular/core';
import {Event} from './event';
import {LogbookService} from "../../ingest/logbook.service";
import {LogbookHelperService} from "./logbook-helper.service";

@Component({
  selector: 'vitam-logbook-operation-events',
  templateUrl: './logbook-operation-events.component.html',
  styleUrls: ['./logbook-operation-events.component.css']
})
export class LogbookOperationEventsComponent implements OnInit {
  @Input() isIngestOperation: boolean;
  @Input() results: any;
  events: Event[] = [];

  constructor(private logbookHelper: LogbookHelperService) { }

  ngOnInit() {
    this.events = this.logbookHelper.initEventsArray(this.results);
  }

}
