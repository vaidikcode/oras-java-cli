package land;

import land.oras.ContainerRef;
import land.oras.Manifest;
import land.oras.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class ExampleTest {

    @TempDir
    private Path blobDir;

    @Test
    public void test() {

        // Push
        Path artifact = Path.of("pom.xml");
        Registry registry = Registry.Builder.builder()
                .withInsecure(true)
                .build();
        Manifest manifest = registry.pushArtifact(ContainerRef.parse("localhost:5000/hello:v1"), artifact);

        // Pull
        registry.pullArtifact(ContainerRef.parse("localhost:5000/hello:v1"), blobDir);

    }

}
