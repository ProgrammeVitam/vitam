import {Component, Input, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {ColumnDefinition} from './column-definition';
import {Router} from '@angular/router';

@Component({
  selector: 'vitam-generic-table',
  templateUrl: './generic-table.component.html',
  styleUrls: ['./generic-table.component.css'],
  changeDetection:ChangeDetectionStrategy.OnPush
})
export class GenericTableComponent implements OnInit {
  @Input() items: any[] = [];
  @Input() filter: 'filter';
  @Input() path = '';
  @Input() identifier: string;
  @Input() selectionMode: string = null;
  @Input() selectedCols: ColumnDefinition[] = [];
  @Input() getClass: () => string = () => '';

  clickable(col: ColumnDefinition): string {
    return col.icons.length ? '' : 'clickableDiv';
  }

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  navigateTo(event) {
    const htmlPath = event.originalEvent.target;
    if (this.path !== '' && (htmlPath.tagName === 'SPAN' ||
        (htmlPath.tagName === 'TD' && htmlPath.getElementsByTagName('i').length === 0))) {
      this.router.navigate([this.path, event.data[this.identifier]]);
    }
  }

}
