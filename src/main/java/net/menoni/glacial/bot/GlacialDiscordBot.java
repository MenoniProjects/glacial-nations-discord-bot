package net.menoni.glacial.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = {
        "net.menoni.glacial.bot",
        "net.menoni.spring.commons"
})
@PropertySource(value = {
        "classpath:commons-application.properties"
})
public class GlacialDiscordBot {
    public static void main( String[] args ) {
        SpringApplication.run(GlacialDiscordBot.class, args);
    }
}
