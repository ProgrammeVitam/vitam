import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { FunctionalTestsService } from './functional-tests.service';
import {Router} from '@angular/router';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Tests', routerLink: ''},
  {label: 'Fonctionnels', routerLink: 'tests/functional-tests'}
];

@Component({
  selector: 'vitam-functional-tests',
  templateUrl: './functional-tests.component.html',
  styleUrls: ['./functional-tests.component.css']
})

export class FunctionalTestsComponent extends PageComponent {
  results: any[];
  pending : boolean;
  error : boolean;
  cols = [
    {field: 'value', label: 'Rapport'},
    {field: 'details', label: 'Détails'}
  ];

  constructor(private service: FunctionalTestsService, public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Tests fonctionnels', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.getResults();
  }

  public getResults() {
    this.service.getResults()
      .subscribe(data => {
      let tmpResults = [];
      data.forEach(function(value) {
        tmpResults.push({value: value, details: 'Accès au détail'});
      });
      this.results = tmpResults;
    });
  }

  public launchTests() {
    this.pending = true;
    this.service.launchTests().subscribe(response => {
      if (response.status != 202) {
        this.pending = false;
        this.error = true;
      }
    });
  }

  getClass() {
    return 'clickableDiv';
  }
}
