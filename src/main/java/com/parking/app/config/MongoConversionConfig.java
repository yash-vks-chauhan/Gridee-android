package com.parking.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Mongo converters to stay compatible with Atlas documents
 * where certain numeric fields (like available) were stored as booleans.
 */
@Configuration
public class MongoConversionConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        converters.add(new Converter<Boolean, Integer>() {
            @Override
            public Integer convert(Boolean source) {
                if (source == null) {
                    return 0;
                }
                return source ? 1 : 0;
            }
        });

        converters.add(new Converter<Boolean, Double>() {
            @Override
            public Double convert(Boolean source) {
                if (source == null) {
                    return 0.0;
                }
                return source ? 1.0 : 0.0;
            }
        });

        return new MongoCustomConversions(converters);
    }
}
