import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';

import { ArchiveUnitService } from '../../../archive-unit/archive-unit.service';
import { LogbookService } from "../../../ingest/logbook.service";
import { PageComponent } from "../../../common/page/page-component";
import { AccessionRegister, AccessionRegisterDetail, RegisterData } from "./accession-register";
import { ErrorService } from "../../../common/error.service";
import { Hits } from "../../../common/utils/response";


const PROCESS_TRADUCTION = {
  'PROCESS_SIP_UNITARY' : 'Standard',
  'FILINGSCHEME' : 'Plan de classement',
  'HOLDINGSCHEME' : 'Arbre de positionnement'
};

@Component({
  selector: 'vitam-accession-register',
  templateUrl: './accession-register.component.html',
  styleUrls: ['./accession-register.component.css']
})
export class AccessionRegisterComponent  extends PageComponent {

  nbRows = 25;
  hits: Hits;
  firstItem = 0;
  displayedItems: any[] = [];

  newBreadcrumb: BreadcrumbElement[];
  register : AccessionRegister;
  registerDetails : AccessionRegisterDetail[];
  mainRegisters : RegisterData[];
  attachedRegisters : RegisterData[];
  registerDetailType = {};
  registersCols = [
    {field: 'TotalUnits', header: 'Nombre d\'unités archivistiques'},
    {field: 'TotalObjectGroups', header: 'Nombre de groupes d\'objets techniques'},
    {field: 'TotalObjects', header: 'Nombre d\'objets'},
    {field: 'ObjectSize', header: 'Volumétrie des objets'}
  ];
  id: string;
  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService,
              public logbookService: LogbookService, private errorService: ErrorService) {
    super('Détail du fonds', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      this.updateBreadcrumb(params['type']);
    });
  }

  updateBreadcrumb(type: string) {
    if (type === 'all') {
      this.newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Services agents', routerLink: 'admin/search/agencies'},
        {label: 'Détail du service agent ' + this.id, routerLink: 'admin/agencies/' + this.id},
        {label: 'Détail du fonds ' + this.id, routerLink: ''}
      ];
    } else {
      this.newBreadcrumb = [
        {label: 'Recherche', routerLink: ''},
        {label: 'Recherche par service producteur', routerLink: 'admin/accessionRegister'},
        {label: 'Détail du service producteur ' + this.id, routerLink: 'admin/agencies/accessionRegister/' + this.id},
        {label: 'Détail du fonds ' + this.id, routerLink: ''}
      ];
    }

    this.setBreadcrumb(this.newBreadcrumb);
  }
  getDetail() {
    this.searchReferentialsService.getFundRegisterById(this.id).subscribe((value) => {
      this.register = plainToClass(AccessionRegister, value.$results)[0];
      if (this.register['#id']) {
        this.mainRegisters = [
          {
            TotalUnits : 'Total : ' + this.register.TotalUnits.ingested,
            TotalObjectGroups : 'Total : ' + this.register.TotalObjectGroups.ingested,
            TotalObjects : 'Total : ' + this.register.TotalObjects.ingested,
            ObjectSize : 'Total : ' + this.register.ObjectSize.ingested,
          },
          {
            TotalUnits : 'Supprimé : ' + this.register.TotalUnits.deleted,
            TotalObjectGroups : 'Supprimé : ' + this.register.TotalObjectGroups.deleted,
            TotalObjects : 'Supprimé : ' + this.register.TotalObjects.deleted,
            ObjectSize : 'Supprimé : ' + this.register.ObjectSize.deleted,
          },
          {
            TotalUnits : 'Restant : ' + this.register.TotalUnits.remained,
            TotalObjectGroups : 'Restant : ' + this.register.TotalObjectGroups.remained,
            TotalObjects : 'Restant : ' + this.register.TotalObjects.remained,
            ObjectSize : 'Restant : ' + this.register.ObjectSize.remained,
          }];
        this.attachedRegisters = [
          {
            TotalUnits : 'Total : ' + this.register.TotalUnits.attached,
            TotalObjectGroups : 'Total : ' + this.register.TotalObjectGroups.attached,
            TotalObjects : 'Total : ' + this.register.TotalObjects.attached,
            ObjectSize : 'Total : ' + this.register.ObjectSize.attached,
          },
          {
            TotalUnits : 'Supprimé : ' + this.register.TotalUnits.detached,
            TotalObjectGroups : 'Supprimé : ' + this.register.TotalObjectGroups.detached,
            TotalObjects : 'Supprimé : ' + this.register.TotalObjects.detached,
            ObjectSize : 'Supprimé : ' + this.register.ObjectSize.detached,
          },
          {
            TotalUnits : 'Restant : ' + this.register.TotalUnits.symbolicRemained,
            TotalObjectGroups : 'Restant : ' + this.register.TotalObjectGroups.symbolicRemained,
            TotalObjects : 'Restant : ' + this.register.TotalObjects.symbolicRemained,
            ObjectSize : 'Restant : ' + this.register.ObjectSize.symbolicRemained,
          }];
      }

      this.searchReferentialsService.getFundRegisterDetailById(this.id).subscribe((value) => {
        this.registerDetails = plainToClass(AccessionRegisterDetail, value.$results);
        this.hits = value.$hits;
        this.displayedItems = this.registerDetails.slice(this.firstItem, this.firstItem + this.nbRows);
      });
      this.logbookService.getResults({
        'events.agIdExt.originatingAgency' : this.id
      }, 0).subscribe(
        data => {
          for (let logbook of data.$results) {
            this.registerDetailType[logbook.evIdProc] = logbook.evType;
          }
        }, (error) => {
          this.errorService.handle404Error(error);
        })
    });
  }


  goToSearchUnitPage() {
    ArchiveUnitService.setInputRequest({originatingagencies : this.id});
    this.router.navigate(['search/archiveUnit']);
  }

  getDetailsMessage(field : string, detail : AccessionRegisterDetail) {
    if (detail.Symbolic) {
      return 'Total : ' + detail[field].attached
        + "\n" + ' Supprimé : ' + detail[field].detached
        + '\n Restant : ' + detail[field].symbolicRemained;
    } else {
      return 'Total : ' + detail[field].ingested
        + "\n" + ' Supprimé : ' + detail[field].deleted
        + '\n Restant : ' + detail[field].remained;
    }
  }

  getDetailIcon(detail : AccessionRegisterDetail) {
    if (detail.Symbolic) {
      return 'fa fa-times-circle';
    } else {
      return 'fa fa-check-circle';
    }
  }

  getDate(detail : AccessionRegisterDetail) {
    return DateService.handleDateWithTime(detail.EndDate);
  }

  getDetailsType(detail : AccessionRegisterDetail) {
    for (let opId of detail.OperationIds) {
      if (PROCESS_TRADUCTION[this.registerDetailType[opId]]) {
        return PROCESS_TRADUCTION[this.registerDetailType[opId]];
      } else {
        // quand c'est une opération de rattachement on doit chercher evType par opId
        return this.logbookService.getDetails(opId).subscribe((data) => {
          this.registerDetailType[opId] = data.$results[0].evType;
          return PROCESS_TRADUCTION[this.registerDetailType[opId]];
        })
      }
    }
  }

  getDetailsStatus(detail : AccessionRegisterDetail) {
    if (detail.Status === 'STORED_AND_COMPLETED') {
      return 'En stock et complète';
    }
    if (detail.Status === 'STORED_AND_UPDATED') {
      return 'En stock et mise à jour';
    }

    if (detail.Status === 'UNSTORED') {
      return 'Non stockée';
    }
  }

  paginate(event) {
      this.firstItem = event.first;
      this.displayedItems = this.registerDetails.slice(this.firstItem, this.firstItem + event.rows);
  }
}
