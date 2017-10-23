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

@Component({
  selector: 'vitam-accession-register',
  templateUrl: './accession-register.component.html',
  styleUrls: ['./accession-register.component.css']
})
export class AccessionRegisterComponent  extends PageComponent {

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
              public logbookService: LogbookService) {
    super('Détail du Fonds', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Service agent', routerLink: 'admin/search/agencies'},
        {label: this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
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
      });
      this.logbookService.getResults({
        'events.agIdExt.originatingAgency' : this.id
      }, 0).subscribe(
          data => {
          for (let logbook of data.$results) {
            this.registerDetailType[logbook.evIdProc] = logbook.evType;
          }
        },
          error => console.log('Error - ', error));
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
    return DateService.handleDate(detail.EndDate);
  }

  getDetailsType(detail : AccessionRegisterDetail) {
    for (let opId of detail.OperationIds) {
      if (this.registerDetailType[opId] === 'PROCESS_SIP_UNITARY') {
        return 'Archive';
      } else {
        return 'Plan';
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
      return 'Non stocké';
    }
  }
}
