Modèle de données
*****************
Afin d'assurer le suivi des opérations du journal du cycle de vie effectuées sur les archives,
un ensemble d'informations sont conservées.

Description des champs
======================
Les noms des champs sont basés sur les distinctions faites par PREMIS V3 entre :

* objet / agent / évènement
* type / identifiant

Les champs seront tous au même niveau dans le journal du cycle de vie ==> pas de notion de bloc comme dans PREMIS, même si
on préserve la capacité à générer un schéma PREMIS (et les blocs qui le compose).

Référence: http://www.loc/gov/standard/premis/v3/premis-3-0-final.pdf

Ci-après la liste des champs stockés dans le journal des opérations  du journal du cycle de vie 
associées à leur correspondance métier :

.. csv-table::
        :header-rows: 1
        :stub-columns: 1
        :widths: 1,10,1,2,2,10
        :file: data/model.csv
        :delim: ,




.. _PREMIS: http://www.loc.gov/standards/premis/Understanding-PREMIS_french.pdf
