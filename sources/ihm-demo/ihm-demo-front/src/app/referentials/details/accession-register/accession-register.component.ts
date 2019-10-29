import {Component, HostListener, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbElement, BreadcrumbService} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {DateService} from '../../../common/utils/date.service';

import {ArchiveUnitService} from '../../../archive-unit/archive-unit.service';
import {LogbookService} from '../../../ingest/logbook.service';
import {PageComponent} from '../../../common/page/page-component';
import {AccessionRegister, AccessionRegisterDetail, RegisterData} from './accession-register';
import {ErrorService} from '../../../common/error.service';
import {Hits} from '../../../common/utils/response';
import {AccessionRegisterSymbolic} from '../accession-register-symbolic/accession-register-symbolic';


const PROCESS_TRADUCTION = {
  'PROCESS_SIP_UNITARY': 'Standard',
  'FILINGSCHEME': 'Plan de classement',
  'HOLDINGSCHEME': 'Arbre de positionnement'
};

@Component({
  selector: 'vitam-accession-register',
  templateUrl: './accession-register.component.html',
  styleUrls: ['./accession-register.component.css']
})
export class AccessionRegisterComponent extends PageComponent {

  nbRows: number = 50;
  firstItem: number = 0;
  hits: Hits;
  newBreadcrumb: BreadcrumbElement[];
  register: AccessionRegister;
  registerDetails: AccessionRegisterDetail[];
  mainRegisters: RegisterData[];
  attachedRegisters: AccessionRegisterSymbolic[];
  registerDetailType = {};
  symbolicsRegistersCols = [
    {field: 'ArchiveUnit', header: 'Nombre d\'unités archivistiques'},
    {field: 'ObjectGroup', header: 'Nombre de groupes d\'objets techniques'},
    {field: 'BinaryObject', header: 'Nombre d\'objets'},
    {field: 'BinaryObjectSize', header: 'Volumétrie des objets'},
    {field: 'CreationDate', header: 'Date de création'}
  ];
  registersCols = [
    {field: 'TotalUnits', header: 'Nombre d\'unités archivistiques'},
    {field: 'TotalObjectGroups', header: 'Nombre de groupes d\'objets techniques'},
    {field: 'TotalObjects', header: 'Nombre d\'objets'},
    {field: 'ObjectSize', header: 'Volumétrie des objets'}
  ];
  @ViewChild('infoSupp') infoSuppElem;
  @ViewChild('infoList') infoListElem;
  displayOptions = false;
  id: string;
  extraColsSelection = [
    {value: 'ArchivalProfile', label: 'Profil d\'archivage'},
    {value: 'LegalStatus', label: 'Statut juridique'},
    {value: 'AcquisitionInformation', label: 'Information d\'aquisition'},
    {value: 'SubmissionAgency', label: 'Service versant'},
    {value: 'objectSize', label: 'Volumétrie'},
    {value: 'ArchivalAgreement', label: 'Contrat d\'entrée'}
  ];
  extraSelectedCols = [];

  constructor(private activatedRoute: ActivatedRoute, private router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService,
              public logbookService: LogbookService, private errorService: ErrorService) {
    super('Détail du fonds', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = decodeURIComponent(params['id']);
      this.getSymbolic();
      this.updateBreadcrumb(params['type']);
      this.paginate({first: 0, rows: this.nbRows})
    });
  }

  @HostListener('document:click', ['$event', '$event.target'])
  clickOutside($event, targetElement) {
    this.displayOptions = !!(
      (this.infoSuppElem && this.infoSuppElem.nativeElement.contains(targetElement))
      || (this.infoListElem && this.infoListElem.nativeElement.contains(targetElement))
    );
  }

  onRowSelect(event) {
    this.extraSelectedCols = event.value;
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

  getSymbolic() {
    this.searchReferentialsService.getAccessionRegisterSymbolic(this.id).subscribe(accessionRegisterSymbolics => {
      this.attachedRegisters = plainToClass(AccessionRegisterSymbolic, accessionRegisterSymbolics.$results);
      this.attachedRegisters.forEach(e => e.CreationDate = `${new Date(e.CreationDate).toLocaleDateString()} à ${new Date(e.CreationDate).toLocaleTimeString()}`)
    });

    this.searchReferentialsService.getFundRegisterById(this.id, 1).subscribe((value) => {
      this.register = plainToClass(AccessionRegister, value.$results)[0];
      if (this.register['#id']) {
        this.mainRegisters = [
          {
            TotalUnits: 'Total : ' + this.register.TotalUnits.ingested,
            TotalObjectGroups: 'Total : ' + this.register.TotalObjectGroups.ingested,
            TotalObjects: 'Total : ' + this.register.TotalObjects.ingested,
            ObjectSize: 'Total : ' + this.register.ObjectSize.ingested,
          },
          {
            TotalUnits: 'Supprimé : ' + this.register.TotalUnits.deleted,
            TotalObjectGroups: 'Supprimé : ' + this.register.TotalObjectGroups.deleted,
            TotalObjects: 'Supprimé : ' + this.register.TotalObjects.deleted,
            ObjectSize: 'Supprimé : ' + this.register.ObjectSize.deleted,
          },
          {
            TotalUnits: 'Restant : ' + this.register.TotalUnits.remained,
            TotalObjectGroups: 'Restant : ' + this.register.TotalObjectGroups.remained,
            TotalObjects: 'Restant : ' + this.register.TotalObjects.remained,
            ObjectSize: 'Restant : ' + this.register.ObjectSize.remained,
          }];
      }
    });
  }

  goToSearchUnitPage() {
    ArchiveUnitService.setInputRequest({originatingagencies: this.id});
    this.router.navigate(['search/archiveUnit']);
  }

  getDetailsMessage(field: string, detail: AccessionRegisterDetail) {
    return `Total : ${detail[field].ingested}\nSupprimé : ${detail[field].deleted}\nRestant : ${detail[field].remained}`;
  }

  getDate(detail: AccessionRegisterDetail) {
    return DateService.handleDateWithTime(detail.EndDate);
  }

  getDetailsType(detail: AccessionRegisterDetail) {
    const opId = detail.OperationIds[0];
    return PROCESS_TRADUCTION[this.registerDetailType[opId]];
  }

  getDetailsStatus(detail: AccessionRegisterDetail) {
    switch (detail.Status) {
      case 'STORED_AND_COMPLETED':
        return 'En stock et complète';
      case 'STORED_AND_UPDATED':
        return 'En stock et mise à jour';
      case 'UNSTORED':
        return 'Non stockée';
      default:
        return 'Inconnu';
    }
  }

  paginate(event) {
    this.firstItem = event.first;
    this.searchReferentialsService.getFundRegisterDetailById(this.id, event.rows, event.first).subscribe((value) => {
      this.registerDetails = plainToClass(AccessionRegisterDetail, value.$results);
      this.hits = value.$hits;
      this.registerDetails.forEach(detail =>
        this.logbookService.getDetails(detail.OperationIds[0], 1).subscribe(data =>
          this.registerDetailType[detail.OperationIds[0]] = data.$results[0].evType
        )
      )
    });
  }

  showOptionalCol(showingCol: string) {
    return this.extraSelectedCols.includes(showingCol);
  }

  onRowClick(mouseEvent: { originalEvent: MouseEvent, data: AccessionRegisterDetail }) {
    const eventsCell = document.getElementById(mouseEvent.data['#id']);
    if (eventsCell) {
      return eventsCell.parentElement.removeChild(eventsCell);
    }

    const accessionRegisterDetailElement: Element = mouseEvent.originalEvent.srcElement.closest('tr');
    const eventsHtml: HTMLTableRowElement = document.createElement('tr');
    eventsHtml.className = accessionRegisterDetailElement.className;
    eventsHtml.id = `${mouseEvent.data['#id']}`;

    const eventsUniqueCell: HTMLTableDataCellElement = document.createElement('td');
    eventsUniqueCell.colSpan = this.extraSelectedCols.length + 9;
    eventsHtml.appendChild(eventsUniqueCell);

    mouseEvent.data.Events.forEach(event => {
      const eventText = document.createElement('li');
      const text = document.createTextNode(this.getOperationType(event));
      eventText.appendChild(text);
      eventsUniqueCell.appendChild(eventText);
    });

    accessionRegisterDetailElement['after'](eventsHtml);
  }

  getOperationType(event) {
    switch (event.OpType) {
      case 'INGEST':
        return `l'opération d'entrée (id : ${event.Opc}) a créé ${event.Units} unités archivistiques, ${event.Gots} groupes d'objets techniques et ${event.Objects} objets pour une taille totale de ${event.ObjSize} octets.`;
      case 'ELIMINATION':
        return `l'opération d'élimination (id : ${event.Opc}) a supprimé ${event.Units} unités archivistiques, ${event.Gots} groupes d'objets techniques et ${event.Objects} objets pour une taille totale de ${event.ObjSize} octets.`;
      case 'TRANSFER_REPLY':
        return `l'opération d'acquittement de transfert (id : ${event.Opc}) a supprimé ${event.Units} unités archivistiques, ${event.Gots} groupes d'objets techniques et ${event.Objects} objets pour une taille totale de ${event.ObjSize} octets.`;
    }
  }
}
