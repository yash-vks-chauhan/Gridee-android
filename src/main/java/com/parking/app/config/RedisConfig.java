package com.parking.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for Distributed Locking
 * ACTIVE when: app.locking.provider=redis (default)
 * INACTIVE when: app.locking.provider=mongodb
 *
 * Enables coordination across multiple application instances with high performance
 */
@Configuration
@ConditionalOnProperty(name = "app.locking.provider", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Use Lettuce for better connection pooling and non-blocking operations
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setHostName("localhost");
        factory.setPort(6379);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
