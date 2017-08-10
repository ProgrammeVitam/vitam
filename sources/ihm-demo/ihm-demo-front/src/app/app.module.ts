import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {CookieService} from 'angular2-cookie/core';

import { ButtonModule, MenubarModule, BreadcrumbModule, DropdownModule, GrowlModule } from 'primeng/primeng';

import { AppComponent } from './app.component';
import {MenuComponent} from './common/menu/menu.component';
import {BreadcrumbComponent} from './common/breadcrumb/breadcrumb.component';
import {ResourcesService} from './common/resources.service';
import {BreadcrumbService} from './common/breadcrumb.service';
import { HomeComponent } from './home/home.component';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';

const appRoutes: Routes = [
  {
    path: 'home', component: HomeComponent
  },
  {
    path: '**', redirectTo: 'home', pathMatch: 'full'
  }
];

@NgModule({
  declarations: [
    AppComponent,
    MenuComponent,
    BreadcrumbComponent,
    HomeComponent
  ],
  imports: [
    RouterModule.forRoot(appRoutes, {useHash: true}),
    BrowserModule,
    MenubarModule,
    ButtonModule,
    BreadcrumbModule,
    DropdownModule,
    GrowlModule,
    FormsModule,
    HttpModule
  ],
  providers: [
    ResourcesService,
    CookieService,
    BreadcrumbService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
