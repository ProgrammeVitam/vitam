import { Component, OnInit, Input } from '@angular/core';
import {Router} from '@angular/router';


/**
 * Component used to display a data-dable with VITAM style and rules.
 * This component MUST be call with:
 * - items: Data to be displayed (Array of object)
 * - cols: Column names and label [{field: 'fieldName in items', label: 'Name to be displayed in header'}]
 *
 * This component CAN be call with:
 * - route: routerLink to use when user click on a row. This route is completed with item.value
 * - colsWithIcon: add these columns at the end of the table with clickable icons
 * - actionOnIcon: function called when clickable icons are clicked
 * - service: service to be used for actionOnIcon
 * - getClass: specific css class for rows of the table
 * - filter: specific orderBy filter for rows of the table
 * - selectionMode: See primeNg datatable selectionMode param. Can be null, 'single' or 'multiple'
 */
@Component({
  selector: 'vitam-generic-table',
  templateUrl: './generic-table.component.html',
  styleUrls: ['./generic-table.component.css']
})
export class GenericTableComponent implements OnInit {
  @Input() items: any[] = [];
  @Input() cols: any[];
  @Input() route: string = null;
  @Input() colsWithIcon: any[];
  @Input() actionOnIcon = () => {};
  @Input() service: any;
  @Input() getClass: any;
  @Input() filter = 'filter';
  @Input() selectionMode: string = null;

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  navigateTo(item: any) {
    if (!!this.route) {
      this.router.navigate([this.route, item.value]);
    }
  }
}
