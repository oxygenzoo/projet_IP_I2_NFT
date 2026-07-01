package com.nft.backend.service;

import com.nft.backend.dto.HealthResponse;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthResponse getHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return new HealthResponse("UP", "UP", "Backend et base disponibles");
            }
            return new HealthResponse("DOWN", "DOWN", "Connexion base invalide");
        } catch (Exception exception) {
            return new HealthResponse("DOWN", "DOWN", "Base de donnees indisponible");
        }
    }
}
