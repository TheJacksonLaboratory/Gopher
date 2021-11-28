package gopher.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

@Component
public class ApplicationProperties {

    private final String applicationUiTitle;

    private final String applicationVersion;

    @Autowired
    public ApplicationProperties(@Value("${application.title}") String uiTitle,
                                 @Value("${application.version") String version) {
        this.applicationUiTitle = uiTitle;
        this.applicationVersion = version;
    }



    public String getApplicationUiTitle() {
        return applicationUiTitle;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }
}
