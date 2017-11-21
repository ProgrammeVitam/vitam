import {Component, OnInit, Input} from '@angular/core';
import {TreeNode} from '../tree-node';
import {ArchiveUnitService} from "../../../archive-unit.service";
import {ArchiveTreeViewComponent} from "../archive-tree-view.component";


@Component({
  selector: 'vitam-tree-parent',
  templateUrl: './tree-parent.component.html',
  styleUrls: ['./tree-parent.component.css']
})
export class TreeParentComponent implements OnInit {
  @Input() level = 1;
  @Input() node: TreeNode;

  constructor(public archiveUnitService: ArchiveUnitService) { }

  ngOnInit() {
  }

  switchParent(node: TreeNode) {
    ArchiveTreeViewComponent.getParents(this.archiveUnitService, node);
    node.expended = !node.expended;
  }

}
