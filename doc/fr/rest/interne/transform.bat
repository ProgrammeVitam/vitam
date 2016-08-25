@echo on
raml2html -i Internal_Logbook.raml -t template\template.nunjucks -o web\Internal_Logbook.html
raml2html -i Internal_Metadata.raml -t template\template.nunjucks -o web\Internal_Metadata.html
raml2html -i Internal_Processing.raml -t template\template.nunjucks -o web\Internal_Processing.html
raml2html -i Internal_Storage.raml -t template\template.nunjucks -o web\Internal_Storage.html
raml2html -i Internal_Worker.raml -t template\template.nunjucks -o web\Internal_Worker.html
raml2html -i Internal_Workspace.raml -t template\template.nunjucks -o web\Internal_Workspace.html
raml2html -i Internal_Ingest.raml -t template\template.nunjucks -o web\Internal_Ingest.html
raml2html -i Module_Acces.raml -t template\template.nunjucks -o web\Module_Acces.html
raml2html -i Module_Ingest.raml -t template\template.nunjucks -o web\Module_Ingest.html
raml2html -i Module_Logbook.raml -t template\template.nunjucks -o web\Module_Logbook.html
