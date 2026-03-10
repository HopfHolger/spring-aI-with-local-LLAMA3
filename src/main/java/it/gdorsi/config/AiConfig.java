package it.gdorsi.config;

import java.time.Duration;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import it.gdorsi.service.tool.VertragTool;
import it.gdorsi.service.tool.XmlTool;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder builder, List<ToolCallback> contractToolCallbacks) {
        // Hier wird der moderne Client mit der .prompt() API erzeugt
        return builder
                .defaultSystem("Du bist ein Experte für Cloud-Architektur.")
                .defaultToolCallbacks(contractToolCallbacks)
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        // Nutzt den Standard Java 11+ HttpClient
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60)); // Großzügig für Ollama

        return RestClient.builder()
                .requestFactory(factory);
    }

    @Bean
    public List<ToolCallback> contractToolCallbacks(VertragTool vertragTools, XmlTool xmlTool) {
        // Erzeugt sauber die Callbacks aus deinem Interface/Component
        ToolCallback[] vertragCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(vertragTools)
                .build()
                .getToolCallbacks();
        
        ToolCallback[] xmlCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(xmlTool)
                .build()
                .getToolCallbacks();
        
        // Combine both arrays into a single list
        List<ToolCallback> allCallbacks = new java.util.ArrayList<>();
        allCallbacks.addAll(java.util.Arrays.asList(vertragCallbacks));
        allCallbacks.addAll(java.util.Arrays.asList(xmlCallbacks));
        
        return allCallbacks;
    }

}
