Ajout d'un plugin
=================

1. Veuillez placer les fichiers `.jar` dans ce dossier `~/vitam/deployment/ansible-vitam/roles/vitam/files/worker/plugins-workspace/`
2. N'oubliez pas de mettre à jour le fichier `plugins.json` qui se trouve dans le dossier `~/vitam/deployment/ansible-vitam/roles/vitam/files/worker/plugins.json`
   Ajouter d'une entrée (exemple):

    ```json
        "HELLO_WORLD_PLUGIN": {
            "className": "fr.vitam.plugin.custom.HelloWorldPlugin",
            "propertiesFile": "hellow_world_plugin.properties",
            "jarName": "hellow-world-plugin.jar"
        }
    ```

3. Pour faire marcher votre plugin, soit :
    - Ajouter une action dans un workflow existant avec comme `actionKey` la clé (dans l'exemple ci-dessus) `HELLO_WORLD_PLUGIN`
    - Créer carrément un nouveau fichier workflow

4. Dans le cas de création d'un nouveau fichier workflow, veuillez vous référer à l'exemple qui se trouve dans le dossier `~/vitam/deployment/ansible-vitam/roles/vitam/files/processing/workflows/`

Requirements
------------

VITAM has already been deployed

License
-------

Cecill 2.1

Auteur
------

Projet VITAM
