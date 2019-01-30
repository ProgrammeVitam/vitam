import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService, BreadcrumbElement} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {PageComponent} from '../../../common/page/page-component';
import {Griffin} from './griffin';
import {ErrorService} from '../../../common/error.service';
import {plainToClass} from 'class-transformer';
import {ReferentialHelper} from '../../referential.helper';

@Component({
  selector: 'vitam-griffins',
  templateUrl: './griffins.component.html',
  styleUrls: ['./griffins.component.css']
})
export class GriffinsComponent extends PageComponent {

  newBreadcrumb: BreadcrumbElement[];
  griffin: Griffin;
  id: string;
  panelHeader: string;
  keyToLabel :  (x: string) => string;

  constructor(private activatedRoute: ActivatedRoute, public router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService, private errorService: ErrorService, private referentialHelper: ReferentialHelper) {
    super('Détail du Griffon', [], titleService, breadcrumbService);

    const translations = this.referentialHelper.getGriffinTranslations();
    this.keyToLabel = (field: string) => {
      const value = translations[field];
      if (translations[field]) {
        return value;
      } else {
        return field.split('.').pop();
      }
    };
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(
      params => {
        this.id = params['id'];
        this.searchReferentialsService.getGriffinById(this.id).subscribe((value) => {
          this.griffin = plainToClass(Griffin, value.$results)[0];
          this.panelHeader = 'Détail du griffon';
        }, (error) => {
          this.errorService.handle404Error(error);
        });
        let newBreadcrumb = [
          {label: 'Administration', routerLink: ''},
          {label: 'Griffons', routerLink: 'admin/search/griffins'},
          {label: 'Détail du griffon ' + this.id, routerLink: ''}
        ];

        this.setBreadcrumb(newBreadcrumb);
      });
  }
}
