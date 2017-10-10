import {OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {BreadcrumbElement, BreadcrumbService} from '../breadcrumb.service';

/**
 * This component is an abstract class for all page component of the application.
 * When calling this component with good title breadcrumb and services, this class initialize all that.
 * Constructor call MUST have public services in herited component.
 * pageOnInit() MUST be implemented and COULD have a non empty core with component initialization.
 * setBreadcrumb() COULD be calles in pageOnInit() or any other code in order to update dynamicaly the breadcrumb informations
 *
 */
export abstract class PageComponent implements OnInit {
  abstract pageOnInit(): void;

  constructor(private title: string, public breadcrumb: BreadcrumbElement[],
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
  }

  ngOnInit() {
    this.titleService.setTitle(`Recette - ${this.title}`);
    this.breadcrumbService.changeState(this.breadcrumb);
    this.pageOnInit();
  }

  public setBreadcrumb(breadcrumb: BreadcrumbElement[]) {
    this.breadcrumb = breadcrumb;
    this.breadcrumbService.changeState(this.breadcrumb);
  }

}
