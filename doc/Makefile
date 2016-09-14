# Makefile for Sphinx documentation
#

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
MAKE	      = make
PAPER         =
LANGUAGE      = fr
DOC           = 
MODULE        = .
SRCDIR        = ./$(LANGUAGE)
BUILDDIR      = ./target
RAMLCONTENT	  = $(SRCDIR)/rest/interne
RAMLBUILDDIR  = $(BUILDDIR)/raml
HTMLBUILDDIR  = $(BUILDDIR)/vitam
PDFBUILDDIR   = $(BUILDDIR)/pdf
AUTRECONTENT    = $(SRCDIR)/autres
AUTREBUILD      = $(BUILDDIR)/autres

# User-friendly check for sphinx-build
ifeq ($(shell which $(SPHINXBUILD) >/dev/null 2>&1; echo $$?), 1)
$(error The '$(SPHINXBUILD)' command was not found. Make sure you have Sphinx installed, then set the SPHINXBUILD environment variable to point to the full path of the '$(SPHINXBUILD)' executable. Alternatively you can add the directory with the executable to your PATH. If you don't have Sphinx installed, grab it from http://sphinx-doc.org/)
endif

# Internal variables.
PAPEROPT_a4     = -D latex_paper_size=a4
PAPEROPT_letter = -D latex_paper_size=letter
ALLSPHINXOPTS   = $(PAPEROPT_$(PAPER)) $(SPHINXOPTS)
# the i18n builder cannot share the environment and doctrees with the others
I18NSPHINXOPTS  = $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) .

LIST_ITEMS	= $(shell find $(SRCDIR) -name "conf.py" -type f | sed 's/\/conf.py//g' | sed 's/.*fr\///g')
LIST_MODULES	= $(shell find . -name "doc" -type d | sed 's/\/doc//g' | sed 's/.*\///g')
LIST_RAML = $(shell cat ${RAMLCONTENT}/index)
LISTE_AUTRES = $(shell cat ${RAMLCONTENT}/index)


.PHONY: help
help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  clean      to delete output"
	@echo "  html       to make standalone HTML files"
	@echo "  latexpdf   to make LaTeX files and run them through pdflatex"
	@echo "  raml       to make standaloneHTML files from RAML"

.PHONY: clean
clean:
	rm -rf $(BUILDDIR); \
	rm -rf $(RAMLBUILDDIR); \
	rm -rf $(HTMLBUILDDIR); \
	rm -rf ${PDFBUILDDIR}; \
	rm -rf ${AUTREBUILD};

.PHONY: html
html:
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b html -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(HTMLBUILDDIR)/$${item}; \
		echo "HTML Build finished. The HTML pages are in \"$${item}/html\"."; \
	done;

.PHONY: autres
autres:
	@echo "Création du répertoire de sortie"
	mkdir -p ${AUTREBUILD}; \
	cp -r ${AUTRECONTENT}/* ${AUTREBUILD}/


.PHONY: raml
raml:
	@echo "Création du répertoire de sortie"
	mkdir -p ${RAMLBUILDDIR}; \
	cd $(RAMLCONTENT); \
	for item in $(LIST_RAML); do \
		raml2html -i $${item}.raml -t template/template.nunjucks -o ../../../${RAMLBUILDDIR}/$${item}.html; \
	done;


.PHONY: latexpdf
latexpdf:
	mkdir -p ${PDFBUILDDIR}; \
	#FIXME sed correspond à un contournement pour Centos 7
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b latex -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(BUILDDIR)/$${item}/latex; \
		echo "Running post-treatment on LaTeX files..."; \
		sed -i 's/\\usepackage{eqparbox}/%\\usepackage{eqparbox}/g' $(BUILDDIR)/$${item}/latex/*.tex; \
		sed -i 's/\\RequirePackage{upquote}/%\\RequirePackage{upquote}/g' $(BUILDDIR)/$${item}/latex/sphinx.sty; \
		sed -i 's/\\RequirePackage{capt-of}/%\\RequirePackage{capt-of}/g' $(BUILDDIR)/$${item}/latex/sphinx.sty; \
		echo "Converting LaTeX files to PDF..."; \
		$(MAKE) -C $(BUILDDIR)/$${item}/latex all-pdf; \
		echo "Copy PDF files to OUTPUT..."; \
		mv $(BUILDDIR)/$${item}/latex/*.pdf $(PDFBUILDDIR); \
		echo "PDF Build finished; the PDF files are in $(PDFBUILDDIR)."; \
	done;


.PHONY: all
all:
	for module in $(LIST_MODULES); do \
		$(MAKE) clean html latexpdf MODULE=$${module}; \
	done;
	
