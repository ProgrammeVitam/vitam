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
import {ActivatedRoute, Router} from '@angular/router';
import {DialogService} from '../../common/dialog/dialog.service';
import {SelectItem} from 'primeng/api';
import {ReferentialHelper} from '../../referentials/referential.helper';
import {ReferentialsService} from '../../referentials/referentials.service';
import {escape} from 'querystring';

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
  displayEliminationDateError = false;
  basketId: string;

  columns: ColumnDefinition[];

  selectedOption: string;
  form: any = {};
  basketOptions: SelectItem[] = [
    {label: 'Audit de cohérence', value: 'AUDIT'},
    {label: 'Élimination', value: 'ELIMINATION'},
    {label: 'Export DIP', value: 'EXPORT'},
    {label: 'Mise à jour de masse', value: 'MASS_UPDATE'},
    {label: 'Préservation ', value: 'PRESERVATION'},
    {label: 'Relevé de valeur probante ', value: 'PROBATIVE_VALUE'},
    {label: 'Vider le panier', value: 'DELETE'}
  ];

  frLocale = DateService.vitamFrLocale;

  nbRows = 25;
  firstItem = 0;
  hits: Hits;

  firstPage = 0;
  lastPage = 0;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, public activatedRoute: ActivatedRoute,
              public archiveUnitHelper: ArchiveUnitHelper, public mySelectionService: MySelectionService,
              public archiveUnitService: ArchiveUnitService, private router: Router, private dialogService: DialogService, public referentialsService: ReferentialsService) {
    super('Ma selection', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.columns = this.getColumns('EXPORT');

    this.activatedRoute.params.subscribe(
      params => {
        this.basketId = params['id'];
        this.mySelectionService.getResults(this.firstItem, 50, this.basketId).subscribe(
          (response: VitamResponse) => {
            this.selectedArchiveUnits = this.getFromResponse(response);
            this.hits = response.$hits;
            this.lastPage = this.hits.limit / this.nbRows;
            this.displayedItems = this.selectedArchiveUnits.slice(this.firstItem, this.firstItem + this.nbRows);
          }
        );
      });
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
    this.selectedArchiveUnits.map((x) => {
      x.selected = newValue;
      return x
    });
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
      // Delete selected items
      ids = this.displayedItems
        .filter(value => value.selected)
        .map(value => value.archiveUnitMetadata['#id']);
      this.mySelectionService.deleteFromBasket(ids);
    } else {
      // Delete all items in basket
      this.mySelectionService.deleteAllFromBasket();
    }

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
      case 'ELIMINATION':
        if (isOK) {
          title = 'Élimination en cours';
          message = 'L\'action d\'élimination (analyse ou action) est en cours sur les éléments du panier';
        } else {
          title = 'Erreur lors de l\'élimination';
          message = 'Erreur lors du lancement de l\'action d\'élimination (analyse ou action) des éléments du panier';
        }
        break;
      case 'MASS_UPDATE':
        if (isOK) {
          title = 'Mise à jour de masse';
          message = 'La mise à jour de masse des unités archivistiques du panier est en cours';
        } else {
          title = 'Erreur lors de la mise à jour de masse';
          message = 'Erreur lors du lancement de la mse à jour de masse des unités archivistiques du panier';
        }
        break;
      case 'PROBATIVE_VALUE':
        if (isOK) {
          title = 'Relevé de valeur probante en cours';
          message = 'Le relevé de valeur probante des unités archivistiques du panier est en cours';
        } else {
          title = 'Erreur lors de l\'export du relevé de valeur probante';
          message = 'Erreur lors du lancement de l\'export du relevé de valeur probante des unités archivistiques du panier';
        }
        break;
      case 'PRESERVATION':
        if (isOK) {
          title = 'Préservation';
          message = 'Le processus de préservation  est en cours';
        } else {
          title = 'Erreur de lancement du process de preservation';
          message = 'Erreur lors du lancement du process de preservation';
        }
        break;
      default:
        break;
    }

    this.dialogService.displayMessage(message, title);
  }

  getQuery(archiveUnits: ArchiveUnitSelection[]) {
    const {ids, roots} = archiveUnits.reduce(
      (finalIds: { ids: string[], roots: string[] }, currentArchiveUnit: ArchiveUnitSelection) => {
        if (currentArchiveUnit.haveChildren) {
          finalIds.roots.push(currentArchiveUnit.archiveUnitMetadata['#id']);
        }
        finalIds.ids.push(currentArchiveUnit.archiveUnitMetadata['#id']);
        return finalIds;
      }, {ids: [], roots: []});

    return {
      $query: [
        {
          $or: [
            {
              $in: {'#id': ids}
            },
            {
              $in: {'#allunitups': roots}
            }
          ]
        }
      ],
      $filter: {},
      $projection: {}
    };
  }

  actionOnBasket(isOnSelection: boolean = false) {
    if (this.selectedOption === 'DELETE') {
      this.doDelete(isOnSelection);
      return;
    }

    let query: any;
    if (isOnSelection) {
      // Action on all selected items
      const selection: ArchiveUnitSelection[] = this.displayedItems
        .filter(value => value.selected);
      if (selection.length === 0) {
        this.dialogService.displayMessage('L\'action n\'a pas été lancée sur la sélection car aucune archive n\'est sélectionnée', 'Sélection vide');
        return;
      }
      query = this.getQuery(selection);
    } else {
      // Action on all items in basket
      query = this.getQuery(this.selectedArchiveUnits);
    }

    switch (this.selectedOption) {
      case 'EXPORT':
        this.archiveUnitService.exportDIP(query, ReferentialHelper.optionLists.DataObjectVersion).subscribe(
          () => this.displayActionEnded(this.selectedOption, true),
          () => this.displayActionEnded(this.selectedOption, false)
        );
        break;
      case 'AUDIT':
        this.archiveUnitService.audit(query).subscribe(
          () => {
            this.displayActionEnded(this.selectedOption, true);
          }, () => {
            this.displayActionEnded(this.selectedOption, false);
          }
        );
        break;
      case 'ELIMINATION':
        if (this.checkInputs()) {
          let eliminationInfo = {
            query: query,
            date: DateService.dateToString(this.form.eliminationDate),
            threshold: this.form.eliminationThreshold,
            mode: this.form.eliminationMode
          };
          if (this.form.eliminationMode === false) {
            this.archiveUnitService.eliminationAnalysis(eliminationInfo).subscribe(
              () => {
                this.displayActionEnded(this.selectedOption, true);
              }, () => {
                this.displayActionEnded(this.selectedOption, false);
              }
            );
            this.displayActionEnded(this.selectedOption, true);
          } else {
            let isEliminationDateAfterToday = this.form.eliminationDate.getTime() > new Date().getTime();
            if (isEliminationDateAfterToday) {
              this.displayEliminationDateError = true;
              return;
            }
            this.archiveUnitService.eliminationAction(eliminationInfo).subscribe(
              () => {
                this.displayActionEnded(this.selectedOption, true);
              }, () => {
                this.displayActionEnded(this.selectedOption, false);
              }
            );
            this.displayActionEnded(this.selectedOption, true);
          }
        }
        break;
      case 'MASS_UPDATE':
        if (this.checkInputs()) {
          // TODO Launch massive Update

          let updateInfo = {
            query: query,
            rulesUpdates: this.form.updateRules,
            metadataUpdates: this.form.updateMetadata,
            threshold: this.form.updateThreshold
          };
          this.archiveUnitService.massUpdate(updateInfo).subscribe(
            () => {
              this.displayActionEnded(this.selectedOption, true);
            }, () => {
              this.displayActionEnded(this.selectedOption, false);
            }
          );
        }
        break;
      case 'PRESERVATION':
        if (this.checkInputs()) {
          let preservationRequest = {
            dslQuery: query,
            targetUsage: this.form.selectedTargetUse,
            sourceUsage: this.form.selectedSourceUse,
            version: this.form.seclectedVersion,
            scenarioId: this.form.selectedScenario
          };

          this.archiveUnitService.preservation(preservationRequest).subscribe(
            () => {
              this.displayActionEnded(this.selectedOption, true);
            }, () => {
              this.displayActionEnded(this.selectedOption, false);
            }
          );
        }
        break;
      case 'PROBATIVE_VALUE':
        this.archiveUnitService.probativeValue(query).subscribe(
          () => {
            this.displayActionEnded(this.selectedOption, true);
          }, () => {
            this.displayActionEnded(this.selectedOption, false);
          }
        );
        break;
      default:
        // TODO Display error ?
        console.log('No action selected');
    }
  }

  initForm() {
    switch (this.selectedOption) {
      case 'EXPORT':
      case 'AUDIT':
      case 'DELETE':
      case 'PROBATIVE_VALUE':
        return;
      case 'ELIMINATION':
        this.form.eliminationMode = false;
        this.form.eliminationDate = null;
        this.form.eliminationThreshold = null;
        break;
      case 'MASS_UPDATE':
        break;
      case 'PRESERVATION':
        this.form.preservationMode = false;
        this.form.targetUsage = ReferentialHelper.preservationLists.DataObjectVersion;
        this.form.sourceUsage = ReferentialHelper.preservationLists.DataObjectVersion;
        this.form.versions = [
          {label: '', value: undefined},
          {label: 'FIRST', value: 'FIRST'},
          {label: 'LAST', value: 'LAST'}
          ];
        this.form.scearioResults = [];

        let scenarioFilter = {
          ScenarioID: "all",
          ScenarioName: "all"
        };

        this.referentialsService.getScenarios(scenarioFilter).subscribe(
          (response) => {
            console.log('');
            if (response.httpCode == 200 && response.$results && response.$results.length > 0) {
              response.$results.forEach((scenario) => {

                this.form.scearioResults.push({label: scenario.Name, value: scenario.Identifier});
              });
            }
          }, function (error) {
            console.log('Error while get tenant. Set default list : ', error);
          });


        break;
      default:
        break;
    }
  }

  checkInputs(): boolean {
    switch (this.selectedOption) {
      case 'EXPORT':
      case 'AUDIT':
      case 'DELETE':
      case 'PROBATIVE_VALUE':
        return true;
      case 'ELIMINATION':
        return this.form.eliminationDate != null;
      case 'MASS_UPDATE':
        return this.form.updateRules || this.form.updateMetadata;
      case 'PRESERVATION':
        return this.form.selectedSourceUse != null && this.form.selectedTargetUse != null &&
          this.form.seclectedVersion != null && this.form.selectedScenario != null;
      default:
        return false;
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
          () => {
          }, null, false),
        ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], (x) => this.goToUnitLifecycles(x),
          () => true, () => ({'width': '75px'}), null, false)
      ];
    }
    return [];
  }
}
