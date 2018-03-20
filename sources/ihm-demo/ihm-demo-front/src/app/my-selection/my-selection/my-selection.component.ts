import {Component} from '@angular/core';
import {PageComponent} from '../../common/page/page-component';
import {Title} from '@angular/platform-browser';
import {plainToClass} from 'class-transformer';

import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {ArchiveUnitMetadata, ArchiveUnitSelection} from '../selection';
import {ColumnDefinition} from '../../common/generic-table/column-definition';
import {DateService} from '../../common/utils/date.service';
import {Hits, VitamResponse} from '../../common/utils/response';
import {ArchiveUnitHelper} from '../../archive-unit/archive-unit.helper';
import {MySelectionService} from '../my-selection.service';

const breadcrumb: BreadcrumbElement[] = [
    {label: 'Panier', routerLink: 'mySelection'}
];

@Component({
  selector: 'vitam-my-selection',
  templateUrl: './my-selection.component.html',
  styleUrls: ['./my-selection.component.css']
})
export class MySelectionComponent extends PageComponent {
  selectedArchiveUnits: ArchiveUnitSelection[] = [];
  displayedItems: ArchiveUnitSelection[] = [];

  manualColumns: ColumnDefinition[] = [
    ColumnDefinition.makeSpecialValueColumn('Intitulé',
      (x: ArchiveUnitSelection) => x.archiveUnitMetadata.Title, undefined,
      () => ({'width': '325px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Type',
      (x: ArchiveUnitSelection) => x.archiveUnitMetadata['#unitType'], this.archiveUnitHelper.transformType,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de début',
      (x: ArchiveUnitSelection) => x.archiveUnitMetadata.StartDate, DateService.handleDate,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de fin',
      (x: ArchiveUnitSelection) => x.archiveUnitMetadata.EndDate, DateService.handleDate,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeSpecialIconColumn('Objet(s) disponible(s)',
      (x: ArchiveUnitSelection) => x.archiveUnitMetadata['#object'] ? ['fa-check'] : ['fa-close greyColor'], () => ({'width': '150px'}),
      () => {}, null, false),
    ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], undefined,
      () => true, () => ({'width': '100px'}), null, false)
  ];

  nbRows = 25;
  firstItem = 0;
  hits: Hits;

  firstPage = 0;
  lastPage = 0;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService,
              public archiveUnitHelper: ArchiveUnitHelper, public mySelectionService: MySelectionService) {
      super('Ma selection', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.mySelectionService.getResults(this.firstItem, 50).subscribe(
      (response: VitamResponse) => {
        this.selectedArchiveUnits = this.getFromResponse(response);
        this.hits = response.$hits;
        this.lastPage = this.hits.limit / this.nbRows;
        this.displayedItems = this.selectedArchiveUnits.slice(this.firstItem, this.firstItem + this.nbRows);
      }
    );

  }

  isAllChecked(): boolean {
    return this.selectedArchiveUnits.find((x) => !x.selected) === undefined;
  }

  isAllCheckedLabel(): string {
    if (this.isAllChecked()) {
      return 'fa fa-check';
    }
    const icon = 'fa fa-close';
    return this.selectedArchiveUnits.find((x) => x.selected) === undefined ? icon : icon + ' greyColor';
  }

  countSelected(): number {
    return this.selectedArchiveUnits.reduce((selectedNumber, currentElement) => selectedNumber + (currentElement.selected ? 1 : 0), 0);
  }

  checkAll(): void {
    const newValue = !this.isAllChecked();
    this.selectedArchiveUnits.map((x) => {x.selected = newValue; return x});
  }

  inverseSelection(item: ArchiveUnitSelection): void {
    item.selected = !item.selected;
  }

  isCheckedLabel(item: ArchiveUnitSelection): string {
    return item.selected ? 'fa fa-check' : 'fa fa-close greyColor';
  }

  deleteSelected() {
    const ids: string[] = this.displayedItems
      .filter(value => value.selected)
      .map(value => value.archiveUnitMetadata['#id']);
    this.mySelectionService.deleteFromBasket(ids);
    this.mySelectionService.getResults(this.firstItem, 50).subscribe(
      (response: VitamResponse) => {
        this.selectedArchiveUnits = this.getFromResponse(response);
        this.hits = response.$hits;
        this.lastPage = this.hits.limit / this.nbRows;
        this.displayedItems = this.selectedArchiveUnits.slice(this.firstItem, this.firstItem + this.nbRows);
      }
    );
  }

  getRootIcon(item: ArchiveUnitSelection): string {
    if (!item.haveChildren) {
      return '';
    }
    return item.displayChildren ? 'fa fa-minus' : 'fa fa-plus';
  }

  showHideChildren(item: ArchiveUnitSelection, index: number) {
    item.displayChildren = !item.displayChildren;
    if (item.displayChildren === true) {
      // Add Children
      if (item.children.length === 0) {
        this.mySelectionService.getChildren(item.archiveUnitMetadata['#id']).subscribe(
          (response) => {
            item.children = this.getFromResponse(response);
            item.children.forEach((x) => x.isChild = true);
            this.displayedItems.splice(index + 1, 0, ...item.children);
          }
        );
        return;
      }
      this.displayedItems.splice(index + 1, 0, ...item.children);
    } else {
      // Remove children
      this.displayedItems.splice(index + 1, item.children.length);
    }
  }

  getFromResponse(response: VitamResponse): ArchiveUnitSelection[] {
    const metadata: ArchiveUnitMetadata[] = plainToClass(ArchiveUnitMetadata, response.$results);
    return metadata.map(
      (item) => {
        return new ArchiveUnitSelection(item, this.mySelectionService.haveChildren(item['#id']));
      }
    );
  }

  hideAllChildrenInPage() {
    for (let i = this.displayedItems.length - 1; i >= 0; i--) {
      if (this.displayedItems[i].displayChildren) {
        this.showHideChildren(this.displayedItems[i], i);
      }
    }
  }

  paginate(event) {
    this.firstItem = event.first;
    const page = event.page;

    this.hideAllChildrenInPage();

    // TODO If unloadedPage reached (See how to trigg) => Call search with offset.
    if (event.page >= this.lastPage || event.page <= this.firstPage) {
      this.firstPage = page;
      this.mySelectionService.getResults(this.firstItem, event.rows).subscribe(
        (response) => {
          const filler: any[] = Array.from('x'.repeat(event.page * event.rows));
          this.selectedArchiveUnits = filler.concat(this.getFromResponse(response));
          this.lastPage = event.page + this.hits.limit / event.rows;
          this.displayedItems = this.selectedArchiveUnits.slice(event.first, event.first + event.rows);
        },
        (error) => console.log('Error: ', error.body));
    } else {
      this.displayedItems = this.selectedArchiveUnits.slice(this.firstItem, this.firstItem + event.rows);
    }
  }

  exportBasket() {
    // FIXME: To do!
    console.log(`Export ${this.displayedItems.length} items`);
  }

}
