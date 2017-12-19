import {Component} from "@angular/core";
import {ActivatedRoute} from "@angular/router";
import {plainToClass} from "class-transformer";
import {Title} from "@angular/platform-browser";

import {BreadcrumbService} from "../../../common/breadcrumb.service";
import {ReferentialsService} from "../../referentials.service";
import {Format} from "./format";
import {PageComponent} from "../../../common/page/page-component";
import {ErrorService} from "../../../common/error.service";

const FORMAT_KEY_TRANSLATION = {
  Name: 'Intitulé',
  HasPriorityOverFileFormatID: 'Priorité sur les versions précédentes',
  MIMEType: 'MIME types',
  VersionPronom: 'Version de Pronom',
  CreatedDate: "Date de création"
};

@Component({
  selector: 'vitam-format',
  templateUrl: './format.component.html',
  styleUrls: ['./format.component.css']
})

export class FormatComponent extends PageComponent {

  id: string;
  format: Format;
  pronomLink: string;
  arrayOfKeys: string[];

  constructor(private activatedRoute: ActivatedRoute, public titleService: Title,
              public breadcrumbService: BreadcrumbService,private errorService: ErrorService,
              private searchReferentialsService: ReferentialsService) {
    super('Détail du format ', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.searchReferentialsService.getFormatById(this.id).subscribe(
        (value) => {
          this.format = plainToClass(Format, value.$results)[0];
          let keys = Object.keys(this.format);
          let format = this.format;
          this.pronomLink = 'http://www.nationalarchives.gov.uk/PRONOM/' + this.format.PUID;
          this.arrayOfKeys = keys.filter(function (key) {
            return key != '_id' && !!format[key] && format[key].length > 0;
          });
        }, (error) => {
          this.errorService.handle404Error(error);
        });
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Formats', routerLink: 'admin/search/format'},
        {label: 'Détail du format ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  getValue(key: string) {
    return this.format[key];
  }

  getKeyName(key: string) {
    return FORMAT_KEY_TRANSLATION[key] || key;
  }

  goToPronomLink() {
    window.open(this.pronomLink);
  }

  encode(puid: string) {
    return encodeURIComponent(puid);
  }
}
