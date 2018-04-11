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
import {ArchiveUnitService} from '../../archive-unit/archive-unit.service';
import {Router} from '@angular/router';
import {DialogService} from '../../common/dialog/dialog.service';
import {SelectItem} from 'primeng/api';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Panier', routerLink: 'basket'}
];

@Component({
  selector: 'vitam-my-selection',
  templateUrl: './my-selection.component.html',
  styleUrls: ['./my-selection.component.css']
})
export class MySelectionComponent extends PageComponent {
  selectedArchiveUnits: ArchiveUnitSelection[] = [];
  displayedItems: ArchiveUnitSelection[] = [];
  displaySelectedDelete = false;
  displayDeleteAll = false;

  columns: ColumnDefinition[];

  selectedOption: string;
  basketOptions: SelectItem[] = [
    { label: 'Export DIP', value: 'EXPORT' },
    { label: 'Audit de cohérence', value: 'AUDIT' },
    { label: 'Vider le panier', value: 'DELETE' }
  ];

  nbRows = 25;
  firstItem = 0;
  hits: Hits;

  firstPage = 0;
  lastPage = 0;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService,
              public archiveUnitHelper: ArchiveUnitHelper, public mySelectionService: MySelectionService,
              public archiveUnitService: ArchiveUnitService, private router: Router, private dialogService: DialogService) {
    super('Ma selection', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.columns = this.getColumns('EXPORT');
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
    return item.selected ? 'fa fa-check clickableDiv' : 'fa fa-close greyColor clickableDiv';
  }

  getRootIcon(item: ArchiveUnitSelection): string {
    if (!item.haveChildren) {
      return '';
    }
    return item.displayChildren ? 'fa fa-minus clickableDiv' : 'fa fa-plus clickableDiv';
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
    const results = response.$results.map((x) => {
      x.StartDate = this.archiveUnitHelper.getStartDate(x);
      x.EndDate = this.archiveUnitHelper.getEndDate(x);
      if (!x.Title && x.Title_) {
        for (const prop in x.Title_) {
          if (x.Title_.hasOwnProperty(prop)) {
            x.Title = x.Title_[prop];
            break;
          }
        }
      }
      return x;
    });

    const metadata: ArchiveUnitMetadata[] = plainToClass(ArchiveUnitMetadata, results);
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
      this.mySelectionService.getResults(this.firstItem).subscribe(
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

  doDelete(deleteSelection: boolean = false) {
    let ids = [];

    if (deleteSelection) {
      // Export all selected items
      ids = this.displayedItems
        .filter(value => value.selected)
        .map(value => value.archiveUnitMetadata['#id']);
    } else {
      // Export all items in basket
      ids = this.selectedArchiveUnits
        .map(value => value.archiveUnitMetadata['#id']);
    }

    this.mySelectionService.deleteFromBasket(ids);
    this.displaySelectedDelete = false;
    this.displayDeleteAll = false;
    this.mySelectionService.getResults(this.firstItem, 50).subscribe(
      (response: VitamResponse) => {
        this.selectedArchiveUnits = this.getFromResponse(response);
        this.hits = response.$hits;
        this.lastPage = this.hits.limit / this.nbRows;
        this.displayedItems = this.selectedArchiveUnits.slice(this.firstItem, this.firstItem + this.nbRows);
        this.displayActionEnded('DELETE', true);
      }, () => {
        this.displayActionEnded('DELETE', false);
      }
    );
  }

  displayActionEnded(action: string, isOK: boolean) {
    let message = '';
    let title = '';
    switch (action) {
      case 'DELETE':
        if (isOK) {
          title = 'Succès de la suppression';
          message = 'Les unités archivistiques ont bien été supprimées du panier';
        } else {
          title = 'Erreur lors de la suppression';
          message = 'Erreur lors de la suppression des unités archivistiques du panier';
        }
        break;
      case 'EXPORT':
        if (isOK) {
          title = 'Export en cours';
          message = 'L\'export DIP des unités archivistiques du panier est en cours';
        } else {
          title = 'Erreur lors de l\'export';
          message = 'Erreur lors du lancement de l\'export des unités archivistiques du panier';
        }
        break;
      case 'AUDIT':
        if (isOK) {
          title = 'Audit en cours';
          message = 'L\'audit de cohérence des unités archivistiques du panier est en cours';
        } else {
          title = 'Erreur lors de l\'audit';
          message = 'Erreur lors du lancement de l\'audit de cohérence des unités archivistiques du panier';
        }
        break;
      default:
        break;
    }

    this.dialogService.displayMessage(message, title);
  }

  getQuery(archiveUnits: ArchiveUnitSelection[]) {
    const {ids, roots} = archiveUnits.reduce(
      (finalIds: {ids: string[], roots: string[]}, currentArchiveUnit: ArchiveUnitSelection) => {
        if (currentArchiveUnit.haveChildren) {
          finalIds.roots.push(currentArchiveUnit.archiveUnitMetadata['#id']);
        }
        finalIds.ids.push(currentArchiveUnit.archiveUnitMetadata['#id']);
        return finalIds;
      }, {ids: [], roots: []});

    return {
      '$query': [
        {
          '$or': [
            {
              '$in': {
                '#id': ids
              }
            },
            {
              '$in': {
                '#allunitups': roots
              }
            }
          ]
        }
      ],
      '$filter': {},
      '$projection': {}
    };
  }

  actionOnBasket(isOnSelection: boolean = false, selectedOption) {
    if (selectedOption === 'DELETE') {
      this.doDelete(isOnSelection);
      return;
    }

    let query: any;
    if (isOnSelection) {
      // Action on all selected items
      const selection: ArchiveUnitSelection[] = this.displayedItems
        .filter(value => value.selected);
      query = this.getQuery(selection);
    } else {
      // Action on all items in basket
      query = this.getQuery(this.selectedArchiveUnits);
    }

    if (selectedOption === 'EXPORT') {
      this.archiveUnitService.exportDIP(query).subscribe(
        () => {
          this.displayActionEnded(selectedOption, true);
        }, () => {
          this.displayActionEnded(selectedOption, false);
        }
      );
    } else {
      this.archiveUnitService.audit(query).subscribe(
        () => {
          this.displayActionEnded(selectedOption, true);
        }, () => {
          this.displayActionEnded(selectedOption, false);
        }
      );
    }
  }

  clickable(col: ColumnDefinition): string {
    return col.icons.length ? '' : 'clickableDiv';
  }

  navigateTo(event) {
    const htmlPath = event.originalEvent.target;
    if (htmlPath.tagName === 'SPAN' ||
      (htmlPath.tagName === 'TD' && htmlPath.getElementsByTagName('i').length === 0)) {
      this.router.navigate(['/search/archiveUnit', event.data.archiveUnitMetadata['#id']]);
    }
  }

  goToAUSearch() {
    this.router.navigate(['search/archiveUnit']);
  }

  goToUnitLifecycles(item: ArchiveUnitSelection) {
    this.router.navigate([`search/archiveUnit/${item.archiveUnitMetadata['#id']}/unitlifecycle`]);
  }

  // 1st step to specific basket with different colomn and actions
  getColumns(type: string): ColumnDefinition[] {
    if (type === 'EXPORT') {
      return [
        ColumnDefinition.makeSpecialValueColumn('Intitulé',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata.Title, undefined,
          () => ({'width': '325px', 'overflow-wrap': 'break-word'}), false),
        ColumnDefinition.makeSpecialValueColumn('Service producteur',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata['#originating_agency'], undefined,
          () => ({'width': '200px', 'overflow-wrap': 'break-word'}), false),
        ColumnDefinition.makeSpecialValueColumn('Type',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata['#unitType'], this.archiveUnitHelper.transformType,
          () => ({'width': '100px'}), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus ancienne',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata.StartDate, DateService.handleDate,
          () => ({'width': '100px'}), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus récente',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata.EndDate, DateService.handleDate,
          () => ({'width': '100px'}), false),
        ColumnDefinition.makeSpecialIconColumn('Objet(s) disponible(s)',
          (x: ArchiveUnitSelection) => x.archiveUnitMetadata['#object'] ? ['fa-check'] : ['fa-close greyColor'], () => ({'width': '150px'}),
          () => {}, null, false),
        ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], (x) => this.goToUnitLifecycles(x),
          () => true, () => ({'width': '75px'}), null, false)
      ];
    }
    return [];
  }
}
