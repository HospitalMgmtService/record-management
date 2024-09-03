package com.pnk.record_management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;


@Slf4j
@Component
public class DateTimeFormatterImpl implements DateTimeFormatter {

    Map<Long, Function<Instant, String>> stategyMap = new LinkedHashMap<>();

    public DateTimeFormatterImpl() {
        stategyMap.put(60L, this::formatInSeconds);
        stategyMap.put(3600L, this::formatInMinutes);
        stategyMap.put(86400L, this::formatInHours);
        stategyMap.put(Long.MAX_VALUE, this::formatInDays);
    }


    @Override
    public String format(Instant instant) {
        long elapsedSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());

        var strategy = stategyMap.entrySet()
                .stream()
                .filter(longFunctionEntry -> elapsedSeconds < longFunctionEntry.getKey())
                .findFirst()
                .get();

        return strategy.getValue().apply(instant);
    }


    private String formatInSeconds(Instant instant) {
        long elapsedSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());

        return elapsedSeconds + " seconds";
    }


    private String formatInMinutes(Instant instant) {
        long elapsedMinutes = ChronoUnit.MINUTES.between(instant, Instant.now());

        return elapsedMinutes + " minutes";
    }


    private String formatInHours(Instant instant) {
        long elapsedHours = ChronoUnit.HOURS.between(instant, Instant.now());

        return elapsedHours + " hours";
    }


    private String formatInDays(Instant instant) {
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        java.time.format.DateTimeFormatter dateTimeFormatter = java.time.format.DateTimeFormatter.ISO_DATE;

        return localDateTime.format(dateTimeFormatter);
    }
}
