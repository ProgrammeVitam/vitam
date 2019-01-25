import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';

import {BreadcrumbElement, BreadcrumbService} from '../../../common/breadcrumb.service';
import {PageComponent} from '../../../common/page/page-component';
import {ActivatedRoute} from '@angular/router';
import {DateService} from '../../../common/utils/date.service';
import {ReferentialsService} from '../../referentials.service';
import {AccessionRegisterSymbolic} from './accession-register-symbolic';
import {plainToClass} from 'class-transformer';
import {Chart, helpers} from 'chart.js';

@Component({
  selector: 'vitam-accession-register-symbolic',
  templateUrl: './accession-register-symbolic.component.html'
})
export class AccessionRegisterSymbolicComponent extends PageComponent {
  id: string;
  breadcrumbs: BreadcrumbElement[] = [
    {label: 'Recherche', routerLink: ''},
    {label: 'Recherche par service producteur', routerLink: 'admin/accessionRegister'},
  ];
  startDate: Date = new Date();
  endDate: Date = new Date();
  locale = DateService.vitamFrLocale;
  colorFunct: Function = helpers.color;
  chartColors = {
    red: 'rgb(255, 99, 132)',
    orange: 'rgb(255, 159, 64)',
    yellow: 'rgb(255, 205, 86)',
    green: 'rgb(75, 192, 192)',
    blue: 'rgb(54, 162, 235)'
  };
  ctxOg: CanvasRenderingContext2D;
  ctxAu: CanvasRenderingContext2D;
  ctxBo: CanvasRenderingContext2D;
  ctxBos: CanvasRenderingContext2D;
  ObjectGroup = {
    label: 'Nombre de groupe d\'objets',
    backgroundColor: this.colorFunct(this.chartColors.red).alpha(0.5).rgbString(),
    borderColor: this.chartColors.red,
    fill: false,
    data: [],
  };
  ArchiveUnit = {
    label: 'Nombre d\'unité archivistique',
    backgroundColor: this.colorFunct(this.chartColors.blue).alpha(0.5).rgbString(),
    borderColor: this.chartColors.blue,
    fill: false,
    data: [],
  };
  BinaryObject = {
    label: 'Nombre d\'objets binaire',
    backgroundColor: this.colorFunct(this.chartColors.green).alpha(0.5).rgbString(),
    borderColor: this.chartColors.green,
    fill: false,
    data: [],
  };
  BinaryObjectSize = {
    label: 'Taille totale des objets',
    backgroundColor: this.colorFunct(this.chartColors.orange).alpha(0.5).rgbString(),
    borderColor: this.chartColors.orange,
    fill: false,
    data: [],
  };

  constructor(private activatedRoute: ActivatedRoute, public titleService: Title, public breadcrumbService: BreadcrumbService, private searchReferentialsService: ReferentialsService) {
    super('Registre de fond symbolique', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.startDate.setDate(this.endDate.getDate() - 30);
    this.activatedRoute.params.subscribe(params => {
      this.id = params.id;
      this.breadcrumbs.push({
        label: `Détail du service producteur ${this.id}`,
        routerLink: `admin/agencies/accessionRegister/${this.id}`
      });
      this.breadcrumbs.push({
        label: `Historique des rattachements ${this.id}`,
        routerLink: `admin/accessionRegister/symbolic/${this.id}`
      });
      this.setBreadcrumb(this.breadcrumbs);
    });

    this.ctxOg = (<HTMLCanvasElement>document.getElementById('chart-accession-register-symbolic-OG')).getContext('2d');
    this.ctxAu = (<HTMLCanvasElement>document.getElementById('chart-accession-register-symbolic-AU')).getContext('2d');
    this.ctxBo = (<HTMLCanvasElement>document.getElementById('chart-accession-register-symbolic-BO')).getContext('2d');
    this.ctxBos = (<HTMLCanvasElement>document.getElementById('chart-accession-register-symbolic-BOS')).getContext('2d');

    this.getAccessionRegisterSymbolic();
  }

  getAccessionRegisterSymbolic() {
    this.searchReferentialsService.getAccessionRegisterSymbolicByDate(this.id, this.startDate, this.endDate).subscribe(accessionRegisterSymbolics => {
      this.ObjectGroup.data = [];
      this.ArchiveUnit.data = [];
      this.BinaryObject.data = [];
      this.BinaryObjectSize.data = [];

      const attachedRegisters: AccessionRegisterSymbolic[] = plainToClass(AccessionRegisterSymbolic, accessionRegisterSymbolics.$results);

      let maxObjectGroup = 0;
      let maxArchiveUnit = 0;
      let maxBinaryObject = 0;
      let maxBinaryObjectSize = 0;

      attachedRegisters.sort((symbolic, otherSymbolic) =>
        new Date(symbolic.CreationDate).getTime() - new Date(otherSymbolic.CreationDate).getTime()
      );

      attachedRegisters.forEach(symbolic => {
        maxObjectGroup = symbolic.ObjectGroup > maxObjectGroup ? symbolic.ObjectGroup : maxObjectGroup;
        maxArchiveUnit = symbolic.ArchiveUnit > maxArchiveUnit ? symbolic.ArchiveUnit : maxArchiveUnit;
        maxBinaryObject = symbolic.BinaryObject > maxBinaryObject ? symbolic.BinaryObject : maxBinaryObject;
        maxBinaryObjectSize = symbolic.BinaryObjectSize > maxBinaryObjectSize ? symbolic.BinaryObjectSize : maxBinaryObjectSize;

        const date = new Date(symbolic.CreationDate);

        this.ObjectGroup.data.push({x: date, y: symbolic.ObjectGroup});
        this.ArchiveUnit.data.push({x: date, y: symbolic.ArchiveUnit});
        this.BinaryObject.data.push({x: date, y: symbolic.BinaryObject});
        this.BinaryObjectSize.data.push({x: date, y: symbolic.BinaryObjectSize});
      });

      new Chart(this.ctxOg, this.configChart(maxArchiveUnit + 2, this.ArchiveUnit));
      new Chart(this.ctxAu, this.configChart(maxObjectGroup + 2, this.ObjectGroup));
      new Chart(this.ctxBo, this.configChart(maxBinaryObject + 2, this.BinaryObject));
      new Chart(this.ctxBos, this.configChart(maxBinaryObjectSize + 5, this.BinaryObjectSize));
    });
  }

  configChart(max: number, dataset: any): any {
    return {
      type: 'line',
      data: {datasets: [dataset]},
      options: {
        responsive: true,
        type: 'bar',
        scales: {
          xAxes: [{
            type: 'time',
            time: {
              unit: 'day',
              max: this.endDate,
              min: this.startDate,
              displayFormats: {quarter: 'D MMM'}
            },
            display: true,
            distribution: 'linear',
            scaleLabel: {
              display: true,
              labelString: 'Date'
            },
            ticks: {
              major: {
                fontStyle: 'bold',
                fontColor: '#FF0000'
              }
            }
          }],
          yAxes: [{
            ticks: {
              suggestedMin: 0,
              suggestedMax: max,
            },
            display: true,
            scaleLabel: {
              display: true,
              labelString: 'value'
            }
          }]
        }
      }
    }
  }
}
