FROM debian:10.12

RUN apt update
RUN apt install -y net-tools wget unzip curl openjdk-11-jdk rpm librpm-dev dpkg dpkg-dev perl gnupg2 build-essential make createrepo npm nodejs python-pip "latexmk" "texlive-latex-base" "texlive-*" "tetex-*" "tokyocabinet*" "texlive-*"

RUN curl https://dlcdn.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -o apache-maven-3.6.3-bin.tar.gz && tar -xzf apache-maven-3.6.3-bin.tar.gz --directory /usr/local/ && ln -s --force /usr/local/apache-maven-3.6.3 /usr/local/maven

RUN curl -L http://mirrors.ctan.org/macros/latex/contrib/tabulary.zip -o /tmp/tabulary.zip && unzip /tmp/tabulary.zip -d /tmp/ && cd /tmp/tabulary/ && latex tabulary.ins && cp /tmp/tabulary/tabulary.sty /usr/share/texlive/texmf-dist/tex/latex/tabulary/ && chmod 0755 /usr/share/texlive/texmf-dist/tex/latex/tabulary/tabulary.sty && texhash
RUN curl -L http://mirrors.ctan.org/macros/latex/contrib/needspace.zip -o /tmp/needspace.zip && unzip /tmp/needspace.zip -d /tmp/ && cd /tmp/needspace/ && latex needspace.ins && cp /tmp/needspace/needspace.sty /usr/share/texlive/texmf-dist/tex/latex/needspace/needspace.sty && chmod 0755 /usr/share/texlive/texmf-dist/tex/latex/needspace/needspace.sty && texhash

RUN pip install --upgrade pip
RUN pip install sphinx==1.7.9
RUN pip install sphinx_rtd_theme

RUN npm install -g raml2html@7.6.0
RUN npm cache clean -f && npm install -g n && n stable

# Make user password-less
RUN apt-get -y install sudo
RUN echo 'ALL            ALL = (ALL) NOPASSWD: ALL' >> /etc/sudoers

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/bin/bash"]
