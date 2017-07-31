import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Title } from '@angular/platform-browser';
import 'rxjs/add/operator/switchMap';

import { BreadcrumbElement, BreadcrumbService } from '../../../common/breadcrumb.service';
import { FunctionalTestsService } from '../functional-tests.service';
import { PageComponent } from '../../../common/page/page-component';


const DefaultBreadcrumb: BreadcrumbElement[] = [];

@Component({
  selector: 'vitam-functional-tests-detail',
  templateUrl: './functional-tests-detail.component.html',
  styleUrls: ['./functional-tests-detail.component.css']
})

export class FunctionalTestsDetailComponent extends PageComponent {
  fileName: string;
  resultDetail : any;
  itemLists: any[];
  cols = [
    {field: 'Feature', label: 'Fonctionnalité'},
    {field: 'OperationId', label: 'Identifiant de l\'opération'},
    {field: 'Description', label: 'Description'},
    {field: 'Errors', label: 'Erreurs'}
  ];
  breadcrumb: BreadcrumbElement[] = [];

  constructor(private route: ActivatedRoute, public titleService: Title, public breadcrumbService: BreadcrumbService,
              private service : FunctionalTestsService) {
    super('Détail des tests fonctionels', DefaultBreadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.paramMap
      .switchMap((params: ParamMap) => {
        this.fileName =  params.get('fileName');
        this.breadcrumb = [
          {label: 'Tests', routerLink: ''},
          {label: 'Fonctionnels', routerLink: 'tests/functional-tests'},
          {label: 'Détails de ' + this.fileName, routerLink: 'tests/functional-tests/' + this.fileName}
        ];
        this.setBreadcrumb(this.breadcrumb);
        return this.service.getResultDetail(this.fileName);
      })
      .subscribe(data => {
        this.resultDetail = data;
      });
  }

  public getClass(data : any) : string {
    if (data.Ok == true) {
      return 'greenRows';
    } else {
      return 'redRows';
    }
  }

}
