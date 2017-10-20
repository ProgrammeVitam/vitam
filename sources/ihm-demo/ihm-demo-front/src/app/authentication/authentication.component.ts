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

  errorMessage = '';
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
    this.errorMessage = '';
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
      if (!this.loginForm.controls.tenant.valid) {
        this.errorMessage = 'Veuillez choisir un tenant';
      }
    }

  }

}