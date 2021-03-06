package net.lelyak.service.impl;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.lelyak.model.impl.FlightDataDTO;
import net.lelyak.service.Reader;
import net.lelyak.utills.Clock;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nazar Lelyak.
 */
@Slf4j
@Service
public class CsvFileReader implements Reader {

    @Value("${flight.data.file}")
    private String file;


    @Override
    public String getFilePath() {
        return file;
    }

    @Override
    public List<FlightDataDTO> parseFile() {
        List<FlightDataDTO> flightData = Lists.newArrayList();
        String csvFile = getFilePath();

        try (CSVReader reader = new CSVReader(new BufferedReader(new FileReader(csvFile)))) {
            String[] line;

            reader.readNext(); // skip the first line - with column names

            while ((line = reader.readNext()) != null) {

                LocalTime departureTime = parseTime(line[0]);
                String ifDeparted = departureTime.isBefore(Clock.getCurrentTime()) ? "DEPARTED" : "";

                FlightDataDTO dto = FlightDataDTO.builder()
                        .departureTime(departureTime)
                        .destination(line[1])
                        .destinationAirport(line[2])
                        .flightNumber(line[3])
                        .days(parseDays(line[4], line[5], line[6], line[7], line[8], line[9], line[10]))
                        .isDeparted(ifDeparted)
                        .build();

                flightData.add(dto);
            }
        } catch (IOException e) {
            log.error("Error during parsing CSV file: {}", e.getMessage());
        }

        // display all flights for the current day and time, in chronological order.
        // Flights that are in the past should be displayed as "Departed"
        return sortTodayFlights(flightData);
    }


    private List<FlightDataDTO> sortTodayFlights(List<FlightDataDTO> flightData) {
        return flightData.stream()
                .filter(d -> d.getDays().contains(Clock.getCurrentDay()))
                .sorted(Comparator.comparing(FlightDataDTO::getDepartureTime))
                .collect(Collectors.toList());
    }

    private LocalTime parseTime(String strTime) {
        String[] arr = strTime.split(":");

        if (arr.length == 2) {
            return LocalTime.of(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
        } else {
            throw new RuntimeException("DEPARTURE_TIME is incorrect " + strTime);
        }
    }

    private Set<DayOfWeek> parseDays(String sunday, String monday,
                                     String tuesday, String wednesday,
                                     String thursday, String friday,
                                     String saturday) {
        Set<DayOfWeek> result = Sets.newHashSet();

        if (StringUtils.isNotEmpty(sunday)) result.add(DayOfWeek.SUNDAY);
        if (StringUtils.isNotEmpty(monday)) result.add(DayOfWeek.MONDAY);
        if (StringUtils.isNotEmpty(tuesday)) result.add(DayOfWeek.TUESDAY);
        if (StringUtils.isNotEmpty(wednesday)) result.add(DayOfWeek.WEDNESDAY);
        if (StringUtils.isNotEmpty(thursday)) result.add(DayOfWeek.THURSDAY);
        if (StringUtils.isNotEmpty(friday)) result.add(DayOfWeek.FRIDAY);
        if (StringUtils.isNotEmpty(saturday)) result.add(DayOfWeek.SATURDAY);

        return result;
    }
}
