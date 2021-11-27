package gopher.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {

    private final String applicationUiTitle;



    @Autowired
    public ApplicationProperties(@Value("${spring.application.ui.title}") String uiTitle) {
        this.applicationUiTitle = uiTitle;

    }

    public String getApplicationUiTitle() {
        return applicationUiTitle;
    }


}
