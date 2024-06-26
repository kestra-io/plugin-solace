package io.kestra.plugin.solace.data;

import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Utility class for retrieving an {@link InputStream} from objects.
 */
public final class InputStreamProvider {
    private final RunContext context;

    public InputStreamProvider(final RunContext context) {
        this.context = context;
    }

    public InputStream get(final String path) {
        try {
            URI from = new URI(context.render(path));
            return context.storage().getFile(from);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream get(final List<Object> objects) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (os) {
            for (Object o : objects) {
                FileSerde.write(os, o);
            }
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    public InputStream get(final Map<String, Object> object) throws IOException {
        return get(List.of(object));
    }
}
