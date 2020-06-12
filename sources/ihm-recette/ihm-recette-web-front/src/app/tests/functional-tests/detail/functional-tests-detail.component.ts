import 'rxjs/add/operator/switchMap';

import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { BreadcrumbElement, BreadcrumbService } from '../../../common/breadcrumb.service';
import { PageComponent } from '../../../common/page/page-component';
import { FunctionalTestsService } from '../functional-tests.service';
import { Report } from './report';
import { TagInfo } from './tag-info';


const DefaultBreadcrumb: BreadcrumbElement[] = [];

@Component({
  selector: 'vitam-functional-tests-detail',
  templateUrl: './functional-tests-detail.component.html',
  styleUrls: ['./functional-tests-detail.component.css']
})

export class FunctionalTestsDetailComponent extends PageComponent {
  fileName: string;
  resultDetail: Report;
  itemLists: any[];
  cols = [
    { field: 'Feature', label: 'Fonctionnalité' },
    { field: 'OperationId', label: 'Identifiant de l\'opération' },
    { field: 'Description', label: 'Description' },
    { field: 'Errors', label: 'Erreurs' }
  ];
  breadcrumb: BreadcrumbElement[] = [];

  constructor(private route: ActivatedRoute, public titleService: Title, public breadcrumbService: BreadcrumbService,
    private service: FunctionalTestsService) {
    super('Détail des tests fonctionels', DefaultBreadcrumb, titleService, breadcrumbService);
  }

  pageOnInit(): Subscription {
    return this.route.paramMap
      .switchMap((params: ParamMap) => {
        this.fileName = params.get('fileName');
        this.breadcrumb = [
          { label: 'Tests', routerLink: '' },
          { label: 'Fonctionnels', routerLink: 'tests/functional-tests' },
          { label: 'Détails de ' + this.fileName, routerLink: 'tests/functional-tests/' + this.fileName }
        ];
        this.setBreadcrumb(this.breadcrumb);
        return this.service.getResultDetail(this.fileName);
      })
      .subscribe(data => {
        this.resultDetail = data;
        if (this.resultDetail && this.resultDetail.Tags)
          this.resultDetail.Tags = this.orderTagsList(this.resultDetail.Tags);
      });
  }

  public getClass(data: any): string {
    if (data.Ok == true) {
      return 'greenRows';
    } else {
      return 'redRows';
    }
  }

  public orderTagsList(tags: TagInfo[]): TagInfo[] {
    return tags.sort((a: any, b: any) => {
      if (a.Ok + a.Ko > b.Ok + b.Ko) {
        return -1;
      } else if (a.Ok + a.Ko < b.Ok + b.Ko) {
        return 1;
      } else {
        return 0;
      }
    });
  }

}
