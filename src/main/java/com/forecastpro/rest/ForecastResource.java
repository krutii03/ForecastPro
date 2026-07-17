package com.forecastpro.rest;

import com.forecastpro.dto.ForecastApiDto;
import com.forecastpro.entity.Forecast;
import com.forecastpro.repository.ForecastRepository;
import com.forecastpro.util.DisplayFormats;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Path("/forecasts")
public class ForecastResource {

    @EJB
    private ForecastRepository forecastRepository;

    @GET
    @Path("/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ForecastApiDto> recent() {
        return forecastRepository.findRecent(20).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ForecastApiDto toDto(Forecast f) {
        ForecastApiDto d = new ForecastApiDto();
        d.setId(f.getId());
        d.setProductId(f.getProduct().getId());
        d.setProductName(f.getProduct().getName());
        d.setMovingAvgValue(f.getMovingAvgValue());
        d.setMlRegressionValue(f.getMlRegressionValue());
        d.setPredictedSales(f.getPredictedSales());
        d.setPredictedRevenue(f.getPredictedRevenue());
        d.setForecastMonth(DisplayFormats.formatDate(f.getForecastMonth()));
        d.setCreatedAt(f.getCreatedAt());
        d.setCreatedAtDisplay(DisplayFormats.formatInstantDate(f.getCreatedAt(), ZoneId.systemDefault()));
        return d;
    }
}
