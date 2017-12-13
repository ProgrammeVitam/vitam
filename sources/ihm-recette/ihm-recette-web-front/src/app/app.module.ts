import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { CookieService } from 'angular2-cookie/core';
import {
  FieldsetModule, PanelModule, ButtonModule, MenubarModule, InputTextModule,
  DialogModule, MessagesModule, DataTableModule, SharedModule, BreadcrumbModule, DropdownModule,
  GrowlModule, PasswordModule } from 'primeng/primeng';
import { RouterModule, Routes } from '@angular/router';
import { VisNetworkDirective } from 'ng2-vis';

import { AppComponent } from './app.component';
import { MenuComponent } from './common/menu/menu.component';
import { BreadcrumbComponent } from './common/breadcrumb/breadcrumb.component';
import { ResultsComponent } from './common/results/results.component';
import { FunctionalTestsComponent } from './tests/functional-tests/functional-tests.component';
import { FunctionalTestsDetailComponent } from './tests/functional-tests/detail/functional-tests-detail.component';
import { PerfComponent } from './tests/perf/perf.component';
import { QueryDSLComponent } from './tests/query-dsl/query-dsl.component';
import { CollectionComponent } from './admin/collection/collection.component';
import { LogbookComponent } from './traceability/logbook/logbook.component';
import { RemoveItemsComponent } from './admin/collection/remove-items/remove-items.component';
import { CollectionService } from './admin/collection/collection.service';
import { ResourcesService } from './common/resources.service';
import { GenericTableComponent } from './common/generic-table/generic-table.component';
import { BreadcrumbService } from './common/breadcrumb.service';
import { QueryDslService } from './tests/query-dsl/query-dsl.service';
import { FunctionalTestsService } from './tests/functional-tests/functional-tests.service';
import { LogbookService } from './traceability/logbook/logbook.service';
import { PerfService } from './tests/perf/perf.service';
import { AuthenticationComponent } from './authentication/authentication.component';
import { AuthenticationService } from './authentication/authentication.service';
import { TenantService } from "./common/tenant.service";
import { DagVisualizationComponent } from './tests/dag-visualization/dag-visualization.component';
import { VisNetworkService } from 'ng2-vis';
import { HttpClientModule } from "@angular/common/http";

const appRoutes: Routes = [
  {
    path: 'admin/collection', component: CollectionComponent
  },
  {
    path: 'login', component: AuthenticationComponent
  },
  {
    path: 'tests/functional-tests', component: FunctionalTestsComponent
  },
  {
    path: 'tests/functional-tests/:fileName', component: FunctionalTestsDetailComponent
  },
  {
    path: 'tests/queryDSL', component: QueryDSLComponent
  },
  {
    path: 'tests/perf', component: PerfComponent
  },
  {
    path: 'traceability/logbook', component: LogbookComponent
  },
  {
    path: 'tests/dag-visualization', component: DagVisualizationComponent
  },
  {
    path: '**', redirectTo: 'login', pathMatch: 'full'
  }
];

@NgModule({
  declarations: [
    AppComponent,
    MenuComponent,
    BreadcrumbComponent,
    ResultsComponent,
    FunctionalTestsComponent,
    FunctionalTestsDetailComponent,
    PerfComponent,
    QueryDSLComponent,
    CollectionComponent,
    LogbookComponent,
    RemoveItemsComponent,
    GenericTableComponent,
    AuthenticationComponent,
    DagVisualizationComponent,
    VisNetworkDirective
  ],
  imports: [
    RouterModule.forRoot(appRoutes, {useHash: true}),
    HttpClientModule,
    MenubarModule,
    ButtonModule,
    PanelModule,
    BrowserAnimationsModule,
    FieldsetModule,
    BrowserModule,
    DialogModule,
    FormsModule,
    MessagesModule,
    DataTableModule,
    SharedModule,
    BreadcrumbModule,
    DropdownModule,
    GrowlModule,
    PasswordModule,
    InputTextModule
  ],
  providers: [
    CollectionService,
    ResourcesService,
    CookieService,
    BreadcrumbService,
    QueryDslService,
    FunctionalTestsService,
    LogbookService,
    PerfService,
    AuthenticationService,
    TenantService,
    VisNetworkService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
