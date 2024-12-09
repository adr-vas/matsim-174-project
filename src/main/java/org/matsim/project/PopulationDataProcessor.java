package org.matsim.project;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;

import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

public class PopulationDataProcessor {

    /**
     * Loads population data from a CSV file and merges it into the district data map.
     *
     * @param csvFilePath Path to the CSV file containing population data.
     * @param districtDataMap The map of district data to update with population values.
     * @throws Exception if there's an error reading the CSV file.
     */
    public static void processPopulationData(String csvFilePath, Map<String, DistrictData> districtDataMap) throws Exception {
        // Open the CSV file
        try (Reader reader = new FileReader(csvFilePath)) {
            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader("tract_name", "population")
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            // Parse and merge the data
            for (CSVRecord record : parser) {
                String tractId = record.get("tract_name");
                try {
                    int population = Integer.parseInt(record.get("population").trim());
                    DistrictData districtData = districtDataMap.get(tractId);
                    if (districtData != null) {
                        districtData.setPopulation((long) population);
                    } else {
                        System.err.println("Warning: Tract ID " + tractId + " not found in district data.");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid population value for tract " + tractId + ": " + record.get("population"));
                }
            }
        }
    }
}