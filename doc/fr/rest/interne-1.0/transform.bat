@echo on
raml2html -i Acces.raml -t template\template.nunjucks -o web\Acces.html
raml2html -i Ingest.raml -t template\template.nunjucks -o web\Ingest.html
raml2html -i Logbook.raml -t template\template.nunjucks -o web\Logbook.html
raml2html -i Metadata.raml -t template\template.nunjucks -o web\Metadata.html
raml2html -i Processing.raml -t template\template.nunjucks -o web\Processing.html
raml2html -i Storage.raml -t template\template.nunjucks -o web\Storage.html
raml2html -i Worker.raml -t template\template.nunjucks -o web\Worker.html
raml2html -i Workspace.raml -t template\template.nunjucks -o web\Workspace.html
