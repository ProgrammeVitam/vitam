import { Component, EventEmitter } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from '../../common/utils/response';
import { PageComponent } from '../../common/page/page-component';
import { Preresult } from '../../common/preresult';
import { FieldDefinition } from '../../common/search/field-definition';
import { ColumnDefinition } from '../../common/generic-table/column-definition';
import { ReferentialsService } from '../../referentials/referentials.service';

@Component({
  selector: 'vitam-accession-register',
  templateUrl: './accession-register.component.html',
  styleUrls: ['./accession-register.component.css']
})
export class AccessionRegisterSearchComponent extends PageComponent {

  referentialType: string;
  breadcrumbName: string;
  public response: VitamResponse;
  public searchForm: any = {};

  referentialData: FieldDefinition[]  = [];
  fundRegisters: string[]= [];
  public columns: ColumnDefinition[] = [];
  public extraColumns: ColumnDefinition[] = [];


  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Recherche par service producteur', [], titleService, breadcrumbService);


    this.breadcrumbName = "Recherche par service producteur";
    this.referentialData = [
      new FieldDefinition('AgencyName', "Intitulé", 4, 10),
      FieldDefinition.createIdField('AgencyID', "Identifiant", 4, 10),
      new FieldDefinition('Description', "Description", 4, 10)
    ];

    this.columns = [
      ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
        () => ({'width': '125px'})),
      ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
        () => ({'width': '125px'})),
      ColumnDefinition.makeStaticColumn('Description', 'Description', undefined,
        () => ({'width': '225px'}))
    ];

    this.setBreadcrumb([
      {label: 'Recherche', routerLink: ''},
      {label: 'Recherche par service producteur', routerLink: ''}
    ]);

    this.searchReferentialsService.setSearchAPI('admin/accession-register');
    this.searchReferentialsService.getResults({"ACCESSIONREGISTER":"ACCESSIONREGISTER","orderby":{"field":"OriginatingAgency","sortType":"ASC"}}).subscribe(
        data => {
        for ( let register of data.$results) {
          this.fundRegisters.push(register.OriginatingAgency);
        }
        this.searchReferentialsService.setSearchAPI('agencies');
        this.searchForm = {"AgencyID":"all","AgencyName":"all","orderby":{"field":"Name","sortType":"ASC"}};

        this.searchReferentialsService.getResults(this.searchForm).subscribe(
            data => {
            this.filterResponse(data);
          },
            error => console.log('Error - ', error)
        );
      },
        error => console.log('Error - ', error)
    );
  }

  pageOnInit() {
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    preResult.request = request;
    preResult.searchProcessSkip = false;
    preResult.success = true;
    return preResult;
  }

  onNotify(event) {
    this.searchReferentialsService.getResults({"ACCESSIONREGISTER":"ACCESSIONREGISTER","orderby":{"field":"OriginatingAgency","sortType":"ASC"}}).subscribe(
        data => {
          for (let register of data.$results) {
            this.fundRegisters.push(register.OriginatingAgency);
          }
          this.searchReferentialsService.setSearchAPI('agencies');
          this.filterResponse(event.response);
        });
    this.searchForm = event.form;
  }

  public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
    service.getResults(form).subscribe(
      (response) => {
        responseEvent.emit({response: response, form: form});
      },
      (error) => responseEvent.emit({response: null, form: form})
    );
  }

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }

  private filterResponse(data : any) {
    let responseAgencies = data;
    let filteredResults = [];
    for (let agency of responseAgencies.$results) {
      if (this.fundRegisters.indexOf(agency.Identifier) != -1) {
        filteredResults.push(agency)
      }
    }
    data.$results = filteredResults;
    data.$hits.total = filteredResults.length;
    this.response = data;
  }
}
