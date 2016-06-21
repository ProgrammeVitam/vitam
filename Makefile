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
SRCDIR        = $(MODULE)/doc/$(LANGUAGE)
BUILDDIR      = $(SRCDIR)/target

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

.PHONY: help
help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  html       to make standalone HTML files"
	@echo "  latexpdf   to make LaTeX files and run them through pdflatex"

.PHONY: clean
clean:
	rm -rf $(BUILDDIR)

.PHONY: html
html:
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b html -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(BUILDDIR)/$${item}/html; \
		echo "HTML Build finished. The HTML pages are in \"$${item}/html\"."; \
	done;

.PHONY: latexpdf
latexpdf:
	#FIXME sed correspond à un contournement pour Centos 7
	for item in $(LIST_ITEMS); do \
		echo "Building \"$${item}\""; \
		$(SPHINXBUILD) -q -b latex -d $(BUILDDIR)/$${item}/doctrees $(ALLSPHINXOPTS) $(SRCDIR)/$${item} $(BUILDDIR)/$${item}/latex; \
		echo "Running post-treatment on LaTeX files..."; \
		sed -i 's/\\usepackage{eqparbox}/%\\usepackage{eqparbox}/g' $(BUILDDIR)/$${item}/latex/*.tex; \
		sed -i 's/\\RequirePackage{upquote}/%\\RequirePackage{upquote}/g' $(BUILDDIR)/$${item}/latex/sphinx.sty; \
		sed -i 's/\\RequirePackage{capt-of}/%\\RequirePackage{capt-of}/g' $(BUILDDIR)/$${item}/latex/sphinx.sty; \
		sed -i 's/\\RequirePackage{needspace}/%\\RequirePackage{needspace}/g' $(BUILDDIR)/$${item}/latex/sphinx.sty; \
		echo "Converting LaTeX files to PDF..."; \
		$(MAKE) -C $(BUILDDIR)/$${item}/latex all-pdf; \
		echo "PDF Build finished; the PDF files are in \"$${item}/latex\"."; \
	done;


.PHONY: all
all:
	for module in $(LIST_MODULES); do \
		$(MAKE) clean html latexpdf MODULE=$${module}; \
	done;
	
