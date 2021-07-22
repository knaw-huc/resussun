FROM openjdk:11-jdk as builder

RUN apt-get update && apt-get install -y curl tar

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL https://apache.osuosl.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN mkdir -p /build/resussun
WORKDIR /build/resussun

COPY . /build/resussun
RUN mvn clean package

FROM openjdk:11-jre-slim

COPY --from=builder  /build/resussun/target/appassembler /app
COPY --from=builder /build/resussun/config.yml /app/config.yml
WORKDIR /app
ENV SERVER_PORT=80 ADMIN_PORT=81
EXPOSE 80
EXPOSE 81
CMD ["./bin/resussun", "server", "./config.yml"]
