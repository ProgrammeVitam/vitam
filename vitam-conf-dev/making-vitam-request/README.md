# Making Rest request on VITAM
## Prerequisite
1. Having VSCode that can make Https request with certificate see [#18557](https://github.com/electron/electron/issues/18557) and [#18380](https://github.com/electron/electron/issues/18380)
2. Having RestClient VSCode extension installed
3. Having the VITAM certificate for the environments
4. Having GIT

## Installation
1. Put in your Rest Client extension file (File > Preferences > Settings > Extensions > Rest Client > Edit in settings.json) the `settings.json` file and change the `pfx`, `cert` and `key` path.
2. Specify the `INT` environment (click on the `No Environnment` on the bottom right of VSCode).
3. Try the search request (located in `vitam-conf-dev/making-vitam-request/requests-workspace/Search/search.http`).
3. Add the requests-workspace directory in VSCode workspace (File > Add Folder to Workspace).

## Synchronize and download
Download and synchronize the workspace with:
```shell script
git clone https://gitlab.dev.programmevitam.fr/vitam/vitam.git --depth 1
git fetch 
git checkout origin/master -- vitam-conf-dev/making-vitam-request/requests-workspace
```