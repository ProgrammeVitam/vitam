import { Component, EventEmitter } from '@angular/core';
import { PageComponent } from '../../common/page/page-component';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ObjectsGroupService } from './objectsgroup.service';
import { FieldDefinition } from '../../common/search/field-definition';
import { Preresult } from '../../common/preresult';
import { ColumnDefinition } from '../../common/generic-table/column-definition';
import { VitamResponse } from '../../common/utils/response';
import { ObjectsGroupHelper } from './objectsgroup.helper';
import { Router } from '@angular/router';
import { MySelectionService } from "../../my-selection/my-selection.service";
import { ResourcesService } from "../../common/resources.service";
import { DialogService } from '../../common/dialog/dialog.service';

const breadcrumb: BreadcrumbElement[] = [
{ label: 'Gestion des archives', routerLink: '' },
{ label: 'Groupes d\'objets', routerLink: 'admin/objectsgroup' }
];

@Component({
    selector: 'vitam-objectsgroup',
    templateUrl: './objectsgroup.component.html',
    styleUrls: ['./objectsgroup.component.css']
})
export class ObjectsGroupComponent extends PageComponent {
    public response: VitamResponse;
    public searchRequest: any = {};
    public searchForm: any = {};
    advancedMode = false;
    disabledFacet = false;
    public options = [
        {label: "<", value: "<"},
        {label: ">=", value: ">="},
    ];
    public advancedSearchFieldsGot = [
        new FieldDefinition('fileFormatId', 'Format', 6, 12),
        new FieldDefinition('fileUsage', 'Usage', 6, 12),
        FieldDefinition.createSelectField('fileSizeOperator', 'Opérateur', '', this.options, 2, 12),
        new FieldDefinition('fileSize', 'Volumétrie', 10, 12)
    ];

    public columns = [
        ColumnDefinition.makeSpecialValueColumn('Format (PUID)', this.objectsGroupHelper.getFormat, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Usage', this.objectsGroupHelper.getUsage, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Taille', this.objectsGroupHelper.getSize, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Intitulé AU', this.objectsGroupHelper.getAuTitles, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeStaticColumn('#originating_agency', 'Service producteur', undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false)
    ];

    public extraColumns = [];

    static addCriteriaProjection(criteriaSearch) {
        criteriaSearch.projection_formatid = '#qualifiers.versions.FormatIdentification.FormatId';
        criteriaSearch.projection_dataobjectversion = '#qualifiers.versions.DataObjectVersion';
        criteriaSearch.projection_dataobjectsize = '#qualifiers.versions.Size';
        criteriaSearch.projection_originatingagencies = '#originating_agency';
        criteriaSearch.projection_id = '#id';

        return criteriaSearch;
    }

    constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, public service: ObjectsGroupService,
                public objectsGroupHelper: ObjectsGroupHelper, private router: Router, private selectionService: MySelectionService,
                private resourceService: ResourcesService, public dialogService: DialogService) {
        super('Recherche de groupe d\'objets', breadcrumb, titleService, breadcrumbService);
    }

    pageOnInit() {
        if (ObjectsGroupService.getInputRequest() && ObjectsGroupService.getInputRequest().originatingagencies) {
            const form = ObjectsGroupComponent.addCriteriaProjection({});
            form.originatingagencies = ObjectsGroupService.getInputRequest().originatingagencies;
            form.isAdvancedSearchFlag = 'Yes';
            this.service.getResults(form, 0).subscribe(
                (response) => {
                    this.response = response;
                    this.advancedMode = true;
                },
                (error) => console.log('Error: ', error)
            );
            delete ObjectsGroupService.getInputRequest().originatingagencies;
        }
    }

    public preSearchFunction(request, advancedMode): Preresult {
        const criteriaSearch: any = {};
        const preResult = new Preresult();
        preResult.searchProcessSkip = false;

        if (!!request.requestFacet) {
            criteriaSearch.requestFacet = request.requestFacet;
        }

        if (request.fileFormatId) {
            criteriaSearch.fileFormatId = request.fileFormatId;
        }
        if (request.fileUsage) {
            criteriaSearch.fileUsage = request.fileUsage;
        }
        if (request.fileSize) {
            criteriaSearch.fileSize = request.fileSize;
            if(request.fileSizeOperator) {
                criteriaSearch.fileSizeOperator = request.fileSizeOperator;
            } else {
                criteriaSearch.fileSizeOperator = "<";
            }
        }

        if (criteriaSearch.fileFormatId || criteriaSearch.fileUsage || criteriaSearch.fileSize) {
            criteriaSearch.titleAndDescription = request.titleCriteria;
            if (!!request.facets) {
                criteriaSearch.facets = request.facets;
            }
            ObjectsGroupComponent.addCriteriaProjection(criteriaSearch);
            criteriaSearch.isAdvancedSearchFlag = 'Yes';
            preResult.request = criteriaSearch;
            preResult.success = true;
            return preResult;
        } else {
            preResult.searchProcessError = 'Aucun résultat. Veuillez entrer au moins un critère de recherche';
            return preResult;
        }
    }

    public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
        service.getResults(form, offset).subscribe(
            (response) => {
                responseEvent.emit({ response: response, form: form });
            },
            (error) => console.log('Error: ', error)
        );
    }

    onNotify(event) {
        this.response = event.response;
        this.searchForm = event.form;
    }

    onChangedSearchRequest(searchRequest) {
        this.searchRequest = searchRequest;
    }

    /**
     * clear results.
     */
    onClear() {
        delete this.response;
    }

    public paginationSearch(service: any, offset) {
        return service.getResults(this.searchForm, offset);
    }

    // FIXME: Unused method ?
    public onClearPressed() {
        delete this.response;
    }

    onChangedSearchMode(searchMode) {
        this.advancedMode = searchMode;
    }

    onDisabledFiled(isToDisable) {
        this.disabledFacet = isToDisable;
    }

}