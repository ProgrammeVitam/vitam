import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { AuthenticationService, UserInformation } from './authentication.service';
import {TranslateService} from '@ngx-translate/core';

@Component({
  selector: 'vitam-authentication',
  templateUrl: './authentication.component.html',
  styleUrls: ['./authentication.component.css']
})
export class AuthenticationComponent implements OnInit {

  errorMessage = '';
  tenantId: string;
  tenants = [];
  isTLSEnabled = false;
  onlyTLSEnabled = false;
  loginForm: FormGroup;
  constructor(private authenticationService: AuthenticationService, private router: Router, private translateService:TranslateService) {
    this.loginForm = new FormGroup( {
      username : new FormControl('', Validators.required),
      password : new FormControl('', Validators.required)
    });

  }

  ngOnInit() {
    if (this.authenticationService.isLoggedIn()) {
      this.router.navigate(['ingest/sip']);
    } else {
      this.authenticationService.loggedOut();
    }
    this.authenticationService.getTenants()
      .subscribe((tenants: Array<string>) => {
        this.tenants = tenants;
        this.tenantId = tenants[0] + '';
      });

    this.authenticationService.storeTenantAdmin();
    this.authenticationService.getAuthenticationMode().subscribe(
      (authenticationModes: string[]) => {
        for (const authenticationMode of authenticationModes) {
          if (authenticationMode.indexOf('x509') > -1) {
            this.isTLSEnabled = true;
            if (authenticationModes.length === 1) {
              this.onlyTLSEnabled = true;
            }
            return;
          }
        }
      },
      (error) => {
        console.log(error);
      }
    );
  }

  login() {
    this.errorMessage = '';
    if (this.loginForm.valid && this.tenantId) {
      const username = this.loginForm.controls.username.value;
      const password = this.loginForm.controls.password.value;
      this.authenticationService.logIn(username, password).subscribe(
        (user: UserInformation) => {
          this.router.navigate(['ingest/sip']);
          this.authenticationService.loggedIn(user, this.tenantId);
          this.translateService.reloadLang(this.translateService.getDefaultLang());
        },
        (error) => {
          this.errorMessage = 'Identifiant et/ou mot de passe incorrect';
          this.authenticationService.loggedOut();
        }
      )
    } else {
      if (!this.loginForm.controls.username.valid) {
        if (!this.loginForm.controls.password.valid) {
          this.errorMessage = 'Veuillez entrer votre identifiant et votre mot de passe';
          return;
        }
        this.errorMessage = 'Veuillez entrer votre identifiant';
        return;
      }
      if (!this.loginForm.controls.password.valid) {
        this.errorMessage = 'Veuillez entrer votre mot de passe';
        return;
      }
      if (!this.tenantId) {
        this.errorMessage = 'Veuillez choisir un tenant';
      }
    }

  }

  loginWithCertificat() {
    this.errorMessage = '';
    if (!this.tenantId) {
      this.errorMessage = 'Veuillez choisir un tenant';
    } else {
      this.authenticationService.logInWithCertificat().subscribe(
        (user: UserInformation) => {
          this.router.navigate(['ingest/sip']);
          this.authenticationService.loggedIn(user, this.tenantId);
          this.translateService.reloadLang(this.translateService.getDefaultLang());
        },
        (error) => {
          this.errorMessage = 'Votre certificat n\'est pas connu';
          this.authenticationService.loggedOut();
        }
      );
    }

  }
}
