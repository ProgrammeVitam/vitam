import { Component, EventEmitter } from '@angular/core';
import { PageComponent } from '../../common/page/page-component';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ArchiveUnitService } from '../archive-unit.service';
import { DynamicSelectItem, FieldDefinition } from '../../common/search/field-definition';
import { Preresult } from '../../common/preresult';
import { ColumnDefinition } from '../../common/generic-table/column-definition';
import { VitamResponse } from '../../common/utils/response';
import { ArchiveUnitHelper } from '../archive-unit.helper';
import { Router } from '@angular/router';
import { DateService } from '../../common/utils/date.service';
import { MySelectionService } from "../../my-selection/my-selection.service";
import { ResourcesService } from "../../common/resources.service";
import { FormGroup } from '@angular/forms';
import { SelectItem } from 'primeng/primeng';
import { DialogService } from '../../common/dialog/dialog.service';

const breadcrumb: BreadcrumbElement[] = [
    { label: 'Recherche', routerLink: '' },
    { label: 'Recherche d\'archives', routerLink: 'search/archiveUnit' }
];

@Component({
    selector: 'vitam-archive-unit',
    templateUrl: './archive-unit.component.html',
    styleUrls: ['./archive-unit.component.css']
})
export class ArchiveUnitComponent extends PageComponent {
    public response: VitamResponse;
    public searchRequest: any = {};
    public searchForm: any = {};
    advancedMode = false;
    disabledFacet = false;
    public archiveUnitFields = [
        new FieldDefinition('titleCriteria', 'Intitulé ou description', 12, 4)
    ];
    private fieldOptions = [
      {label: 'AcquiredDate', value: 'AcquiredDate'},
      {label: 'Addressee.BirthDate', value: 'Addressee.BirthDate'},
      {label: 'Addressee.BirthPlace.PostalCode', value: 'Addressee.BirthPlace.PostalCode'},
      {label: 'Addressee.DeathDate', value: 'Addressee.DeathDate'},
      {label: 'Addressee.DeathPlace.PostalCode', value: 'Addressee.DeathPlace.PostalCode'},
      {label: 'Addressee.Identifier', value: 'Addressee.Identifier'},
      {label: 'ArchivalAgencyArchiveUnitIdentifier', value: 'ArchivalAgencyArchiveUnitIdentifier'},
      {label: 'ArchiveUnitProfile', value: 'ArchiveUnitProfile'},
      {label: 'AuthorizedAgent.BirthDate', value: 'AuthorizedAgent.BirthDate'},
      {label: 'AuthorizedAgent.BirthPlace.PostalCode', value: 'AuthorizedAgent.BirthPlace.PostalCode'},
      {label: 'AuthorizedAgent.DeathDate', value: 'AuthorizedAgent.DeathDate'},
      {label: 'AuthorizedAgent.DeathPlace.PostalCode', value: 'AuthorizedAgent.DeathPlace.PostalCode'},
      {label: 'AuthorizedAgent.Identifier', value: 'AuthorizedAgent.Identifier'},
      {label: 'CreatedDate', value: 'CreatedDate'},
      {label: 'CustodialHistory.CustodialHistoryFile.DataObjectGroupReferenceId', value: 'CustodialHistory.CustodialHistoryFile.DataObjectGroupReferenceId'},
      {label: 'DescriptionLevel', value: 'DescriptionLevel'},
      {label: 'Descriptions', value: 'Descriptions'},
      {label: 'DescriptionLanguage', value: 'DescriptionLanguage'},
      {label: 'EndDate', value: 'EndDate'},
      {label: 'Event.EventDateTime', value: 'Event.EventDateTime'},
      {label: 'Event.EventIdentifier', value: 'Event.EventIdentifier'},
      {label: 'FilePlanPosition', value: 'FilePlanPosition'},
      {label: 'Gps.GpsAltitude', value: 'Gps.GpsAltitude'},
      {label: 'Gps.GpsAltitudeRef', value: 'Gps.GpsAltitudeRef'},
      {label: 'Gps.GpsDateStamp', value: 'Gps.GpsDateStamp'},
      {label: 'Gps.GpsLatitude', value: 'Gps.GpsLatitude'},
      {label: 'Gps.GpsLatitudeRef', value: 'Gps.GpsLatitudeRef'},
      {label: 'Gps.GpsLongitude', value: 'Gps.GpsLongitude'},
      {label: 'Gps.GpsLongitudeRef', value: 'Gps.GpsLongitudeRef'},
      {label: 'Gps.GpsVersionID', value: 'Gps.GpsVersionID'},
      {label: 'Keyword.KeywordContent', value: 'Keyword.KeywordContent'},
      {label: 'Keyword.KeywordReference', value: 'Keyword.KeywordReference'},
      {label: 'Keyword.KeywordType', value: 'Keyword.KeywordType'},
      {label: 'Language', value: 'Language'},
      {label: 'OriginatingAgency.Identifier', value: 'OriginatingAgency.Identifier'},
      {label: 'OriginatingAgencyArchiveUnitIdentifier', value: 'OriginatingAgencyArchiveUnitIdentifier'},
      {label: 'OriginatingSystemId', value: 'OriginatingSystemId'},
      {label: 'ReceivedDate', value: 'ReceivedDate'},
      {label: 'Recipient.BirthDate', value: 'Recipient.BirthDate'},
      {label: 'Recipient.BirthPlace.PostalCode', value: 'Recipient.BirthPlace.PostalCode'},
      {label: 'Recipient.DeathDate', value: 'Recipient.DeathDate'},
      {label: 'Recipient.DeathPlace.PostalCode', value: 'Recipient.DeathPlace.PostalCode'},
      {label: 'Recipient.Identifier', value: 'Recipient.Identifier'},
      {label: 'RegisteredDate', value: 'RegisteredDate'},
      {label: 'RelatedObjectReference.IsPartOf.ArchiveUnitRefId', value: 'RelatedObjectReference.IsPartOf.ArchiveUnitRefId'},
      {label: 'RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectGroupReferenceId', value: 'RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectGroupReferenceId'},
      {label: 'RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectReferenceId', value: 'RelatedObjectReference.IsPartOf.DataObjectReference.DataObjectReferenceId'},
      {label: 'RelatedObjectReference.IsPartOf.RepositoryArchiveUnitPID', value: 'RelatedObjectReference.IsPartOf.RepositoryArchiveUnitPID'},
      {label: 'RelatedObjectReference.IsPartOf.RepositoryObjectPID', value: 'RelatedObjectReference.IsPartOf.RepositoryObjectPID'},
      {label: 'RelatedObjectReference.IsVersionOf.ArchiveUnitRefId', value: 'RelatedObjectReference.IsVersionOf.ArchiveUnitRefId'},
      {label: 'RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectGroupReferenceId', value: 'RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectGroupReferenceId'},
      {label: 'RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectReferenceId', value: 'RelatedObjectReference.IsVersionOf.DataObjectReference.DataObjectReferenceId'},
      {label: 'RelatedObjectReference.IsVersionOf.RepositoryArchiveUnitPID', value: 'RelatedObjectReference.IsVersionOf.RepositoryArchiveUnitPID'},
      {label: 'RelatedObjectReference.IsVersionOf.RepositoryObjectPID', value: 'RelatedObjectReference.IsVersionOf.RepositoryObjectPID'},
      {label: 'RelatedObjectReference.References.ArchiveUnitRefId', value: 'RelatedObjectReference.References.ArchiveUnitRefId'},
      {label: 'RelatedObjectReference.References.DataObjectReference.DataObjectGroupReferenceId', value: 'RelatedObjectReference.References.DataObjectReference.DataObjectGroupReferenceId'},
      {label: 'RelatedObjectReference.References.DataObjectReference.DataObjectReferenceId', value: 'RelatedObjectReference.References.DataObjectReference.DataObjectReferenceId'},
      {label: 'RelatedObjectReference.References.RepositoryArchiveUnitPID', value: 'RelatedObjectReference.References.RepositoryArchiveUnitPID'},
      {label: 'RelatedObjectReference.References.RepositoryObjectPID', value: 'RelatedObjectReference.References.RepositoryObjectPID'},
      {label: 'RelatedObjectReference.Replaces.ArchiveUnitRefId', value: 'RelatedObjectReference.Replaces.ArchiveUnitRefId'},
      {label: 'RelatedObjectReference.Replaces.DataObjectReference.DataObjectGroupReferenceId', value: 'RelatedObjectReference.Replaces.DataObjectReference.DataObjectGroupReferenceId'},
      {label: 'RelatedObjectReference.Replaces.DataObjectReference.DataObjectReferenceId', value: 'RelatedObjectReference.Replaces.DataObjectReference.DataObjectReferenceId'},
      {label: 'RelatedObjectReference.Replaces.RepositoryArchiveUnitPID', value: 'RelatedObjectReference.Replaces.RepositoryArchiveUnitPID'},
      {label: 'RelatedObjectReference.Replaces.RepositoryObjectPID', value: 'RelatedObjectReference.Replaces.RepositoryObjectPID'},
      {label: 'RelatedObjectReference.Requires.ArchiveUnitRefId', value: 'RelatedObjectReference.Requires.ArchiveUnitRefId'},
      {label: 'RelatedObjectReference.Requires.DataObjectReference.DataObjectGroupReferenceId', value: 'RelatedObjectReference.Requires.DataObjectReference.DataObjectGroupReferenceId'},
      {label: 'RelatedObjectReference.Requires.DataObjectReference.DataObjectReferenceId', value: 'RelatedObjectReference.Requires.DataObjectReference.DataObjectReferenceId'},
      {label: 'RelatedObjectReference.Requires.RepositoryArchiveUnitPID', value: 'RelatedObjectReference.Requires.RepositoryArchiveUnitPID'},
      {label: 'RelatedObjectReference.Requires.RepositoryObjectPID', value: 'RelatedObjectReference.Requires.RepositoryObjectPID'},
      {label: 'SentDate', value: 'SentDate'},
      {label: 'Signature.DateSignature', value: 'Signature.DateSignature'},
      {label: 'Signature.Masterdata.Value', value: 'Signature.Masterdata.Value'},
      {label: 'Signature.ReferencedObject.SignedObjectDigest.Algorithm', value: 'Signature.ReferencedObject.SignedObjectDigest.Algorithm'},
      {label: 'Signature.ReferencedObject.SignedObjectDigest.Value', value: 'Signature.ReferencedObject.SignedObjectDigest.Value'},
      {label: 'Signature.ReferencedObject.SignedObjectId', value: 'Signature.ReferencedObject.SignedObjectId'},
      {label: 'Signature.Signer.Activity', value: 'Signature.Signer.Activity'},
      {label: 'Signature.Signer.BirthDate', value: 'Signature.Signer.BirthDate'},
      {label: 'Signature.Signer.BirthPlace.PostalCode', value: 'Signature.Signer.BirthPlace.PostalCode'},
      {label: 'Signature.Signer.DeathDate', value: 'Signature.Signer.DeathDate'},
      {label: 'Signature.Signer.DeathPlace.PostalCode', value: 'Signature.Signer.DeathPlace.PostalCode'},
      {label: 'Signature.Signer.Function', value: 'Signature.Signer.Function'},
      {label: 'Signature.Signer.Identifier', value: 'Signature.Signer.Identifier'},
      {label: 'Signature.Signer.SigningTime', value: 'Signature.Signer.SigningTime'},
      {label: 'Signature.Validator.Activity', value: 'Signature.Validator.Activity'},
      {label: 'Signature.Validator.BirthDate', value: 'Signature.Validator.BirthDate'},
      {label: 'Signature.Validator.BirthPlace.PostalCode', value: 'Signature.Validator.BirthPlace.PostalCode'},
      {label: 'Signature.Validator.DeathDate', value: 'Signature.Validator.DeathDate'},
      {label: 'Signature.Validator.DeathPlace.PostalCode', value: 'Signature.Validator.DeathPlace.PostalCode'},
      {label: 'Signature.Validator.Function', value: 'Signature.Validator.Function'},
      {label: 'Signature.Validator.Identifier', value: 'Signature.Validator.Identifier'},
      {label: 'Signature.Validator.ValidationTime', value: 'Signature.Validator.ValidationTime'},
      {label: 'StartDate', value: 'StartDate'},
      {label: 'Status', value: 'Status'},
      {label: 'SubmissionAgency.Identifier', value: 'SubmissionAgency.Identifier'},
      {label: 'SystemId', value: 'SystemId'},
      {label: 'Tag', value: 'Tag'},
      {label: 'Titles', value: 'Titles'},
      {label: 'TransactedDate', value: 'TransactedDate'},
      {label: 'TransferringAgencyArchiveUnitIdentifier', value: 'TransferringAgencyArchiveUnitIdentifier'},
      {label: 'Type', value: 'Type'},
      {label: 'Version', value: 'Version'},
      {label: 'Sender.Activity', value: 'Sender.Activity'},
      {label: 'Sender.BirthDate', value: 'Sender.BirthDate'},
      {label: 'Sender.BirthPlace.PostalCode', value: 'Sender.BirthPlace.PostalCode'},
      {label: 'Sender.DeathDate', value: 'Sender.DeathDate'},
      {label: 'Sender.DeathPlace.PostalCode', value: 'Sender.DeathPlace.PostalCode'},
      {label: 'Sender.Function', value: 'Sender.Function'},
      {label: 'Sender.Identifier', value: 'Sender.Identifier'},
      {label: 'Transmitter.Activity', value: 'Transmitter.Activity'},
      {label: 'Transmitter.BirthDate', value: 'Transmitter.BirthDate'},
      {label: 'Transmitter.BirthPlace.PostalCode', value: 'Transmitter.BirthPlace.PostalCode'},
      {label: 'Transmitter.DeathDate', value: 'Transmitter.DeathDate'},
      {label: 'Transmitter.DeathPlace.PostalCode', value: 'Transmitter.DeathPlace.PostalCode'},
      {label: 'Transmitter.Function', value: 'Transmitter.Function'},
      {label: 'Transmitter.Identifier', value: 'Transmitter.Identifier'},
      {label: 'Writer.Activity', value: 'Writer.Activity'},
      {label: 'Writer.BirthDate', value: 'Writer.BirthDate'},
      {label: 'Writer.BirthPlace.PostalCode', value: 'Writer.BirthPlace.PostalCode'},
      {label: 'Writer.DeathDate', value: 'Writer.DeathDate'},
      {label: 'Writer.DeathPlace.PostalCode', value: 'Writer.DeathPlace.PostalCode'},
      {label: 'Writer.Function', value: 'Writer.Function'},
      {label: 'Writer.Identifier', value: 'Writer.Identifier'},
      {label: '#elimination.OperationId', value: '#elimination.OperationId'},
      {label: '#elimination.GlobalStatus', value: '#elimination.GlobalStatus'},
      {label: '#elimination.DestroyableOriginatingAgencies', value: '#elimination.DestroyableOriginatingAgencies'},
      {label: '#elimination.NonDestroyableOriginatingAgencies', value: '#elimination.NonDestroyableOriginatingAgencies'},
      {label: '#elimination.ExtendedInfo', value: '#elimination.ExtendedInfo'},
      {label: '#elimination.ExtendedInfo.ExtendedInfoType', value: '#elimination.ExtendedInfo.ExtendedInfoType'},
      {label: '#elimination.ExtendedInfo.ExtendedInfoDetails', value: '#elimination.ExtendedInfo.ExtendedInfoDetails'},
      {label: '#elimination.ExtendedInfo.ExtendedInfoDetails.ParentUnitId', value: '#elimination.ExtendedInfo.ExtendedInfoDetails.ParentUnitId'},
      {label: '#elimination.ExtendedInfo.ExtendedInfoDetails.DestroyableOriginatingAgencies', value: '#elimination.ExtendedInfo.ExtendedInfoDetails.DestroyableOriginatingAgencies'},
      {label: '#elimination.ExtendedInfo.ExtendedInfoDetails.NonDestroyableOriginatingAgencies', value: '#elimination.ExtendedInfo.ExtendedInfoDetails.NonDestroyableOriginatingAgencies'},
      {label: '#glpd', value: '#glpd'},
      {label: '#graph', value: '#graph'},
      {label: '#max', value: '#max'},
      {label: '#mgt.AccessRule.Inheritance.PreventInheritance', value: '#mgt.AccessRule.Inheritance.PreventInheritance'},
      {label: '#mgt.AccessRule.Inheritance.PreventRulesId', value: '#mgt.AccessRule.Inheritance.PreventRulesId'},
      {label: '#mgt.AccessRule.Rules.EndDate', value: '#mgt.AccessRule.Rules.EndDate'},
      {label: '#mgt.AccessRule.Rules.Rule', value: '#mgt.AccessRule.Rules.Rule'},
      {label: '#mgt.AccessRule.Rules.StartDate', value: '#mgt.AccessRule.Rules.StartDate'},
      {label: '#mgt.AppraisalRule.FinalAction', value: '#mgt.AppraisalRule.FinalAction'},
      {label: '#mgt.AppraisalRule.Inheritance.PreventInheritance', value: '#mgt.AppraisalRule.Inheritance.PreventInheritance'},
      {label: '#mgt.AppraisalRule.Inheritance.PreventRulesId', value: '#mgt.AppraisalRule.Inheritance.PreventRulesId'},
      {label: '#mgt.AppraisalRule.Rules.EndDate', value: '#mgt.AppraisalRule.Rules.EndDate'},
      {label: '#mgt.AppraisalRule.Rules.Rule', value: '#mgt.AppraisalRule.Rules.Rule'},
      {label: '#mgt.AppraisalRule.Rules.StartDate', value: '#mgt.AppraisalRule.Rules.StartDate'},
      {label: '#mgt.ClassificationRule.Inheritance.PreventInheritance', value: '#mgt.ClassificationRule.Inheritance.PreventInheritance'},
      {label: '#mgt.ClassificationRule.Inheritance.PreventRulesId', value: '#mgt.ClassificationRule.Inheritance.PreventRulesId'},
      {label: '#mgt.ClassificationRule.Rules.EndDate', value: '#mgt.ClassificationRule.Rules.EndDate'},
      {label: '#mgt.ClassificationRule.Rules.Rule', value: '#mgt.ClassificationRule.Rules.Rule'},
      {label: '#mgt.ClassificationRule.Rules.StartDate', value: '#mgt.ClassificationRule.Rules.StartDate'},
      {label: '#mgt.DisseminationRule.Inheritance.PreventInheritance', value: '#mgt.DisseminationRule.Inheritance.PreventInheritance'},
      {label: '#mgt.DisseminationRule.Inheritance.PreventRulesId', value: '#mgt.DisseminationRule.Inheritance.PreventRulesId'},
      {label: '#mgt.ClassificationRule.ClassificationLevel', value: '#mgt.ClassificationRule.ClassificationLevel'},
      {label: '#mgt.ClassificationRule.ClassificationAudience', value: '#mgt.ClassificationRule.ClassificationAudience'},
      {label: '#mgt.ClassificationRule.ClassificationReassessingDate', value: '#mgt.ClassificationRule.ClassificationReassessingDate'},
      {label: '#mgt.ClassificationRule.NeedReassessingAuthorization', value: '#mgt.ClassificationRule.NeedReassessingAuthorization'},
      {label: '#mgt.DisseminationRule.Rules.EndDate', value: '#mgt.DisseminationRule.Rules.EndDate'},
      {label: '#mgt.DisseminationRule.Rules.Rule', value: '#mgt.DisseminationRule.Rules.Rule'},
      {label: '#mgt.DisseminationRule.Rules.StartDate', value: '#mgt.DisseminationRule.Rules.StartDate'},
      {label: '#mgt.ReuseRule.Inheritance.PreventInheritance', value: '#mgt.ReuseRule.Inheritance.PreventInheritance'},
      {label: '#mgt.ReuseRule.Inheritance.PreventRulesId', value: '#mgt.ReuseRule.Inheritance.PreventRulesId'},
      {label: '#mgt.ReuseRule.Rules.EndDate', value: '#mgt.ReuseRule.Rules.EndDate'},
      {label: '#mgt.ReuseRule.Rules.Rule', value: '#mgt.ReuseRule.Rules.Rule'},
      {label: '#mgt.ReuseRule.Rules.StartDate', value: '#mgt.ReuseRule.Rules.StartDate'},
      {label: '#mgt.StorageRule.FinalAction', value: '#mgt.StorageRule.FinalAction'},
      {label: '#mgt.StorageRule.Inheritance.PreventInheritance', value: '#mgt.StorageRule.Inheritance.PreventInheritance'},
      {label: '#mgt.StorageRule.Inheritance.PreventRulesId', value: '#mgt.StorageRule.Inheritance.PreventRulesId'},
      {label: '#mgt.StorageRule.Rules.EndDate', value: '#mgt.StorageRule.Rules.EndDate'},
      {label: '#mgt.StorageRule.Rules.Rule', value: '#mgt.StorageRule.Rules.Rule'},
      {label: '#mgt.StorageRule.Rules.StartDate', value: '#mgt.StorageRule.Rules.StartDate'},
      {label: '#mgt.NeedAuthorization', value: '#mgt.NeedAuthorization'},
      {label: '#min', value: '#min'},
      {label: '#nbc', value: '#nbc'},
      {label: '#og', value: '#og'},
      {label: '#ops', value: '#ops'},
      {label: '#opi', value: '#opi'},
      {label: '#score', value: '#score'},
      {label: '#sp', value: '#sp'},
      {label: '#sps', value: '#sps'},
      {label: '#storage._nbc', value: '#storage._nbc'},
      {label: '#storage.offerIds', value: '#storage.offerIds'},
      {label: '#storage.strategyId', value: '#storage.strategyId'},
      {label: '#tenant', value: '#tenant'},
      {label: '#uds', value: '#uds'},
      {label: '#unitType', value: '#unitType'},
      {label: '#unused', value: '#unused'},
      {label: '#up', value: '#up'},
      {label: '#us', value: '#us'},
      {label: '#us_sp', value: '#us_sp'},
      {label: '#v', value: '#v'},
      {label: '#id', value: '#id'}
    ];
    public advancedSearchFields = [
        new FieldDefinition('title', 'Intitulé', 4, 12),
        new FieldDefinition('description', 'Description', 4, 12),
        FieldDefinition.createIdField('id', 'Identifiant', 4, 12),
        FieldDefinition.createDateField('startDate', 'Date de début', 4, 12),
        FieldDefinition.createDateField('endDate', 'Date de fin', 4, 12),
        new FieldDefinition('originatingagencies', 'Service producteur de l\'entrée', 4, 12),
        FieldDefinition.createSelectField('ruleCategory', 'Catégorie de règle', 'Catégorie',
          this.archiveUnitHelper.rulesCategories.map(x => ({value: x.rule, label: x.label})), 4, 12, ArchiveUnitComponent.updateValues),
        FieldDefinition.createDateField('ruleDateSup', 'Date d\'échéance', 4, 12),
        FieldDefinition.createDynamicSelectField('ruleFinalAction', 'Sort final',
            this.makeDynamicFinalActions(), 'select', ArchiveUnitComponent.computeFinalActions , 4, 12),
        FieldDefinition.createSelectField('fieldArchiveUnit', 'Champs à choisir', 'AcquiredDate', this.fieldOptions, 4, 12),
      new FieldDefinition('valueArchiveUnit', 'Valeur', 4, 12),
    ];

    static updateValues(allData: FieldDefinition[], searchForm: FormGroup): void {
        const updatingField: FieldDefinition[] = allData.filter((x) => 'ruleFinalAction' === x.name);

        if (updatingField && updatingField.length === 1) {
            updatingField[0].options = ArchiveUnitComponent.computeFinalActions(updatingField[0].baseOptions, searchForm.value.ruleCategory);
        }
    }

    static computeFinalActions(items: DynamicSelectItem[], otherData: string): SelectItem[] {
        if (!otherData || otherData === '') {
            return  [];
        }

        return DynamicSelectItem.toSelectItems(items.filter(x => otherData === x.data));
    }

    makeDynamicFinalActions() {
        let finalActions: DynamicSelectItem[] = [];

      finalActions = finalActions.concat(
            this.archiveUnitHelper.finalActionSelector.AppraisalRule
              .map(x => new DynamicSelectItem(x.label, x.value, 'AppraisalRule')));
      finalActions = finalActions.concat(
            this.archiveUnitHelper.finalActionSelector.StorageRule
              .map(x => new DynamicSelectItem(x.label, x.value, 'StorageRule')));

        return finalActions;
    }

    public columns = [
        ColumnDefinition.makeStaticColumn('#id', 'Identifiant', undefined,
            () => ({ 'width': '325px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Intitulé', this.archiveUnitHelper.getTitle, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeStaticColumn('#unitType', 'Type', this.archiveUnitHelper.transformType,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeStaticColumn('#originating_agency', 'Service producteur', undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus ancienne', this.archiveUnitHelper.getStartDate, DateService.handleDate,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus récente', this.archiveUnitHelper.getEndDate, DateService.handleDate,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeSpecialIconColumn('Objet(s) disponible(s)',
            (data) => data['#object'] ? ['fa-check'] : ['fa-close greyColor'], () => ({ 'width': '100px' }), null, null, false),
        ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], (item) => this.routeToLFC(item),
            () => true, () => ({ 'width': '50px' }), null, false),
        ColumnDefinition.makeSpecialIconColumn('Ajout au panier', ArchiveUnitComponent.getBasketIcons, () => {},
          (item, service, icon) => {
            let message = '';
            switch(icon) {
            case 'fa-file':
              this.selectionService.addToSelection(false, [item['#id']], this.resourceService.getTenant());
              message = 'L\'unité archivistique à bien été ajouté au panier';
              break;
            case 'fa-sitemap':
              this.selectionService.addToSelection(true, [item['#id']], this.resourceService.getTenant());
              message = 'L\'unité archivistique et sa déscendance ont bien étés ajoutés au panier';
              break;
              case 'fa-archive':
              // TODO: Think about change that in order to set opi and not all ids
              this.selectionService.getIdsToSelect(true, item['#opi']).subscribe(
                (response) => {
                  const ids: string[] = response.$results.reduce(
                    (x, y) => {
                      x.push(y['#id']);
                      return x;
                    }, []);
                  this.selectionService.addToSelection(false, ids, this.resourceService.getTenant());
                }, () => {
                  console.log('Error while get archive from opi')
                }
              );

              this.selectionService.addToSelection(false, [item['#id']], this.resourceService.getTenant());
              message = 'L\'unité archivistique et les unité de son éntrée ont bien étés ajoutés au panier';
              break;
            default:
              console.log('Error ? Impossible de reconnaitre l\'action');
            }

            this.dialogService.displayMessage(message, 'Ajout au panier')
            // TODO: Display message ?
      }, null, false, null, ArchiveUnitComponent.getBasketIconsLabel)
    ];

    static getBasketIconsLabel(icon): string {
        switch(icon) {
          case 'fa-file': return 'Unité archivistique seule';
          case 'fa-sitemap': return 'Unitié archivistique et sa déscendance';
          case 'fa-archive': return 'Unité archivistique et son entrée';
          default: return '';
        }
    }

    static getBasketIcons(): string[] {
        return ['fa-file', 'fa-sitemap', 'fa-archive'];
    }

    public extraColumns = [];

    static addCriteriaProjection(criteriaSearch) {
        criteriaSearch.projection_startdate = 'StartDate';
        criteriaSearch.projection_enddate = 'EndDate';
        criteriaSearch.projection_createddate = 'CreatedDate';
        criteriaSearch.projection_acquireddate = 'AcquiredDate';
        criteriaSearch.projection_sentdate = 'SentDate';
        criteriaSearch.projection_receiveddate = 'ReceivedDate';
        criteriaSearch.projection_registereddate = 'RegisteredDate';
        criteriaSearch.projection_transactdate = 'TransactedDate';
        criteriaSearch.projection_descriptionlevel = 'DescriptionLevel';
        criteriaSearch.projection_originatingagencies = '#originating_agency';
        criteriaSearch.projection_id = '#id';
        criteriaSearch.projection_opi = '#opi';
        criteriaSearch.projection_unitType = '#unittype';
        criteriaSearch.projection_title = 'Title';
        criteriaSearch.projection_titlefr = 'Title_.fr';
        criteriaSearch.projection_object = '#object';
        criteriaSearch.orderby = { field: 'TransactedDate', sortType: 'ASC' };

        return criteriaSearch;
    }

    constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, public service: ArchiveUnitService,
                public archiveUnitHelper: ArchiveUnitHelper, private router: Router, private selectionService: MySelectionService,
                private resourceService: ResourcesService, public dialogService: DialogService) {
        super('Recherche d\'archives', breadcrumb, titleService, breadcrumbService);
    }

    pageOnInit() {
        if (ArchiveUnitService.getInputRequest() && ArchiveUnitService.getInputRequest().originatingagencies) {
            const form = ArchiveUnitComponent.addCriteriaProjection({});
            form.originatingagencies = ArchiveUnitService.getInputRequest().originatingagencies;
            form.isAdvancedSearchFlag = 'Yes';
            this.service.getResults(form, 0).subscribe(
                (response) => {
                    this.response = response;
                    this.advancedMode = true;
                    // FIXME add 'originatingagencies' value in search form
                },
                (error) => console.log('Error: ', error)
            );
            delete ArchiveUnitService.getInputRequest().originatingagencies;
        }
    }

    routeToLFC(item) {
        this.router.navigate(['search/archiveUnit/' + item['#id'] + '/unitlifecycle']);
    }

    public preSearchFunction(request, advancedMode): Preresult {
        const criteriaSearch: any = {}; // TODO Type me !
        const preResult = new Preresult();
        preResult.searchProcessSkip = false;

        if (!!request.requestFacet) {
            criteriaSearch.requestFacet = request.requestFacet;
        }

        if (advancedMode) {
            if (request.id) {
                criteriaSearch.id = request.id;
            } else {
                if (request.title) { criteriaSearch.Title = request.title; }
                if (request.description) { criteriaSearch.Description = request.description; }
                if (request.originatingagencies) { criteriaSearch.originatingagencies = request.originatingagencies; }

                const isStartDate = request.startDate;
                const isEndDate = request.endDate;
                if (isStartDate && isEndDate) {
                    if (request.startDate > request.endDate) {
                        preResult.searchProcessError = 'La date de début doit être antérieure à la date de fin.';
                        return preResult;
                    }
                    criteriaSearch.StartDate = request.startDate;
                    criteriaSearch.EndDate = request.endDate;
                    criteriaSearch.EndDate.setDate(criteriaSearch.EndDate.getDate() + 1)
                } else if (isStartDate || isEndDate) {
                    preResult.searchProcessError = 'Une date de début et une date de fin doivent être indiquées.';
                    return preResult;
                }

                if (request.ruleCategory) {
                    criteriaSearch.RuleCategory = request.ruleCategory;
                }

                if (request.ruleDateSup) {
                    criteriaSearch.RuleDateSup = request.ruleDateSup;
                    criteriaSearch.RuleDateSup.setDate(criteriaSearch.RuleDateSup.getDate() + 1)
                }

                if (request.ruleFinalAction) {
                    criteriaSearch.RuleFinalAction = request.ruleFinalAction;
                }
                if (request.fieldArchiveUnit && request.valueArchiveUnit) {
                  criteriaSearch.fieldArchiveUnit = request.fieldArchiveUnit;
                  criteriaSearch.valueArchiveUnit = request.valueArchiveUnit;
                }
            }

            if (criteriaSearch.id || criteriaSearch.Title || criteriaSearch.Description || criteriaSearch.StartDate
                || criteriaSearch.EndDate || criteriaSearch.originatingagencies || criteriaSearch.RuleCategory
                || criteriaSearch.RuleDateSup || criteriaSearch.RuleFinalAction || (criteriaSearch.fieldArchiveUnit && criteriaSearch.valueArchiveUnit)) {
                if (!!request.facets) {
                    criteriaSearch.facets = request.facets;
                }
                ArchiveUnitComponent.addCriteriaProjection(criteriaSearch);
                criteriaSearch.isAdvancedSearchFlag = 'Yes';
                preResult.request = criteriaSearch;
                preResult.success = true;
                return preResult;
            } else {
                preResult.searchProcessError = 'Aucun résultat. Veuillez entrer au moins un critère de recherche';
                return preResult;
            }

        } else {
            if (!!request.titleCriteria) {
                criteriaSearch.titleAndDescription = request.titleCriteria;
                if (!!request.facets) {
                    criteriaSearch.facets = request.facets;
                }
                ArchiveUnitComponent.addCriteriaProjection(criteriaSearch);
                criteriaSearch.isAdvancedSearchFlag = 'No';

                preResult.request = criteriaSearch;
                preResult.success = true;
                return preResult;
            } else {
                preResult.searchProcessError = 'Aucun résultat. Veuillez entrer au moins un critère de recherche';
                return preResult;
            }
        }

    }

    public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
        service.getResults(form, offset).subscribe(
            (response) => {
                responseEvent.emit({ response: response, form: form });
            },
            (error) => console.log('Error: ', error)
        );
    }

    onNotify(event) {
        this.response = event.response;
        this.searchForm = event.form;
    }

    onChangedSearchRequest(searchRequest) {
        this.searchRequest = searchRequest;
    }

    /**
     * clear results.
     */
    onClear() {
        delete this.response;
    }

    public paginationSearch(service: any, offset) {
        return service.getResults(this.searchForm, offset);
    }

    // FIXME: Unused method ?
    public onClearPressed() {
        delete this.response;
    }

    onChangedSearchMode(searchMode) {
        this.advancedMode = searchMode;
    }

    onDisabledFiled(isToDisable) {
        this.disabledFacet = isToDisable;
    }

}
