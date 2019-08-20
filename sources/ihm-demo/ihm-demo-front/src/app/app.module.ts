import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CookieService} from 'angular2-cookie/core';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {TranslateModule, TranslateLoader} from '@ngx-translate/core';

import {
  AccordionModule, ButtonModule, CalendarModule, MenubarModule, BreadcrumbModule, DropdownModule,
  ProgressBarModule, PaginatorModule, PanelModule, ListboxModule, GrowlModule, RadioButtonModule, TabViewModule,
  InputTextModule, DataTableModule, SharedModule, DialogModule, FieldsetModule, ToggleButtonModule,
  ConfirmDialogModule, ConfirmationService, OverlayPanelModule, InputSwitchModule, ChipsModule, MultiSelectModule,
  CheckboxModule, DataGridModule, SliderModule, TriStateCheckboxModule, SpinnerModule
} from 'primeng/primeng';
import {AppComponent} from './app.component';
import {MenuComponent} from './common/menu/menu.component';
import {BreadcrumbComponent} from './common/breadcrumb/breadcrumb.component';
import {ArchiveUnitHelper} from './archive-unit/archive-unit.helper';
import {ObjectsGroupHelper} from './admin/objectsgroup/objectsgroup.helper';
import {ReferentialHelper} from './referentials/referential.helper';
import {ResourcesService} from './common/resources.service';
import {BreadcrumbService} from './common/breadcrumb.service';
import {IngestUtilsService} from './common/utils/ingest-utils.service';
import {LogbookService} from './ingest/logbook.service';
import {IngestService} from './ingest/ingest.service';
import {HomeComponent} from './home/home.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {FileDropModule} from 'angular2-file-drop';

import { LogbookComponent } from './ingest/logbook/logbook.component';
import { ResultsComponent } from './common/results/results.component';
import { MetadataFieldComponent } from './common/metadata-field/metadata-field.component';
import { GenericTableComponent } from './common/generic-table/generic-table.component';
import { LogbookDetailsComponent } from './ingest/logbook/logbook-details/logbook-details.component';
import { UploadService } from './common/upload/upload.service';
import { SipComponent } from './ingest/sip/sip.component';
import { AuthenticationComponent } from './authentication/authentication.component';
import { AuthenticationService } from './authentication/authentication.service';
import { SearchComponent } from './common/search/search.component';
import { LogbookOperationComponent } from './admin/logbook-operation/logbook-operation.component';
import { LogbookOperationDetailsComponent } from './admin/logbook-operation/logbook-operation-details/logbook-operation-details.component';
import { LogbookOperationEventsComponent } from './common/logbook-operation-events/logbook-operation-events.component';
import { EventDisplayComponent } from './common/logbook-operation-events/event-display/event-display.component';
import { ArchiveUnitComponent } from './archive-unit/archive-unit-search/archive-unit.component';
import { ArchiveUnitDetailsComponent } from './archive-unit/archive-unit-details/archive-unit-details.component';
import { ArchiveMainDescriptionComponent } from './archive-unit/archive-unit-details/archive-main-description/archive-main-description.component';
import { ArchiveExtraDescriptionComponent } from './archive-unit/archive-unit-details/archive-extra-description/archive-extra-description.component';
import { ArchiveObjectGroupComponent } from './archive-unit/archive-unit-details/archive-object-group/archive-object-group.component';
import { ArchiveTreeViewComponent } from './archive-unit/archive-unit-details/archive-tree-view/archive-tree-view.component';
import { KeysPipe, BytesPipe } from './common/utils/pipes';
import { DateService } from './common/utils/date.service';
import { ObjectsService } from './common/utils/objects.service';
import { ArchiveUnitService } from './archive-unit/archive-unit.service';
import { ImportComponent } from './referentials/import/import.component';
import { SearchReferentialsComponent } from './referentials/search-referentials/search-referentials.component';
import { ReferentialsService } from './referentials/referentials.service';
import { UploadReferentialsComponent } from './common/upload/upload-referentials/upload-referentials.component';
import { UploadSipComponent } from './common/upload/upload-sip/upload-sip.component';
import { AccessContractService } from './common/access-contract.service';
import { FormatComponent } from './referentials/details/format/format.component';
import { RuleComponent } from './referentials/details/rule/rule.component';
import { AccessContractComponent } from './referentials/details/access-contract/access-contract.component';
import { IngestContractComponent } from './referentials/details/ingest-contract/ingest-contract.component';
import { ManagementContractComponent } from './referentials/details/management-contract/management-contract.component';
import { ProfilComponent } from './referentials/details/profil/profil.component';
import { ContextComponent } from './referentials/details/context/context.component';
import { TreeParentComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-parent/tree-parent.component';
import { TreeChildComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-child/tree-child.component';
import { TreeSearchComponent } from './archive-unit/archive-unit-details/archive-tree-view/tree-search/tree-search.component';
import { AgenciesComponent } from './referentials/details/agencies/agencies.component';
import { AuditComponent } from './admin/audit/audit.component';
import { AuditService } from './admin/audit/audit.service';
import {ObjectsGroupComponent} from './admin/objectsgroup/objectsgroup.component';
import {ObjectsGroupService} from './admin/objectsgroup/objectsgroup.service';
import {AccessionRegisterSearchComponent} from './admin/accession-register/accession-register.component';
import {LogbookDetailsDescriptionComponent} from './admin/logbook-operation/logbook-operation-details/logbook-details-description/logbook-details-description.component';
import {LogbookHelperService} from './common/logbook-operation-events/logbook-helper.service';
import {AccessionRegisterComponent} from './referentials/details/accession-register/accession-register.component';
import {OperationComponent} from './admin/traceability/operation/operation.component';
import {HoldingschemeComponent} from './admin/holdingscheme/holdingscheme.component';
import {ArchiveExportDIPComponent} from './archive-unit/archive-unit-details/archive-export-dip/archive-export-dip.component';
import {DialogComponent} from './common/dialog/dialog.component';
import {DialogService} from './common/dialog/dialog.service';
import {VitamInterceptor} from './common/http-interceptor';
import {LifecycleComponent} from './archive-unit/archive-unit-details/lifecycle/lifecycle.component';
import {CustomLoader} from './common/translate/custom-loader';
import {TraceabilityOperationDetailsComponent} from './admin/traceability/traceability-operation-details/traceability-operation-details.component';
import {TraceabilityOperationService} from './admin/traceability/traceability-operation.service';
import {WorkflowComponent} from './admin/workflow/workflow.component';
import {WorkflowService} from './admin/workflow.service';
import {ErrorService} from './common/error.service';
import {LoadingBlockComponent} from './common/loading-block/loading-block.component';
import {ArchiveUnitProfileComponent} from './referentials/details/archive-unit-profile/archive-unit-profile.component';
import {OntologyComponent} from './referentials/details/ontology/ontology.component';
import {MySelectionComponent} from './my-selection/my-selection/my-selection.component';
import {MySelectionService} from './my-selection/my-selection.service';
import {ArchiveUnitSelectionComponent} from './archive-unit/archive-unit-details/archive-unit-selection/archive-unit-selection.component';
import {ScrollPanelModule} from 'primeng/scrollpanel';
import {MessagesUtilsService} from './common/utils/messages-utils.service';
import {EliminationSearchComponent} from './elimination-search/elimination-search.component';
import {FacetsComponent} from './common/facets/facets.component';
import {ArchiveRuleBlocComponent} from './archive-unit/archive-unit-details/archive-rule-bloc/archive-rule-bloc.component';
import {ComputeRulesUtilsService} from './archive-unit/archive-unit-details/archive-rule-bloc/compute-rules-utils.service';
import {RulesDisplayModeComponent} from './archive-unit/archive-unit-details/archive-rule-bloc/rules-display-mode/rules-display-mode.component';
import {RulesUpdateModeComponent} from './archive-unit/archive-unit-details/archive-rule-bloc/rules-update-mode/rules-update-mode.component';
import {NgArrayPipesModule} from 'ngx-pipes';
import {MassiveUpdateFormComponent} from './my-selection/my-selection/massive-update-form/massive-update-form.component';
import {ArchiveUnitFacetComponent} from './archive-unit/archive-unit-facet/archive-unit-facet.component';
import {ObjectsGroupFacetComponent} from './admin/objectsgroup/objectsgroup-facet/objectsgroup-facet.component';
import {AccessionRegisterSymbolicComponent} from './referentials/details/accession-register-symbolic/accession-register-symbolic.component';
import {GriffinsComponent} from './referentials/details/griffins/griffins.component';
import {ScenariosComponent} from './referentials/details/scenarios/scenarios.component';

const appRoutes: Routes = [
  {
    path: 'home', component: HomeComponent
  },
  {
    path: 'search/archiveUnit', component: ArchiveUnitComponent, data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'basket', component: MySelectionComponent, data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'basket/:id', component: MySelectionComponent, data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'search/archiveUnit/:id',
    component: ArchiveUnitDetailsComponent,
    data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'search/archiveUnit/:id/:lifecycleType',
    component: LifecycleComponent,
    data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'search/archiveUnit/:unitId/:lifecycleType/:id',
    component: LifecycleComponent,
    data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'login', component: AuthenticationComponent
  },
  {
    path: 'ingest/logbook', component: LogbookComponent, data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'ingest/logbook/:id', component: LogbookDetailsComponent, data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'ingest/sip', component: SipComponent, data: {permission: 'ingest:create'}
  },
  {
    path: 'admin/logbookOperation', component: LogbookOperationComponent, data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'admin/logbookOperation/:id',
    component: LogbookOperationDetailsComponent,
    data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'admin/holdingScheme', component: HoldingschemeComponent, data: {permission: 'format:create'}
  },
  {
    path: 'admin/traceabilityOperation', component: OperationComponent, data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'admin/traceabilityOperation/:id', component: TraceabilityOperationDetailsComponent,
    data: {permission: 'logbook:operations:read'}
  },
  {
    path: 'admin/import/:referentialType', component: ImportComponent, data: {permission: 'format:create'}
  },
  {
    path: 'admin/format/:id', component: FormatComponent, data: {permission: 'admin:formats:read'}
  },
  {
    path: 'admin/rule/:id', component: RuleComponent, data: {permission: 'admin:rules:read'}
  },
  {
    path: 'admin/accessContract/:id', component: AccessContractComponent, data: {permission: 'accesscontracts:read'}
  },
  {
    path: 'admin/ingestContract/:id', component: IngestContractComponent, data: {permission: 'contracts:read'}
  },
  {
    path: 'admin/managementContract/:id', component: ManagementContractComponent, data: {permission: 'managementcontracts:read'}
  },
  {
    path: 'admin/profil/:id', component: ProfilComponent, data: {permission: 'profiles:read'}
  },
  {
    path: 'admin/archiveUnitProfile/:id',
    component: ArchiveUnitProfileComponent,
    data: {permission: 'archiveunitprofiles:read'}
  },
  {
    path: 'admin/ontologies/:id', component: OntologyComponent, data: {permission: 'ontologies:read'}
  },
  {
    path: 'admin/context/:id', component: ContextComponent, data: {permission: 'contexts:read'}
  },
  {
    path: 'admin/agencies/:type/:id', component: AgenciesComponent, data: {permission: 'admin:accession-register:read'}
  },
  {
    path: 'admin/griffins/:id',component: GriffinsComponent, data: {permission: 'griffin:read'}
  },
  {
    path: 'admin/scenarios/:id',component: ScenariosComponent, data: {permission: 'preservationScenario:read'}
  },

  {
    path: 'admin/accessionRegister',
    component: AccessionRegisterSearchComponent,
    data: {permission: 'admin:accession-register:read'}
  },
  {
    path: 'admin/accessionRegister/symbolic/:id',
    component: AccessionRegisterSymbolicComponent,
    data: {permission: 'admin:accession-register:read'}
  },
  {
    path: 'admin/accessionRegister/:type/:id',
    component: AccessionRegisterComponent,
    data: {permission: 'admin:accession-register:read'}
  },
  {
    path: 'admin/search/:referentialType',
    component: SearchReferentialsComponent,
    data: {permission: 'admin:formats:read'}
  },
  {
    path: 'admin/audits', component: AuditComponent, data: {permission: 'admin:audit'}
  },
  {
    path: 'admin/objectsgroup', component: ObjectsGroupComponent, data: {permission: 'admin:audit'}
  },
  {
    path: 'archiveManagement/eliminationSearch', component: EliminationSearchComponent, data: {permission: 'archivesearch:units:read'}
  },
  {
    path: 'admin/workflow', component: WorkflowComponent, data: {permission: 'admin:audit'}
  },
  {
    path: '**', redirectTo: 'ingest/sip', pathMatch: 'full'
  }
];


@NgModule({
  declarations: [
    AppComponent,
    MenuComponent,
    BreadcrumbComponent,
    HomeComponent,
    LogbookComponent,
    ResultsComponent,
    GenericTableComponent,
    SipComponent,
    AuthenticationComponent,
    SearchComponent,
    LogbookDetailsComponent,
    LogbookOperationComponent,
    LogbookOperationDetailsComponent,
    LogbookOperationEventsComponent,
    EventDisplayComponent,
    ArchiveUnitComponent,
    ArchiveUnitDetailsComponent,
    ArchiveMainDescriptionComponent,
    ArchiveExtraDescriptionComponent,
    ArchiveObjectGroupComponent,
    ArchiveTreeViewComponent,
    KeysPipe,
    BytesPipe,
    LogbookDetailsComponent,
    SipComponent,
    AuthenticationComponent,
    SearchComponent,
    ImportComponent,
    SearchReferentialsComponent,
    UploadReferentialsComponent,
    UploadSipComponent,
    FormatComponent,
    RuleComponent,
    AccessContractComponent,
    IngestContractComponent,
    ManagementContractComponent,
    ProfilComponent,
    ArchiveUnitProfileComponent,
    OntologyComponent,
    ContextComponent,
    MetadataFieldComponent,
    TreeParentComponent,
    TreeChildComponent,
    TreeSearchComponent,
    AgenciesComponent,
    GriffinsComponent,
    ScenariosComponent,
    AuditComponent,
    TreeSearchComponent,
    EventDisplayComponent,
    LogbookDetailsDescriptionComponent,
    AuditComponent,
    ObjectsGroupComponent,
    AccessionRegisterComponent,
    AccessionRegisterSearchComponent,
    AccessionRegisterSymbolicComponent,
    OperationComponent,
    HoldingschemeComponent,
    ArchiveExportDIPComponent,
    DialogComponent,
    LifecycleComponent,
    TraceabilityOperationDetailsComponent,
    WorkflowComponent,
    LoadingBlockComponent,
    MySelectionComponent,
    ArchiveUnitSelectionComponent,
    EliminationSearchComponent,
    ArchiveUnitFacetComponent,
    ObjectsGroupFacetComponent,
    MassiveUpdateFormComponent,
    FacetsComponent,
    ArchiveRuleBlocComponent,
    RulesDisplayModeComponent,
    RulesUpdateModeComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes, {useHash: true}),
    BrowserModule,
    BrowserAnimationsModule,
    MenubarModule,
    AccordionModule,
    ButtonModule,
    BreadcrumbModule,
    CalendarModule,
    DropdownModule,
    GrowlModule,
    PanelModule,
    RadioButtonModule,
    FormsModule,
    ListboxModule,
    PaginatorModule,
    FileDropModule,
    InputTextModule,
    TabViewModule,
    ToggleButtonModule,
    ProgressBarModule,
    DataTableModule,
    SharedModule,
    ReactiveFormsModule,
    CalendarModule,
    FieldsetModule,
    DialogModule,
    ConfirmDialogModule,
    InputSwitchModule,
    ChipsModule,
    TriStateCheckboxModule,
    OverlayPanelModule,
    MultiSelectModule,
    CheckboxModule,
    DataGridModule,
    CheckboxModule,
    BrowserModule,
    HttpClientModule,
    ScrollPanelModule,
    SliderModule,
    NgArrayPipesModule,
    TranslateModule.forRoot({
      loader: {provide: TranslateLoader, useClass: CustomLoader}
    }),
    SpinnerModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: VitamInterceptor,
      multi: true,
    },
    AuthenticationService,
    ResourcesService,
    CookieService,
    BreadcrumbService,
    LogbookService,
    LogbookHelperService,
    IngestService,
    IngestUtilsService,
    UploadService,
    ArchiveUnitHelper,
    ObjectsGroupHelper,
    ReferentialHelper,
    ArchiveUnitService,
    ReferentialsService,
    DateService,
    AccessContractService,
    ConfirmationService,
    ObjectsService,
    ObjectsGroupService,
    AuditService,
    DialogService,
    TraceabilityOperationService,
    WorkflowService,
    ErrorService,
    MySelectionService,
    MessagesUtilsService,
    ComputeRulesUtilsService
  ],
  bootstrap: [AppComponent]
})

export class AppModule {
}
