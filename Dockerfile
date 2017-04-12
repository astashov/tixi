FROM openjdk:8-jre-alpine
MAINTAINER Celso
ENV LEIN_ROOT 1
RUN apk --update add bash curl && \ 
 curl -L "https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein" -o /usr/local/bin/lein  && \ 
 apk --no-cache add openssl && \ 
 mkdir /tixi && chmod -R 755 /tixi
COPY . /tixi
COPY entrypoint.sh /
RUN chmod -R +x /usr/local/bin/lein && \ 
 chmod -R 755 /usr/local/bin/lein 
RUN lein upgrade
RUN cd /tixi && \ 
 lein cljsbuild once
RUN apk del curl && \ 
 rm -rf /etc/ssl /var/cache/apk/*
EXPOSE 3449
ENTRYPOINT ["/entrypoint.sh"]
