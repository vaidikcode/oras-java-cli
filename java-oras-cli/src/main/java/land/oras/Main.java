package land.oras;

import land.oras.auth.AuthProvider;
import land.oras.auth.FileStoreAuthenticationProvider;
import land.oras.auth.UsernamePasswordProvider;
import land.oras.exception.OrasException;
import org.apache.commons.lang3.tuple.Pair;
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

                // Artifacts
                Main.ArtifactPush.class,
                Main.ArtifactPull.class,

                Main.ArtifactCopy.class,
                Main.CopyOciLayout.class
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

    /**
     * Reusable options
     */
    @CommandLine.Command(synopsisHeading = "%nUsage:%n",
            descriptionHeading = "%nDescription:%n",
            parameterListHeading = "%nParameters:%n",
            optionListHeading = "%nOptions:%n",
            commandListHeading = "%nCommands:%n")
    public static class CopyOptions {

        // Define a positional parameter for the repository name
        @CommandLine.Parameters(index = "0", description = "The repository to copy from")
        private String sourceRepository;

        // Define a positional parameter for the repository name
        @CommandLine.Parameters(index = "1", description = "The repository to copy to")
        private String targetRepository;

        @CommandLine.Option(names = { "--source-username" }, description = {
                "Username"})
        private String sourceUsername;

        @CommandLine.Option(names = { "--source-password" }, description = {
                "Username"})
        private String sourcePassword;

        @CommandLine.Option(names = { "--target-username" }, description = {
                "Username"})
        private String targetUsername;

        @CommandLine.Option(names = { "--target-password" }, description = {
                "Username"})
        private String targetPassword;

        @CommandLine.Option(names = { "--debug" }, description = {
                "Enable debug mode"})
        private Boolean debug = false;

        @CommandLine.Option(names = { "--source-insecure" }, description = {
                "Allow insecure connections over HTTP"})
        private Boolean sourceInsecure = false;

        @CommandLine.Option(names = { "--source-skip-tls-verify" }, description = {
                "Skip TLS verification"})
        private Boolean sourceSkipTlsVerify = false;

        @CommandLine.Option(names = { "--target-insecure" }, description = {
                "Allow insecure connections over HTTP"})
        private Boolean targetInsecure = false;

        @CommandLine.Option(names = { "--target-skip-tls-verify" }, description = {
                "Skip TLS verification"})
        private Boolean targetSkipTlsVerify = false;

    }

    /**
     * Get the auth provider
     * @param options The options
     * @return The auth provider
     */
    private static AuthProvider getAuthProvider(ReusableOptions options) {
        if (options.username != null && options.password != null) {
            return new UsernamePasswordProvider(options.username, options.password);
        }
        return new FileStoreAuthenticationProvider();
    }

    /**
     * Get the auth provider for copy
     * @param copyOptions The copy options
     * @return The auth provider
     */
    private static Pair<AuthProvider, AuthProvider> getAuthProvider(CopyOptions copyOptions) {
        AuthProvider sourceAuthProvider = null;
        AuthProvider targetAuthProvider = null;
        if (copyOptions.sourceUsername != null && copyOptions.sourcePassword != null) {
            sourceAuthProvider = new UsernamePasswordProvider(copyOptions.sourceUsername, copyOptions.sourcePassword);
        }
        else {
            sourceAuthProvider = new FileStoreAuthenticationProvider();
        }
        if (copyOptions.targetUsername != null && copyOptions.targetPassword != null) {
            targetAuthProvider = new UsernamePasswordProvider(copyOptions.targetUsername, copyOptions.targetPassword);
        }
        else {
            targetAuthProvider = new FileStoreAuthenticationProvider();
        }
        return Pair.of(sourceAuthProvider, targetAuthProvider);
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
                    .withAuthProvider(getAuthProvider(options)).build();
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
                    .withAuthProvider(getAuthProvider(options)).build();
            try {
                Layer layer = registry.pushBlob(containerRef, file);
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
                    .withAuthProvider(getAuthProvider(options)).build();
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
                    .withAuthProvider(getAuthProvider(options)).build();
            try {
                registry.pushManifest(containerRef, Manifest.fromJson(Files.readString(file)));
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
                    .withAuthProvider(getAuthProvider(options)).build();
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
                    .withAuthProvider(getAuthProvider(options)).build();
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

    @CommandLine.Command(name = "copy", description = "Copy an artifact")
    public static class ArtifactCopy implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(ArtifactCopy.class);

        @CommandLine.Mixin
        private CopyOptions options;

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            LOG.info("Copy artifact...");
            ContainerRef sourceContainer = ContainerRef.parse(options.sourceRepository);
            Registry sourceRegistry = Registry.Builder.builder()
                    .withInsecure(options.sourceInsecure)
                    .withSkipTlsVerify(options.sourceSkipTlsVerify)
                    .withAuthProvider(getAuthProvider(options).getKey()).build();

            ContainerRef targetContainer = ContainerRef.parse(options.targetRepository);
            Registry targetRegistry = Registry.Builder.builder()
                    .withInsecure(options.targetInsecure)
                    .withSkipTlsVerify(options.targetSkipTlsVerify)
                    .withAuthProvider(getAuthProvider(options).getRight()).build();

            try {
                sourceRegistry.copy(targetRegistry, sourceContainer, targetContainer);
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }

    }

    @CommandLine.Command(name = "copy-oci", description = "Copy an artifact into OCI layout")
    public static class CopyOciLayout implements Callable<Integer> {
        private static final Logger LOG = LoggerFactory.getLogger(CopyOciLayout.class);

        @CommandLine.Mixin
        private ReusableOptions options;

        @CommandLine.Option(names = { "--output" }, required = false)
        private Path output = Path.of(".");

        @Override
        public Integer call() throws Exception {
            if (options.debug) {
                Main.DEBUG = true;
            }
            Files.createDirectory(output);
            LOG.info("Copy artifact to OCI layout on %s".formatted(output.toAbsolutePath()));
            ContainerRef container = ContainerRef.parse(options.repository);
            Registry sourceRegistry = Registry.Builder.builder()
                    .withInsecure(options.insecure)
                    .withSkipTlsVerify(options.skipTlsVerify)
                    .withAuthProvider(getAuthProvider(options)).build();


            try {
                sourceRegistry.copy(container, output);
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
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
                    .withAuthProvider(getAuthProvider(options)).build();
            try {
                Annotations annotations = Annotations.empty();
                if (annotationFile != null) {
                    annotations = Annotations.fromJson(Files.readString(annotationFile));
                }
                Manifest manifest = registry.pushArtifact(containerRef, ArtifactType.from(artifactType), annotations, LocalPath.of(file));
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

        @CommandLine.Option(names = { "--keep-old-files" }, required = false)
        private boolean keepOldFiles = false;

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
                    .withAuthProvider(getAuthProvider(options)).build();
            try {
                Files.createDirectories(output);
                registry.pullArtifact(containerRef, output, !keepOldFiles);
            }
            catch (OrasException e) {
                handleException(e);
                return 1;
            }
            return 0;
        }
    }
    
}
