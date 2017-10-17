import {ChangeDetectorRef, Component, Input, OnInit, SimpleChanges} from '@angular/core';
import {ColumnDefinition} from '../generic-table/column-definition';
import {SelectItem} from 'primeng/primeng';
import {Hits} from "../utils/response";

@Component({
  selector: 'vitam-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css']
})
export class ResultsComponent implements OnInit {
  @Input() data: any;
  @Input() cols: ColumnDefinition[] = [];
  @Input() extraCols: ColumnDefinition[] = [];
  @Input() searchFunction;
  @Input() path: string;
  @Input() identifier = "#id";
  @Input() getClass: () => string;
  @Input() service: any;
  @Input() searchForm: any;

  errorOnResults = false;

  selectedCols: ColumnDefinition[] = [];
  extraColsSelection: SelectItem[];
  extraSelectedCols: ColumnDefinition[] = [];
  displayOptions = false;
  items: any[] = [];
  hits: Hits;
  displayedItems: ColumnDefinition[] = [];
  nbRows = 25;
  firstItem = 0;

  firstPage = 0;
  lastPage = 0;

  constructor(private changeDetectorRef: ChangeDetectorRef) { }

  ngOnChanges(changes: SimpleChanges) {
    if (!!this.data) {
      this.items = this.data.$results;
      this.hits = this.data.$hits;
      this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
      this.firstPage = 0;
      this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
      this.errorOnResults = false;
    } else {
      this.errorOnResults = true;
    }

    if (changes.cols) {
      this.selectedCols = changes.cols.currentValue;
    }

    if (changes.extraCols) {
      this.extraSelectedCols = [];
      if (this.extraCols.length == 0) {
        this.displayOptions = false;
      }
      this.extraColsSelection = this.extraCols.map((x) => ({label: x.label, value: x}));
      if (!!this.data) {
        this.items = this.data.$results;
        this.hits = this.data.$hits;
        this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
        this.firstPage = 0;
        this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
        this.errorOnResults = false;
      } else {
        this.errorOnResults = true;
      }
    }

  }

  ngOnInit() {
    this.selectedCols = this.cols;
    this.extraColsSelection = this.extraCols.map((x) => ({label: x.label, value: x}));
    if (!!this.data) {
      this.items = this.data.$results;
      this.hits = this.data.$hits;
      this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
      this.firstPage = 0;
      this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
      this.errorOnResults = false;
    } else {
      this.errorOnResults = true;
    }
  }

  onRowSelect() {
    this.changeDetectorRef.detectChanges();
    this.selectedCols = this.cols.concat(this.extraSelectedCols);
  }

  paginate(event) {
    this.firstItem = event.first;
    // TODO If unloadedPage reached (See how to trigg) => Call search with offset.
    if (event.page >= this.lastPage || event.page <= this.firstPage) {
      var searchScope = {response: null};
      this.searchFunction(this.service, this.firstItem, this.nbRows, searchScope).subscribe(
        (response) => {
          response.$hits.offset = this.firstItem;
          this.items = Array.from('x'.repeat(event.page * this.nbRows)).concat(response.$results);
          this.firstPage = event.page;
          this.lastPage = this.firstPage + this.hits.limit / this.nbRows;
          this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
        },
        (error) => console.log('Error: ', error._body));
    } else {
      this.displayedItems = this.items.slice(this.firstItem, this.firstItem + this.nbRows);
    }
  }
}
