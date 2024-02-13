package com.skemu.rdf.rdftemplate.config;

import com.skemu.rdf.rdftemplate.dataresolver.DataResolverException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigResourceLoaders {

    @SneakyThrows
    public static Optional<Resource> getResource(@NonNull String resourceLocation) {

        var relativeUri = resolve(ConfigProperties.getRelativeFileConfigPath(), resourceLocation);
        if (uriExists(relativeUri)) {
            return Optional.of(new UrlResource(relativeUri));
        }

        var uri = resolve(ConfigProperties.getFileConfigPath(), resourceLocation);
        if (uriExists(uri)) {
            return Optional.of(new UrlResource(uri));
        }

        var resource = new ClassPathResource(ConfigProperties.getConfigPath() + resourceLocation);
        if (resource.exists()) {
            return Optional.of(resource);
        }

        return Optional.empty();
    }

    private static URI resolve(@NonNull URI basePath, String resourceLocation) {
        if (resourceLocation == null) {
            return basePath;
        }
        return basePath.resolve(resourceLocation);
    }

    static boolean uriExists(URI uri) {
        return Files.exists(Paths.get(uri));
    }

    public static String resolveResourceUriString(Resource resource) {
        return resolveResourceUriStrings(resource, false).getFirst();
    }

    public static List<String> resolveResourceUriStringsDeeply(Resource resource) {
        return resolveResourceUriStrings(resource, true);
    }

    private static List<String> resolveResourceUriStrings(Resource resource, boolean deepResolve) {
        try {
            if (deepResolve && resource.isFile()) {
                var fileResource = resource.getFile();

                try (var traversal = Files.walk(fileResource.toPath())) {
                    return traversal
                            .filter(Files::isRegularFile)
                            .map(Path::toUri)
                            .map(URI::toString)
                            .toList();
                }
            }

            return List.of(resource.getURI().toString());
        } catch (IOException ioException) {
            throw new DataResolverException(
                    String.format("Exception getting URI for resource %s", resource), ioException);
        }
    }
}
