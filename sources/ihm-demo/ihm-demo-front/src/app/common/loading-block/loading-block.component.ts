import {Component, Input, OnInit} from '@angular/core';

/**
 * Component used to display a loading message when data is not ready.
 * There is a panelMode (Block with title and content) or a divMode (panelMode = false, only display message)
 */
@Component({
  selector: 'vitam-loading-block',
  templateUrl: './loading-block.component.html',
  styleUrls: ['./loading-block.component.css']
})
export class LoadingBlockComponent implements OnInit {
  @Input() displayContent: boolean;
  @Input() displayLoading: boolean;
  @Input() loadingMessage: string = 'En cours de chargement...';
  // If panelMode = false, following params are useless
  @Input() panelMode: boolean = true;
  @Input() blockTitle: string = 'Chargement';
  @Input() toggleable: boolean = true;
  @Input() collapsed: boolean = false;

  constructor() { }

  ngOnInit() { }

}
