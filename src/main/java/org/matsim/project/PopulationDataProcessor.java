package org.matsim.project;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withHeader("tract_name", "population")
                    .withFirstRecordAsHeader()
                    .parse(reader);

            // Parse and merge the data
            for (CSVRecord record : records) {
                String tractId = record.get("tract_name");
                int population = Integer.parseInt(record.get("population"));

                DistrictData districtData = districtDataMap.get(tractId);
                if (districtData != null) {
                    districtData.setPopulation((long) population);
                } else {
                    System.err.println("Warning: Tract ID " + tractId + " not found in district data.");
                }
            }
        }
    }
}