package it.gdorsi.dao;

import java.time.LocalDate;

public record DocumentOverview(
        String fileName,
        long chunkCount,
        LocalDate lastUpdated
) {}
