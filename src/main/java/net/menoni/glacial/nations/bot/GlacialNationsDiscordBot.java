package net.menoni.glacial.nations.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = {
        "net.menoni.ws.discord",
        "net.menoni.glacial.nations.bot",
        "net.menoni.spring.commons"
})
@PropertySource(value = {
        "classpath:commons-application.properties"
})
@ComponentScan(basePackages = {
        "net.menoni.ws.discord",
        "net.menoni.glacial.nations"
})
public class GlacialNationsDiscordBot {
    public static void main( String[] args ) {
        SpringApplication.run(GlacialNationsDiscordBot.class, args);
    }
}
