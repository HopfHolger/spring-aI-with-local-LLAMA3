package it.gdorsi.dao;

public record DocumentOverview(
        String fileName,
        long chunkCount,
        String lastUpdated
) {}
