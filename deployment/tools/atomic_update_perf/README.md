# Test de perf du workflow bulk atomic update

Le script `atomic_update.py` permet de générer un fichier de requête de mise à jour en masse atomique (Bulk Atomic Update).
Il prend en entrée une liste d'opérations ingest, et génère une requête d'update unitaire pour chaque unité.
Le fichier de requête généré peut être alors utilisé comme body pour lancer un workflow de bulk atomic update.

Ce script a vocation à être utilisé pour générer des jeux de test de perf. Il peut être adapté pour générer des requêtes plus complexe si besoin.

## Génération de fichier de requête

**Usage :**
```chmod +x atomic_update.py
./atomic_update.py [params...]
-e : elasticsearch url             (eg. http://localhost:9200/, https://xyz.env.programmevitam.fr/elasticsearch-data)
-u : basic auth user name (opt)    (eg. username)
-t : tenant                        (eg. 0)
-o : comma-separated operation ids (eg. aeeaaaaaachn7hk2aaatkalxwxodlfyaaaaq,aecaaaaaachmf443aay7ialybbjgxyyaaaba)
-m : Max threshold (default=10000) (eg. 100000)
-f : Output query file (opt)       (eq. ./query.json)
```

**Exemples :** 
```
   ./atomic_update.py -e  https://xyz.env.programmevitam.fr/elasticsearch-data -t 0 -o "guid1, guid2..." -m 100000 -u basic_auth_username -f query.json
   ./atomic_update.py -e  http://localhost:9200 -t 0 -o "guid1, guid2..." -m 1000
```

## Lancement du workflow

```
curl --request POST --url https://xyz.env.programmevitam.fr/access-external/v1/units/bulk \
     --header 'accept: application/json' \
     --header 'content-type: application/json' \
     --header 'x-access-contract-id: ContratTNR' \
     --header 'x-tenant-id: 0' \
     --data @query.json \
     --cert "path/to/externe_pub.pem"
     --key "path/to/externe_key.pem"
```
