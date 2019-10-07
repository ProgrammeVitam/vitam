@rem ***************************************************************************
@rem Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
@rem
@rem contact.vitam@culture.gouv.fr
@rem
@rem This software is a computer program whose purpose is to implement a digital archiving back-office system managing
@rem high volumetry securely and efficiently.
@rem
@rem This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
@rem software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
@rem circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
@rem
@rem As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
@rem users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
@rem successive licensors have only limited liability.
@rem
@rem In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
@rem developing or reproducing the software by the user in light of its specific status of free software, that may mean
@rem that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
@rem experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
@rem software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
@rem to be ensured and, more generally, to use and operate it in the same conditions as regards security.
@rem
@rem The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
@rem accept its terms.
@rem ***************************************************************************
@echo on
raml2html -i Acces.raml -o web\Acces.html
raml2html -i Ingest.raml -o web\Ingest.html
raml2html -i Logbook.raml -o web\Logbook.html
raml2html -i Metadata.raml -o web\Metadata.html
raml2html -i Processing.raml -o web\Processing.html
raml2html -i Storage.raml -o web\Storage.html
raml2html -i Worker.raml -o web\Worker.html
raml2html -i Workspace.raml -o web\Workspace.html
raml2html -i Technical-administration.raml -o web\Technical-administration.html
