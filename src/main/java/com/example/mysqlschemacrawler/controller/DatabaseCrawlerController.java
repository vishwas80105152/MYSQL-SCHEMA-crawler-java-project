package com.example.mysqlschemacrawler.controller;

import com.example.mysqlschemacrawler.dto.ConnectionConfigDto;
import com.example.mysqlschemacrawler.dto.DatabaseMetadataResponse;
import com.example.mysqlschemacrawler.dto.GeneratedModelResponse;
import com.example.mysqlschemacrawler.service.DatabaseCrawlerService;
import com.example.mysqlschemacrawler.service.ModelGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class DatabaseCrawlerController {

    private final DatabaseCrawlerService crawlerService;
    private final ModelGeneratorService modelGeneratorService;

    @PostMapping("/connect")
    public ResponseEntity<String> testConnection(@RequestBody ConnectionConfigDto config) {
        boolean connected = crawlerService.testConnection(
                config.getUrl(), 
                config.getUsername(), 
                config.getPassword()
        );
        
        if (connected) {
            return ResponseEntity.ok("Connection successful");
        } else {
            return ResponseEntity.badRequest().body("Connection failed");
        }
    }

    @PostMapping("/metadata")
    public ResponseEntity<DatabaseMetadataResponse> getDatabaseMetadata(@RequestBody ConnectionConfigDto config) {
        DatabaseMetadataResponse metadata = crawlerService.extractDatabaseMetadata(
                config.getUrl(),
                config.getUsername(),
                config.getPassword(),
                config.getSchema()
        );
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/generate-models")
    public ResponseEntity<GeneratedModelResponse> generateModels(@RequestBody ConnectionConfigDto config) {
        DatabaseMetadataResponse metadata = crawlerService.extractDatabaseMetadata(
                config.getUrl(),
                config.getUsername(),
                config.getPassword(),
                config.getSchema()
        );
        
        Map<String, String> generatedClasses = modelGeneratorService.generateModelClasses(
                metadata, 
                config.getModelPackage()
        );
        
        GeneratedModelResponse response = new GeneratedModelResponse();
        response.setGeneratedClasses(generatedClasses);
        response.setTotalClasses(generatedClasses.size());
        
        return ResponseEntity.ok(response);
    }
}
