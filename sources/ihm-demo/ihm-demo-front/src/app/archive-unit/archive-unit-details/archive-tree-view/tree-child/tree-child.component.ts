import { Component, OnInit, Input } from '@angular/core';
import {TreeNode} from '../tree-node';
import {ArchiveUnitService} from "../../../archive-unit.service";
import {ArchiveTreeViewComponent} from "../archive-tree-view.component";
import {FormGroup, FormControl} from "@angular/forms";

@Component({
  selector: 'vitam-tree-child',
  templateUrl: './tree-child.component.html',
  styleUrls: ['./tree-child.component.css']
})
export class TreeChildComponent implements OnInit {
  @Input() level = 1;
  @Input() node: TreeNode;

  searchForm: FormGroup;

  constructor(public archiveUnitService: ArchiveUnitService) { }

  ngOnInit() {
    this.searchForm = new FormGroup({
      titleAndDescription: new FormControl(''),
      startDate: new FormControl(''),
      endDate: new FormControl('')
    })
  }

  switchChild(node: TreeNode) {
    ArchiveTreeViewComponent.getChildren(this.archiveUnitService, node);
    node.expended = !node.expended;
  }

}
