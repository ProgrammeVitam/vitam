import { ChangeDetectorRef, Component, Input, OnInit, SimpleChanges, HostListener, ViewChild } from '@angular/core';
import { ColumnDefinition } from '../generic-table/column-definition';
import { SelectItem } from 'primeng/primeng';
import { Hits, VitamResponse } from '../utils/response';
import { Observable } from 'rxjs/Observable';

@Component({
  selector: 'vitam-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css']
})
export class ResultsComponent implements OnInit {
  @Input() data: any;
  @Input() cols: ColumnDefinition[] = [];
  @Input() extraCols: ColumnDefinition[] = [];
  @Input() searchFunction: (service: any, offset: number, rows?: number, searchScope?: any) => Observable<VitamResponse>;
  @Input() path: string;
  @Input() identifier = '#id';
  @Input() getClass: () => string;
  @Input() service: any;
  @Input() searchForm: any;
  @Input() specificRowCss: (item, index) => string;
  @ViewChild('infoSupp') infoSuppElem;
  @ViewChild('infoList') infoListElem;
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

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!!this.data) {
      this.items = this.data.$results;
      this.firstItem = 0;
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
      if (this.extraCols.length === 0) {
        this.displayOptions = false;
      }
      this.extraColsSelection = this.extraCols.map((x) => ({ label: x.label, value: x }));
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
    this.extraColsSelection = this.extraCols.map((x) => ({ label: x.label, value: x }));
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

  @HostListener('document:click', ['$event', '$event.target'])
  clickOutside($event, targetElement) {
    this.displayOptions = ((this.infoSuppElem && this.infoSuppElem.nativeElement.contains(targetElement))
    || (this.infoListElem && this.infoListElem.nativeElement.contains(targetElement))) ? true : false;
  }



  onRowSelect() {
    this.changeDetectorRef.detectChanges();
    this.selectedCols = this.cols.concat(this.extraSelectedCols);
  }

  paginate(event) {
    this.firstItem = event.first;
    let page = event.page;

    // TODO If unloadedPage reached (See how to trigg) => Call search with offset.
    if (event.page >= this.lastPage || event.page <= this.firstPage) {
      var searchScope = { response: null };
      this.firstPage = page;
      this.searchFunction(this.service, this.firstItem, event.rows, searchScope).subscribe(
        (response) => {
          this.items = Array.from('x'.repeat(event.page * event.rows)).concat(response.$results);
          this.lastPage = event.page + this.hits.limit / event.rows;
          this.displayedItems = this.items.slice(event.first, event.first + event.rows);
        },
        (error) => console.log('Error: ', error.body));
    } else {
      this.displayedItems = this.items.slice(this.firstItem, this.firstItem + event.rows);
    }
  }
}
