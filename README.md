# Java ORAS CLI

Test ORAS Java SDK 

## Usage

Make sure the ORAS java SDK is compiled and installed on the local maven cache (Not distributed yet)

```shell
mvn clean install
```

### Run

Docker a local registry

```shell
$ docker run -d -p 5000:5000 ghcr.io/project-zot/zot-linux-amd64:v2.1.2
```

### Push an Artifact

```shell
$ echo "Hello World" > hi.txt
$ java -jar java-oras-cli/target/oras-java.jar push --insecure --file hi.txt localhost:5000/hello:v1
Uploaded: sha256:d2a84f4b8b650937ec8f73cd8be2c74add5a911ba64df27458ed8229da804a26
```

### Push a Blob

```shell
$ java -jar java-oras-cli/target/oras-java.jar blob-push --file pom.xml --insecure localhost:5000/hello:v1
Pushed blob with digest sha256:fe46cc83694e6476a22d25bbfc0c91dcadf37b123767d82f7a581dbd129eb641
```

## Push manifest

```shell
$ java -jar java-oras-cli/target/oras-java.jar manifest-push --file empty-manifest.json --insecure localhost:5000/hello:v1
Pushed manifest to http://localhost:5000/v2/hello/manifests/sha256:adaed35e3a0e62e47074aece95c96f34079f70a549f8dcf284aba4f9080291d3
```

### Pull an Artifact

```shell
rm -f hi.txt
java -jar java-oras-cli/target/oras-java.jar pull --insecure localhost:5000/hello:v1
```

### Pull a blob
    
```shell
$ java -jar java-oras-cli/target/oras-java.jar blob-fetch --output my-blob --insecure localhost:5000/hello:v1@sha256:fe46cc83694e6476a22d25bbfc0c91dcadf37b123767d82f7a581dbd129eb641
Fetching blob... 
Fetched blob on /home/vald/git/github/oras-java-cli/my-blob
```

### Delete a blob

```shell 
$ java -jar java-oras-cli/target/oras-java.jar blob-delete --insecure localhost:5000/hello:v1@sha256:fe46cc83694e6476a22d25bbfc0c91dcadf37b123767d82f7a581dbd129eb641
Deleting blob... 
Deleted blob
```

### Pull a manifest

```shell
$ java -jar java-oras-cli/target/oras-java.jar manifest-fetch --output my-manifest --insecure localhost:5000/hello:v1
$ cat my-manifest
{"schemaVersion":2,"mediaType":"application/vnd.oci.image.manifest.v1+json","config":{"mediaType":"application/vnd.oci.empty.v1+json","digest":"sha256:44136fa355b3678a1146ad16f7e86
49e94fb4fc21fe77e8310c060f61caaff8a","size":2},"layers":[{"mediaType":"application/vnd.oci.empty.v1+json","digest":"sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a","size":2,"data":"e30\u003d"}],"annotations":{}}
```
