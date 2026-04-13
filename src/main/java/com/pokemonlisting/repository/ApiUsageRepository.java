package com.pokemonlisting.repository;

import com.pokemonlisting.model.ApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pokemonlisting.dto.ServiceUsageSummary;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiUsageRepository extends JpaRepository<ApiUsage, Long> {

    @Query("SELECT a.service AS service, COUNT(a) AS callCount, SUM(a.cost) AS totalCost " +
           "FROM ApiUsage a WHERE (:service IS NULL OR a.service = :service) " +
           "AND a.createdAt BETWEEN :start AND :end GROUP BY a.service")
    List<ServiceUsageSummary> summarizeByDateRange(@Param("service") String service,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
