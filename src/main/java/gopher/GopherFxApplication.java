package gopher;

/*-
 * #%L
 * PhenoteFX
 * %%
 * Copyright (C) 2017 - 2022 Peter Robinson
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;


public class GopherFxApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(GopherFxApplication.class);
    private ConfigurableApplicationContext applicationContext;


    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(StockUiApplication.class).run();
    }
    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting application");
        applicationContext.publishEvent(new StageReadyEvent(stage));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        LOGGER.info("Stopping application");
        final Properties pgProperties = applicationContext.getBean("pgProperties", Properties.class);
        final Path configFilePath = applicationContext.getBean("configFilePath", Path.class);
        try (OutputStream os = Files.newOutputStream(configFilePath)) {
            pgProperties.store(os, "GOPHER properties");
        }
        Platform.exit();
        applicationContext.close();
    }


    static class StageReadyEvent extends ApplicationEvent {
        public StageReadyEvent(Stage stage) {
            super(stage);
        }
        public Stage getStage() {
            return ((Stage) getSource());
        }
    }



}
