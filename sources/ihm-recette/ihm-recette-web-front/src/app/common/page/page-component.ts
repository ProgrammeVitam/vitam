import {OnDestroy, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {BreadcrumbElement, BreadcrumbService} from '../breadcrumb.service';
import {TenantService} from "../tenant.service";
import {Subscription} from "rxjs/Subscription";

/**
 * This component is an abstract class for all page component of the application.
 * When calling this component with good title breadcrumb and services, this class initialize all that.
 * Constructor call MUST have public services in herited component.
 * pageOnInit() MUST be implemented, COULD have a non empty core with component initialization and COULD return a Subscription
 *  The returned Subscription will be unsubscribed on destroy
 * setBreadcrumb() COULD be call in pageOnInit() or any other code in order to update dynamicaly the breadcrumb informations
 *
 */
export abstract class PageComponent implements OnInit, OnDestroy {
  subscriptionToDestroy: Subscription;

  abstract pageOnInit(): Subscription;

  constructor(private title: string, public breadcrumb: BreadcrumbElement[],
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
  }

  ngOnDestroy() {
    if (this.subscriptionToDestroy) {
      this.subscriptionToDestroy.unsubscribe();
    }
  }

  ngOnInit() {
    this.titleService.setTitle(`Recette - ${this.title}`);
    this.breadcrumbService.changeState(this.breadcrumb);
    this.subscriptionToDestroy = this.pageOnInit();
  }

  public setBreadcrumb(breadcrumb: BreadcrumbElement[]) {
    this.breadcrumb = breadcrumb;
    this.breadcrumbService.changeState(this.breadcrumb);
  }

}
