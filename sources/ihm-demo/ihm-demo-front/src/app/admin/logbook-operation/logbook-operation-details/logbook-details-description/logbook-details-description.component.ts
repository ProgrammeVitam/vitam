import { ChangeDetectorRef, Component, Input, OnInit, SimpleChanges, HostListener, ViewChild } from '@angular/core';
import { SelectItem } from 'primeng/primeng';
import { ColumnDefinition } from '../../../../common/generic-table/column-definition';

@Component({
  selector: 'vitam-logbook-details-description',
  templateUrl: './logbook-details-description.component.html',
  styleUrls: ['./logbook-details-description.component.css']
})
export class LogbookDetailsDescriptionComponent implements OnInit {
  @Input() data: any;
  @Input() cols: ColumnDefinition[] = [];
  @Input() extraCols: ColumnDefinition[] = [];
  @Input() getClass: () => string;
  @Input() service: any;
  @Input() isIngestOperation: boolean;
  @ViewChild('infoSuppDetail') infoSuppDetailElem;
  @ViewChild('infoListDetail') infoListDetailElem;

  selectedCols: ColumnDefinition[] = [];
  extraColsSelection: SelectItem[];
  extraSelectedCols: ColumnDefinition[] = [];
  displayOptions = false;
  items: any[] = [];
  displayedItems: ColumnDefinition[] = [];
  path = '';
  identifier = '#id';

  constructor(private changeDetectorRef: ChangeDetectorRef) { }

  ngOnChanges(changes: SimpleChanges) {
    if (!!this.data) {
      this.items = this.data.$results;
      this.displayedItems = this.items;
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
      }
    }

  }

  ngOnInit() {
    this.selectedCols = this.cols;
    this.extraColsSelection = this.extraCols.map((x) => ({ label: x.label, value: x }));
    if (!!this.data) {
      this.items = this.data.$results;
    }
  }

  @HostListener('document:click', ['$event', '$event.target'])
  clickOutside($event, targetElement) {
    this.displayOptions = ((this.infoSuppDetailElem && this.infoSuppDetailElem.nativeElement.contains(targetElement))
      || (this.infoListDetailElem && this.infoListDetailElem.nativeElement.contains(targetElement))) ? true : false;
  }

  onRowSelect() {
    this.changeDetectorRef.detectChanges();
    this.selectedCols = this.cols.concat(this.extraSelectedCols);
  }
}
