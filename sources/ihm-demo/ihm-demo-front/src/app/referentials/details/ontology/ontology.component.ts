import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {PageComponent} from '../../../common/page/page-component';
import {Ontology} from './ontology';
import {ErrorService} from '../../../common/error.service';

@Component({
  selector: 'vitam-ontology',
  templateUrl: './ontology.component.html',
  styleUrls: ['./ontology.component.css']
})
export class OntologyComponent extends PageComponent {

  ontology: Ontology;
  file: File;
  id: string;
  update: boolean;
  updatedFields: any = {};

  constructor(private activatedRoute: ActivatedRoute, private router: Router, private errorService: ErrorService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService) {
    super('Détail de l\'ontologie ', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.getDetail();
      const newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Ontologies', routerLink: 'admin/search/ontology'},
        {label: 'Détail de l\'ontologie ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  getDetail() {
    this.searchReferentialsService.getOntologyById(this.id).subscribe(
      (value) => {
        this.initData(value);
      }, (error) => {
        this.errorService.handle404Error(error);
      }
    );
  }

  initData(value) {
    this.ontology = plainToClass(Ontology, value.$results)[0];
  }
}
