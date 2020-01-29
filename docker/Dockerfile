FROM openjdk:11-jdk as builder

RUN apt-get update && apt-get install -y curl tar

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL https://apache.osuosl.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN mkdir /build
WORKDIR /build

RUN git clone https://github.com/knaw-huc/resussun.git
WORKDIR /build/resussun
RUN mvn clean package

FROM openjdk:11-jre-slim

COPY --from=builder  /build/resussun/target/appassembler /app
COPY --from=builder /build/resussun/config.yml /app/config.yml
WORKDIR /app
EXPOSE 8080
CMD ["./bin/resussun", "server", "./config.yml"]
