# Java ORAS CLI

Test ORAS Java SDK 

## Build

Make sure the ORAS java SDK is compiled and installed on the local maven cache (Not distributed yet)

```shell
mvn clean install
```

## Run

Docker a local registry

```shell
docker run -d -p 5000:5000 ghcr.io/oras-project/registry:latest
```

### Push an Artifact

```shell
echo "Hello World" > hi.txt
java -jar java-oras-cli/target/oras-java.jar push --insecure --file hi.txt localhost:5000/hello:v1
Uploaded: sha256:d2a84f4b8b650937ec8f73cd8be2c74add5a911ba64df27458ed8229da804a26
```

## Pull an Artifact

```shell
rm -f hi.txt
java -jar java-oras-cli/target/oras-java.jar pull --insecure localhost:5000/hello:v1
```

