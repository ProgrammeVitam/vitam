import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { AuthenticationService, UserInformation } from './authentication.service';

@Component({
  selector: 'vitam-authentication',
  templateUrl: './authentication.component.html',
  styleUrls: ['./authentication.component.css']
})
export class AuthenticationComponent implements OnInit {

  errorMessages = '';
  tenantId: string;
  username: string;
  password: string;
  tenants = [];
  loginForm: FormGroup;
  constructor(private authenticationService: AuthenticationService, private router: Router) {
    this.loginForm = new FormGroup( {
      username : new FormControl('', Validators.required),
      password : new FormControl('', Validators.required),
      tenant : new FormControl('', Validators.required)
    });
  }

  ngOnInit() {
    if (this.authenticationService.isLoggedIn()) {
      this.router.navigate(["ingest/sip"]);
    }
    this.authenticationService.getTenants()
      .subscribe((tenants: Array<string>) => {
        this.tenants = tenants;
        this.loginForm.patchValue({
          tenant :tenants[0]
        });
      });
  }

  login() {
    this.errorMessages = '';
    if (this.loginForm.valid) {
      let username = this.loginForm.controls.username.value;
      let password = this.loginForm.controls.password.value;
      let tenant = this.loginForm.controls.tenant.value;
      this.authenticationService.logIn(username, password).subscribe(
        (user : UserInformation) => {
          this.router.navigate(["ingest/sip"]);
          this.authenticationService.loggedIn(user, tenant);
        },
        (error) => {
          this.errorMessages = 'Merci de vérifier votre identifiant et votre mot de passe';
          this.authenticationService.loggedOut();
        }
      )
    } else {
      if (!this.loginForm.controls.username.valid) {
        this.errorMessages = 'Merci de remplir votre identifiant';
      }
      if (!this.loginForm.controls.password.valid) {
        this.errorMessages = 'Merci de remplir votre mot de passe ';
      }
      if (!this.loginForm.controls.tenant.valid) {
        this.errorMessages = 'Merci de choisir un tenant';
      }
    }

  }

}