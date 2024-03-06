package com.smarttrader.price.history.service.stresstest.config;

import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "instruments")
@PropertySource(value = "classpath:instruments.yaml", factory = YamlPropertySourceFactory.class)
@Setter
public class InstrumentConfig {

    private Boolean all;

    private List<InstrumentConfigItem> list;

    public List<String> getInstruments(boolean enabledOnly) {
        List<String> enabledInstruments;

        if (all || !enabledOnly) {
            enabledInstruments = list.stream()
                    .map(instrumentConfigItem -> instrumentConfigItem.name)
                    .toList();
        } else {
            enabledInstruments = list.stream()
                    .filter(instrumentConfigItem -> instrumentConfigItem.enabled)
                    .map(instrumentConfigItem -> instrumentConfigItem.name)
                    .toList();
        }

        return enabledInstruments;
    }

    @Data
    public static final class InstrumentConfigItem {
        private String name;
        private Boolean enabled;
    }
}
