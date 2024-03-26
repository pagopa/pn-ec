package it.pagopa.pn.library.pec.configuration;

import com.namirial.pec.library.service.PnPecServiceImpl;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Configuration
@CustomLog
public class PnPecProvidersConf {

    @SneakyThrows(IOException.class)
    private Set<String> getNamirialPropertiesKeySet() {
        try (FileInputStream fileInputStream = new FileInputStream("src/main/resources/namirial/namirial.properties")) {
            Properties prop = new Properties();
            prop.load(fileInputStream);
            return prop.stringPropertyNames();
        }
    }

    @Bean
    public PnPecServiceImpl namirialService(@Autowired Environment env) {
        getNamirialPropertiesKeySet().forEach(key -> {
            String property = env.getRequiredProperty(key);
            System.setProperty(key, property);
        });
        return new PnPecServiceImpl();
    }

}
