import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';

import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { PerfService } from './perf.service';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Tests', routerLink: ''},
  {label: 'Performance', routerLink: 'tests/perf'}
];

@Component({
  selector: 'vitam-perf',
  templateUrl: './perf.component.html',
  styleUrls: ['./perf.component.css']
})
export class PerfComponent extends PageComponent {

  sipList : Array<SelectItem>;
  reportsList: any[];
  parallelIngest: number;
  numberOfIngest: number;
  selectedSIP : string;
  cols = [
    {field: 'value', label: 'Nom'}
  ];

  colsWithIcon = [
    {icon: 'fa fa-download', label: 'Télecharger'}
  ];

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService,
              public service : PerfService) {
    super('Tests de performance', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.service.generateIngestStatReport().subscribe(data => {
      let tmpResults = [];
      data.forEach(function(value) {
        tmpResults.push({value: value});
      });
      this.reportsList = tmpResults;
    });

    this.service.getAvailableSipForUpload().subscribe(data => {
      this.sipList = data.map(
        (fileName) => {
          return {label: fileName, value: fileName}
        }
      );
    });
    this.parallelIngest = 1;
    this.numberOfIngest = 1;
  }

  uploadSelectedSip() {
    let data =    {
      "fileName": this.selectedSIP,
      "parallelIngest": this.parallelIngest,
      "numberOfIngest":this.numberOfIngest,
    };
    this.service.uploadSelected(data).subscribe(response => {
      if (response.status != 202) {
        console.log('Test performance error');
      }
    });
  }

  downloadReport(report : any) {
    this.service.downloadURL(report.value);
  }

}
