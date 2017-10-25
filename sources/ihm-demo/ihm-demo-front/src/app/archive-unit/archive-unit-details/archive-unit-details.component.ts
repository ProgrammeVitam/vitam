import {Component} from '@angular/core';
import {ActivatedRoute, ParamMap, Router, NavigationEnd} from '@angular/router';
import {PageComponent} from "../../common/page/page-component";
import { Title } from '@angular/platform-browser';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import 'rxjs/add/operator/switchMap';
import {ArchiveUnitService} from "../archive-unit.service";

@Component({
  selector: 'vitam-archive-unit-details',
  templateUrl: './archive-unit-details.component.html',
  styleUrls: ['./archive-unit-details.component.css']
})
export class ArchiveUnitDetailsComponent extends PageComponent {
  data;
  objects;
  id: string;

  constructor(private route: ActivatedRoute, public titleService: Title, public breadcrumbService: BreadcrumbService,
              private archiveUnitService: ArchiveUnitService, private router: Router) {
    super('Détails de l\'unitée archivistique', [], titleService, breadcrumbService);
    router.events.subscribe((val) => {
      if (val instanceof NavigationEnd) {
        this.pageOnInit();
      }
    });
  }

  pageOnInit() {
    this.route.paramMap
      .switchMap((params: ParamMap) => {
        this.id =  params.get('id');
        let newBreadcrumb = [
          {label: 'Recherche', routerLink: ''},
          {label: 'Recherche d\'archives', routerLink: 'search/archiveUnit'},
          {label: 'Détails de l\'unité archivistique ' + this.id, routerLink: 'search/archiveUnit/' + this.id}
        ];
        this.setBreadcrumb(newBreadcrumb);
        return [];
      })
      .subscribe(() => {/* Need a Subscribe to trigger switchMap */});

    this.archiveUnitService.getDetails(this.id).subscribe(
      (data) => {
        if (data) {
          this.data = data.$results[0];
          if (this.data['#object']) {
            this.archiveUnitService.getObjects(this.id).subscribe(
              (data) => {
                // FIXME Backend does not return VitamResponse. For now hack in order to return the good object
                this.objects = data
              }
            );
          }
        }
      }
    );
  }


}
