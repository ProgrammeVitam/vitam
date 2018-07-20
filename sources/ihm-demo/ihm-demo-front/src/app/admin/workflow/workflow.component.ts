import {Component, EventEmitter, OnDestroy} from '@angular/core';
import {ColumnDefinition} from '../../common/generic-table/column-definition';
import {WorkflowService} from '../workflow.service';
import {VitamResponse} from '../../common/utils/response';
import {PageComponent} from '../../common/page/page-component';
import {Title} from '@angular/platform-browser';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {DateService} from '../../common/utils/date.service';
import {Preresult} from '../../common/preresult';
import {DynamicSelectItem, FieldDefinition} from '../../common/search/field-definition';
import {SelectItem} from 'primeng/primeng';
import {FormGroup} from '@angular/forms';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Gestion des opérations', routerLink: 'admin/workflow'}
];


@Component({
  selector: 'vitam-workflow',
  templateUrl: './workflow.component.html',
  styleUrls: ['./workflow.component.css']
})
export class WorkflowComponent extends PageComponent implements OnDestroy {


  public response: VitamResponse;
  public searchForm: any = {'states': ['PAUSE', 'RUNNING']};
  public service;
  public optionsCategories: SelectItem[];
  public optionsStatuses: SelectItem[] = [{
    label: 'Tous', value: ''
  }, {
    label: 'Succès', value: 'OK'
  }, {
    label: 'Avertissement', value: 'WARNING'
  }, {
    label: 'En cours', value: 'STARTED'
  }, {
    label: 'Échec', value: 'KO'
  }, {
    label: 'Erreur Technique', value: 'FATAL'
  }];
  public optionsStates: SelectItem[] = [{
    label: 'Tous', value: ''
  }, {
    label: 'Pause', value: 'PAUSE'
  }, {
    label: 'En cours', value: 'RUNNING'
  }, {
    label: 'Terminé', value: 'COMPLETED'
  }];
  public optionsWorkflowSteps: DynamicSelectItem[];
  public workflowData;
  private refreshWorkflow;

  columns = [
    ColumnDefinition.makeStaticColumn('operationId', 'Identifiant de la demande d\'entrée', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('processType', 'Catégorie de l\'opération', undefined,
      () => ({'width': '130px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('processDate', 'Date de l\'entrée', DateService.handleDateWithTime,
      () => ({'width': '105px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Mode d\'exécution', this.executionMode, undefined,
      () => ({'width': '100px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('globalState', 'Etat', undefined,
      () => ({'width': '150px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('stepStatus', 'Statut', undefined,
      () => ({'width': '100px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('previousStep', 'Etape en cours', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('nextStep', 'Prochaine étape', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialIconColumn('Action', this.getIconsByGlobalState,
      () => ({'width': '175px'}), this.onClickOnIcon, this.workflowService, false, undefined, this.getLabelIcon)
  ];

  extraColumns = [];

  static updateValues(allData: FieldDefinition[], searchForm: FormGroup): void {
    const updatingField: FieldDefinition[] = allData.filter((x) => 'steps' === x.name);

    if (updatingField && updatingField.length === 1) {
      updatingField[0].options = WorkflowComponent.computeSelectItems(updatingField[0].baseOptions, searchForm.value.categories);
    }
  }

  static computeSelectItems(items: DynamicSelectItem[], otherData: any): SelectItem[] {
    let filteredItems = [];
    const alreadyHereItems = [];
    if (otherData && otherData.length > 0 && otherData[0] !== '') {
      filteredItems = items.filter((x) => {
        if (alreadyHereItems.indexOf(x.value) === -1 && otherData.indexOf(x.data) !== -1) {
          alreadyHereItems.push(x.value);
          return true;
        }
        return false;
      });
    } else {
      filteredItems = items.filter((x) => {
        if (alreadyHereItems.indexOf(x.value) === -1) {
          alreadyHereItems.push(x.value);
          return true;
        }
        return false;
      });
    }

    return DynamicSelectItem.toSelectItems(filteredItems);
  }

  constructor(public workflowService: WorkflowService,
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Gestion des opérations', breadcrumb, titleService, breadcrumbService);
  }

  public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any) {
    service.getOperations(form).subscribe(
      (data) => {
        responseEvent.emit({response: data, form: form});
      }, (error) => console.error('Error: ', error)
    );
  }

  initSearchForm() {
    this.workflowService.getWorkflowsDefinition().subscribe(
      (data) => {

        this.optionsCategories = [{label: 'Tous', value: ''}];
        this.optionsWorkflowSteps = [new DynamicSelectItem('Tous', '', '')];
        for (let i = 0; i < data.$results.length; i++) {
          const workflow = data.$results[i];
          const item = {label: data.$results[i].name, value: data.$results[i].identifier};

          this.optionsCategories.push(item);
          for (const stepProp in workflow.steps) {
            if (workflow.steps.hasOwnProperty(stepProp)) {
              const step = workflow.steps[stepProp];
              // FIXME handle categories for change on step (parent: workflow.identifier)
              this.optionsWorkflowSteps.push(new DynamicSelectItem(step.stepName, step.stepName, workflow.identifier));
            }
          }
        }
        this.workflowData = [
          FieldDefinition.createIdField('id', 'Identifiant', 4, 12),
          FieldDefinition.createDateField('startDateMin', 'Date de début', 4, 8),
          FieldDefinition.createDateField('startDateMax', 'Date de fin', 4, 12),
          FieldDefinition.createSelectMultipleField('categories', 'Process', this.optionsCategories, 4, 12)
            .onChange(WorkflowComponent.updateValues),
          FieldDefinition.createSelectMultipleField('statuses', 'Statut', this.optionsStatuses, 2, 12),
          FieldDefinition.createSelectMultipleField('states', 'États', this.optionsStates, 2, 12),
          FieldDefinition.createDynamicSelectField('steps', 'Dernière étape', this.optionsWorkflowSteps, 'dynamicSelect',
            WorkflowComponent.computeSelectItems, 4, 12)
        ];
      }
    )
  }

  pageOnInit(): void {
    this.initSearchForm();
    this.workflowService.getOperations(this.searchForm).subscribe(
      data => this.response = data,
      () => console.error('Error - ', this.response));

    this.refreshWorkflow = setInterval(() => {
      this.refreshButton();
    }, 3000);
  }

  ngOnDestroy() {
    clearInterval(this.refreshWorkflow);
  }

  onNotify(event) {
    this.response = event.response;
    this.searchForm = event.form;
  }

  public refreshButton() {
    this.workflowService.getOperations(this.preSearchFunction(this.searchForm).request).subscribe(
      (data) => {
        this.response = data;
      }, (error) => console.error('Error: ', error)
    );
  }



  public preSearchFunction(request): Preresult {

    const preResult = new Preresult();

    delete request.orderby;

    // Handle id
    if (!!request.id) {
      delete request.categories;
      delete request.steps;
      delete request.startDateMax;
      delete request.startDateMin;
      delete request.states;
      delete request.statuses;
      delete request.categories;
      preResult.request = request;
      preResult.searchProcessSkip = false;
      preResult.success = true;
      return preResult;
    }
    delete request.id;

    // Handle workflows
    if (!!request.categories) {
      request.workflows = [];
      request.workflows = request.categories;
      if (request.categories.length === 1 && request.categories[0] === '') {
        delete request.categories;
        delete request.workflows;
      }
      delete request.categories;
    } else {
      delete request.workflows;
      delete request.categories;
    }

    // Handle steps
    if (!!request.steps) {
      request.listSteps = [];
      request.listSteps = request.steps;
      delete request.steps;
      if (request.listSteps.length === 1 && request.listSteps[0] === '') {
        delete request.listSteps;
      }
    } else {
      delete request.steps;
    }

    // Handle states
    if (!!request.states) {
      if (request.states.length === 1 && request.states[0] === '') {
        delete request.states;
      }
    } else {
      delete request.states;
    }

    // Handle statuses
    if (!!request.statuses) {
      if (request.statuses.length === 1 && request.statuses[0] === '') {
        delete request.statuses;
      }
    } else {
      delete request.statuses;
    }

    // Handle Dates and fix format of the date.
    if (!request.startDateMin) {
      delete request.startDateMin;
    } else {
      request.startDateMin = DateService.handleDate(request.startDateMin);
    }
    if (!request.startDateMax) {
      delete request.startDateMax;
    } else {
      request.startDateMax.setDate(request.startDateMax.getDate() + 1);
      request.startDateMax = DateService.handleDate(request.startDateMax);
    }

    preResult.request = request;
    preResult.searchProcessSkip = false;
    preResult.success = true;
    return preResult;
  }

  executionMode(item) {
    if (item.globalState === 'RUNNING') {
      if (item.stepByStep) {
        return 'Pas à pas';
      } else {
        return 'En continu';
      }
    } else {
      return '';
    }
  }


  getIconsByGlobalState(item) {
    switch (item.globalState) {
      case 'PAUSE':
        return ['fa-play fa-2x fa-pull-left', 'fa-forward fa-2x fa-pull-left',
          'fa-refresh fa-2x fa-pull-left', 'fa-stop fa-2x fa-pull-left'];
      case 'RUNNING':
        return ['fa-pause fa-2x fa-pull-left', 'fa-forward fa-2x fa-pull-left', 'fa-stop fa-2x fa-pull-left'];
      case 'COMPLETED':
        return [];
      default :
        // For the UNKNOWN case we don't know if it is still present.
        return ['fa-play fa-2x fa-pull-left', 'fa-forward fa-2x fa-pull-left',
          'fa-refresh fa-2x fa-pull-left', 'fa-stop fa-2x fa-pull-left'];
    }
  }

  onClickOnIcon(item, workflowService, iconType) {
    if (!!iconType) {
      iconType = iconType.split(' ')[0];
    }
    switch (iconType) {
      case 'fa-play' :
        workflowService.sendOperationsAction(item.operationId, {}, 'NEXT').subscribe(
          () => {}
        );
        break;
      case 'fa-forward':
        workflowService.sendOperationsAction(item.operationId, {}, 'RESUME').subscribe(
          () => {}
        );
        break;
      case 'fa-refresh':
        workflowService.sendOperationsAction(item.operationId, {}, 'REPLAY').subscribe(
          () => {}
        );
        break;
      case 'fa-stop':
        workflowService.stopOperation(item.operationId, {}).subscribe(
          () => {}
        );
        break;
      case 'fa-pause':
        workflowService.sendOperationsAction(item.operationId, {}, 'PAUSE').subscribe(
          () => {}
        );
        break;
    }
  }

  getLabelIcon(iconType) {
    if (!!iconType) {
      iconType = iconType.split(' ')[0];
    }
    switch (iconType) {
      case 'fa-play' :
        return 'Suivant';
      case 'fa-forward':
        return 'Terminer';
      case 'fa-refresh':
        return 'Rejouer';
      case 'fa-stop':
        return 'Annuler';
      case 'fa-pause':
        return 'Pause';
    }
  }

}
