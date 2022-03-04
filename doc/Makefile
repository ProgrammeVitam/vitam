# Makefile for Sphinx documentation
#

# You can set these variables from the command line.
SHELL         = /bin/bash
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
MAKE	      = make
PAPER         =
LANGUAGE      = fr
DOC           =
SRCDIR        = ./$(LANGUAGE)
BUILDDIR      = ./target
RAMLCONTENTINTERNE  = $(SRCDIR)/rest/interne
RAMLCONTENTEXTERNE  = $(SRCDIR)/rest/externe
RAMLCONTENTCONSOLE   = $(SRCDIR)/rest/console
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
LIST_RAMLINTERNE = $(shell cat ${RAMLCONTENTINTERNE}/index)
LIST_RAMLEXTERNE = $(shell cat ${RAMLCONTENTEXTERNE}/index)
LISTE_AUTRES = $(shell cat ${RAMLCONTENT}/index)
LIST_COMPONENTS = $(shell find ../sources -maxdepth 1 -type d | sed 's/.*\///g' | sed 's/sources//g')


.PHONY: help
help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  clean      to delete output and symlinks"
	@echo "  symlinks   to create the symlinks for the detailed documentation (components)"
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



.PHONY: symlinks
symlinks:
	# Clean up, then add symlinks to modules documentation
	for item in $(LIST_ITEMS); do \
		for component in $(LIST_COMPONENTS); do \
			if [[ -e fr/$${item}/include/$${component} ]]; then \
				rm -f fr/$${item}/include/$${component}; \
			fi; \
		done; \
	done; \
	for item in $(LIST_ITEMS); do \
		for component in $(LIST_COMPONENTS); do \
			if [[ -d ../sources/$${component}/doc/fr/$${item} ]]; then \
				if [[ -d fr/$${item}/include ]]; then \
					if [[ ! -e fr/$${item}/include/$${component} ]]; then \
						ln -s ../../../../sources/$${component}/doc/fr/$${item} fr/$${item}/include/$${component}; \
					fi; \
				fi; \
			fi; \
		done; \
	done;

.PHONY: html
html:
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b html -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(HTMLBUILDDIR)/$${item} || exit 1; \
		echo "HTML Build finished. The HTML pages are in \"$(HTMLBUILDDIR)/$${item}\"."; \
	done;

.PHONY: autres
autres:
	@echo "Création du répertoire de sortie"
	mkdir -p ${AUTREBUILD}; \
	cp -r ${AUTRECONTENT}/* ${AUTREBUILD}/


.PHONY: raml
raml:
	@echo "Création du répertoire de sortie"
	mkdir -p ${RAMLBUILDDIR}/interne; \
	echo "Generation interne"; \
	cd $(RAMLCONTENTINTERNE); \
	cp *raml ../../../${RAMLBUILDDIR}/interne/; \
	./transform.sh; \
	cp -r target/html/* ../../../${RAMLBUILDDIR}/interne; \
	cd ../../..; \
	mkdir -p ${RAMLBUILDDIR}/externe; \
	echo "Generation externe"; \
	cd $(RAMLCONTENTEXTERNE); \
	./transform.sh; \
	cp -r target/html/* ../../../${RAMLBUILDDIR}/externe; \
	cd ../../..; \
	# OMA : commented as useless now ; seen with Frédéric BREGIER 19/10/2016
	# mkdir -p ${RAMLBUILDDIR}/console; \
	# echo "Recopie console"; \
	# cd $(RAMLCONTENTCONSOLE); \
	# cp -r * ../../../${RAMLBUILDDIR}/console/;

.PHONY: latexpdf
latexpdf:
	mkdir -p ${PDFBUILDDIR}; \
	#FIXME sed correspond à un contournement pour Centos 7
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b latex -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(BUILDDIR)/$${item}/latex || exit 1; \
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
