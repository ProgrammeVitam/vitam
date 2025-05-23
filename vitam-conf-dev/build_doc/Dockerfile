FROM debian:bookworm-slim

RUN apt update
RUN apt install -y net-tools wget unzip curl openjdk-21-jdk rpm librpm-dev dpkg dpkg-dev perl gnupg2 build-essential make python3-pip "latexmk" "texlive-latex-base" "texlive-*" "tetex-*" "tokyocabinet*" "texlive-*"

RUN curl https://archive.apache.org/dist/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz -o apache-maven-3.8.7-bin.tar.gz && tar -xzf apache-maven-3.8.7-bin.tar.gz --directory /usr/local/ && ln -s --force /usr/local/apache-maven-3.8.7 /usr/local/maven

RUN curl -L http://mirrors.ctan.org/macros/latex/contrib/tabulary.zip -o /tmp/tabulary.zip && unzip /tmp/tabulary.zip -d /tmp/ && cd /tmp/tabulary/ && latex tabulary.ins && cp /tmp/tabulary/tabulary.sty /usr/share/texlive/texmf-dist/tex/latex/tabulary/ && chmod 0755 /usr/share/texlive/texmf-dist/tex/latex/tabulary/tabulary.sty && texhash

RUN curl -L https://distrib-coffee.ipsl.jussieu.fr/pub/mirrors/ctan/macros/latex/contrib/needspace.zip -o /tmp/needspace.zip && unzip /tmp/needspace.zip -d /tmp/ && cd /tmp/needspace/ && latex needspace.ins &&  chmod 0755 /usr/share/texlive/texmf-dist/tex/latex/needspace/needspace.sty && texhash

# Install nvm with node and npm
ENV NVM_DIR /root/.nvm
ENV NODE_VERSION 10.24.0
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash \
    && . $NVM_DIR/nvm.sh \
    && nvm install $NODE_VERSION \
    && nvm alias default $NODE_VERSION \
    && nvm use default
ENV NODE_PATH $NVM_DIR/versions/node/v$NODE_VERSION/lib/node_modules
ENV PATH      $NVM_DIR/versions/node/v$NODE_VERSION/bin:$PATH

RUN rm /usr/lib/python3.*/EXTERNALLY-MANAGED
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
