package land.oras;

import land.oras.auth.EnvironmentPasswordProvider;
import land.oras.auth.UsernamePasswordProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "oras-java",
        synopsisSubcommandLabel = "COMMAND",
        subcommands = {
                // Blobs
                Main.PushBlobCommand.class,
                Main.DeleteBlobCommand.class,
                Main.FetchBlob.class,

                // Manifests
                Main.PushManifest.class,
                Main.DeleteManifest.class,
                Main.FetchManifest.class,

                // Mush
                Main.ArtifactPush.class,
                Main.ArtifactPull.class
        },
        description = "Oras Java CLI")
public class Main implements Runnable {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    // Global log filter
    public static boolean DEBUG = false;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(final String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    private static void handleException(OrasException r) {
        LOG.error("response code: {}", r.getStatusCode());
        if (r.getError() != null) {
            LOG.error("code: {}", r.getError().code());
            LOG.error("message: {}", r.getError().message());
            LOG.error("details: {}", r.getError().details());
        }
        else {
            LOG.info("Exception message: {}", r.getMessage());
        }

    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /**
     * Reusable options
     */
    @CommandLine.Command(synopsisHeading = "%nUsage:%n",
            descriptionHeading = "%nDescription:%n",
            parameterListHeading = "%nParameters:%n",
            optionListHeading = "%nOptions:%n",
            commandListHeading = "%nCommands:%n")
    public static class ReusableOptions {

        // Define a positional parameter for the repository name
        @CommandLine.Parameters(index = "0", description = "The repository to pull from")
        private String repository;

        @CommandLine.Option(names = { "--username" }, description = {
                "Username"})
        private String username;

        @CommandLine.Option(names = { "--password" }, description = {
                "Username"})
        private String password;

        @CommandLine.Option(names = { "--debug" }, description = {
                "Enable debug mode"})
        private Boolean debug = false;

        @CommandLine.Option(names = { "--insecure" }, description = {
                "Allow insecure connections over HTTP"})
        private Boolean insecure = false;

        @CommandLine.Option(names = { "--skip-tls-verify" }, description = {
                "Skip TLS verification"})
        private Boolean skipTlsVerify = false;

    }
    @CommandLine.Command(name = "blob-delete", description = "Delete a blob")
    public static class DeleteBlobCommand implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(DeleteBlobCommand.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Deleting blob...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                registry.deleteBlob(containerRef);
                LOG.info("Deleted blob");
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "blob-push", description = "Push a blob")
    public static class PushBlobCommand implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(PushBlobCommand.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--file" }, required = true)
        private Path file;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Pushing blob...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                Layer layer = registry.uploadBlob(containerRef, file);
                LOG.info("Pushed blob with digest " + layer.getDigest());
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "blob-fetch", description = "Fetch a manifest")
    public static class FetchBlob implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(FetchBlob.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--output" }, required = true)
        private File output;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Fetching blob...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                registry.fetchBlob(containerRef, output.toPath());
                LOG.info("Fetched blob on {}", output.getAbsolutePath());
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "manifest-push", description = "Push a manifest")
    public static class PushManifest implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(PushManifest.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--file" }, required = true)
        private Path file;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Pushing manifest...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                String location = registry.pushManifest(containerRef, Manifest.fromJson(Files.readString(file)));
                LOG.info("Pushed manifest to " + location);
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "manifest-delete", description = "Push a manifest")
    public static class DeleteManifest implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(DeleteManifest.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Deleting manifest...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                registry.deleteManifest(containerRef);
                LOG.info("Deleted manifest");
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "manifest-fetch", description = "Fetch a manifest")
    public static class FetchManifest implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(FetchManifest.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--output" }, required = true)
        private File output;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Fetching manifest...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                Manifest manifest = registry.getManifest(containerRef);
                Files.writeString(output.toPath(), manifest.toJson());
                LOG.info("Fetched manifest");
                return 0;
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
        }
    }

    @CommandLine.Command(name = "push", description = "Push an artifact")
    public static class ArtifactPush implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(ArtifactPush.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--file" }, required = true)
        private Path file;

        @CommandLine.Option(names = {"--export-manifest"}, description = "path of the pushed manifest")
        private Path exportManifestPath;

        @CommandLine.Option(names = {"--artifact-type"}, description = "type of the pushed artifact")
        private String artifactType;

        @CommandLine.Option(names = {"--annotation-file"}, description = "path of the annotation file")
        private Path annotationFile;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Pushing artifact...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                Annotations annotations = Annotations.empty();
                if (annotationFile != null) {
                    annotations = Annotations.fromJson(Files.readString(annotationFile));
                }
                Manifest manifest = registry.pushArtifact(containerRef, artifactType, annotations, file);
                if (exportManifestPath != null) {
                    Files.writeString(exportManifestPath, manifest.toJson());
                    LOG.info("Exported manifest to {}", exportManifestPath);
                }
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "pull", description = "Pull an artifact")
    public static class ArtifactPull implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(ArtifactPull.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--output" }, required = false)
        private Path output = Path.of(".");

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Pull artifact...");
            ContainerRef containerRef = ContainerRef.parse(options.repository);
            Registry registry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(options.username != null && options.password != null ? new UsernamePasswordProvider(options.username, options.password) : new EnvironmentPasswordProvider())
                    .build();
            try {
                Files.createDirectories(output);
                registry.pullArtifact(containerRef, output);
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }

}
