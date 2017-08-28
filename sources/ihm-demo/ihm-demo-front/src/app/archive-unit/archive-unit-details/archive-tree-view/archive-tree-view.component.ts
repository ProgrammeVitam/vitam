import {Component, OnInit, OnChanges, Input} from '@angular/core';
import {ArchiveUnitService} from "../../archive-unit.service";
import {TreeNode, NodeData} from './tree-node';

const LIMIT = 5;

@Component({
  selector: 'vitam-archive-tree-view',
  templateUrl: './archive-tree-view.component.html',
  styleUrls: ['./archive-tree-view.component.css']
})
export class ArchiveTreeViewComponent implements OnInit, OnChanges {
  @Input() unitId: string;
  @Input() unitUps: string[];
  @Input() type: string;
  @Input() title: string;
  root: TreeNode;

  constructor(private archiveUnitService: ArchiveUnitService) { }

  ngOnChanges(change) {
    if(change.unitId) {
      this.initRoot();
    }
  }

  ngOnInit() {
    this.initRoot();
  }

  initRoot() {
    let root = new TreeNode(this.title, this.unitId, new NodeData(this.type, this.unitUps || []));
    this.root = root;
    ArchiveTreeViewComponent.getParents(this.archiveUnitService, root);
    ArchiveTreeViewComponent.getChildren(this.archiveUnitService, root);
  }

  static getParents(archiveUnitService, node) {
    if (node.parents.length > 0) {
      return;
    }
    if (node.data.unitups.length === 0) {
      node.leaf = true;
      return;
    }

    // TODO Criteria: ADD_ROOT: ['id1', 'id2', ...] => Add multiple roots (Need add ADD_ROOT in queryDslHelper)
    // That would avoid loop over unitups Ids
    // That would avoid manual limit
    let iteration = 0;
   /* for(let unitId of node.data.unitups) {
      if (iteration < LIMIT) {
        iteration++;
      } else {
        node.data.haveMore = true;
        break;
      }*/

      let criteria = {
        'ADD_ROOT': node.data.unitups,
        isAdvancedSearchFlag: "Yes",
        projection_title: 'Title',
        projection_id: '#id',
        projection_unitups: '#unitups',
        projection_unitType: '#unittype'
      };
      archiveUnitService.getResults(criteria).subscribe(
          (response) => {
            let data: NodeData = new NodeData(response.$results[0]['_unitType'], response.$results[0]['#unitups']);
            let parent = new TreeNode(response.$results[0].Title, response.$results[0]['#id'], data);
            node.parents.push(parent);
          }
      );
  /*  }*/
  }

  static getChildren(archiveUnitService, node) {


    if (node.children.length > 0) {
      return;
    }

    // TODO Check if node have more child ? HTD ?

    // TODO Criteria: Find a direct child of a node ? depth works ?
    // TODO How to add a limit ?
    let criteria = {
      'id': node.id,
      'UNITUPS': node.id,
      isAdvancedSearchFlag: "Yes",
      projection_title: 'Title',
      projection_id: '#id',
      projection_unitups: '#unitups',
      projection_unitType: '#unittype'
    };
    archiveUnitService.getResults(criteria).subscribe(
        (response) => {

          let children = [];
          for (let result of response.$results) {
            let data: NodeData = new NodeData(result['_unitType'], result['#unitups']);
            let child = new TreeNode(result.Title, result['#id'], data);
            children.push(child);
          }

          node.children = children;
        }
    );
  }

}
