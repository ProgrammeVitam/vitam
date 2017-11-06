import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService} from "../../../common/breadcrumb.service";
import {ReferentialsService} from "../../referentials.service";
import {ObjectsService} from '../../../common/utils/objects.service';
import {PageComponent} from "../../../common/page/page-component";
import {DialogService} from "../../../common/dialog/dialog.service";
import {Context} from "./context";

const CONTEXT_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate: 'Date de création',
  LastUpdate: 'Date de mise à jour',
  ActivationDate: 'Date d\'activation',
  DeactivationDate: 'Date de désactivation',
  Name: 'Intitulé',
  Status: 'Statut',
  Description: 'Description',
  '#tenant': 'Tenant'
};

@Component({
  selector: 'vitam-context',
  templateUrl: './context.component.html',
  styleUrls: ['./context.component.css']
})

export class ContextComponent extends PageComponent {

  context: Context;
  modifiedContext: Context;
  arrayOfKeys: string[];
  tenants: number[];
  selectedTenant: string;
  id: string;
  update: boolean;
  updatedFields = {};
  saveRunning = false;

  constructor(private activatedRoute: ActivatedRoute, private router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private referentialsService: ReferentialsService, private dialogService: DialogService) {
    super('Détail du contexte applicatif ', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.referentialsService.getTenants()
      .subscribe((tenants: Array<number>) => {
        this.tenants = tenants;
      });

    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Référentiel des contextes applicatifs', routerLink: 'admin/search/context'},
        {label: 'Détail du contexte applicatif ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.context[key];
  }

  getKeyName(key: string) {
    return CONTEXT_KEY_TRANSLATION[key] || key;
  }

  translate(field: string) {
    if (field.indexOf('_tenant') >= 0) {
      return 'Tenant';
    }
    if (field.indexOf('AccessContracts') >= 0) {
      return 'Contrats d\'accès';
    }
    if (field.indexOf('IngestContracts') >= 0) {
      return 'Contrats d\'entrée';
    }
    return field;
  }

  isUpdatable(key: string) {
    if (['CreationDate', 'LastUpdate', 'Identifier', '#tenant', '_id'].indexOf(key) > -1) {
      return false;
    } else {
      return this.update;
    }
  }

  valueChange(key: string) {
    this.updatedFields[key] = this.modifiedContext[key];
  }

  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedContext = ObjectsService.clone(this.context);
    }
  }

  getDetail() {
    this.referentialsService.getContextById(this.id).subscribe((value) => {
      this.initData(value);
    });
  }

  saveUpdate() {
    if (Object.keys(this.updatedFields).length == 0) {
      this.switchUpdateMode();
      this.dialogService.displayMessage('Aucune modification effectuée', '');
      return;
    }

    this.saveRunning = true;
    this.updatedFields['LastUpdate'] = new Date();

    if (JSON.stringify(this.modifiedContext.Permissions) != JSON.stringify(this.context.Permissions.toString)) {
      this.updatedFields['Permissions'] = this.modifiedContext.Permissions;
    }
    this.referentialsService.updateDocumentById('contexts', this.id, this.updatedFields)
      .subscribe((data) => {
        this.referentialsService.getContextById(this.id).subscribe((value) => {
          this.initData(value);
          this.switchUpdateMode();
          this.saveRunning = false;
          if (data.httpCode >= 400) {
            this.dialogService.displayMessage('Erreur de modification. Aucune révision effectuée.', '');
          } else {
            this.dialogService.displayMessage('Les modifications ont bien été enregistrées.', '');
          }
        }, (error) => {
          this.saveRunning = false;
          this.dialogService.displayMessage('Erreur de modification. Aucune révision effectuée.', '');
        });
      }, (error) => {
        this.saveRunning = false;
        this.dialogService.displayMessage('Erreur de modification. Aucune révision effectuée.', '');
      });
  }

  removeTenant(tenantId) {
    if (!this.modifiedContext.Permissions) {
      this.modifiedContext.Permissions = [];
    }
    let modifiedContext = this.modifiedContext;
    this.modifiedContext.Permissions.forEach(function(value, index) {
      if (tenantId == value._tenant) {
        modifiedContext.Permissions.splice(index, 1);
      }
    });
    this.tenants.push(tenantId);
    this.tenants.sort();
  }

  addTenant() {
    if (!this.modifiedContext.Permissions) {
      this.modifiedContext.Permissions = [];
    }
    let newTenant = parseInt(this.selectedTenant);
    let newPermission = {
      _tenant: newTenant,
      AccessContracts: [],
      IngestContracts: []
    };
    this.modifiedContext.Permissions.push(newPermission);
    let position = this.tenants.indexOf(newTenant);
    this.tenants.splice(position, 1);
    this.selectedTenant = this.tenants[0].toString();
  }

  initData(value) {
    this.context = plainToClass(Context, value.$results)[0];
    this.modifiedContext = ObjectsService.clone(this.context);

    for (let permission of this.modifiedContext.Permissions) {
      this.tenants.splice(this.tenants.indexOf(permission._tenant), 1);
    }
  }

}
