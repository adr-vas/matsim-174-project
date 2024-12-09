package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class RunMatsim {
    public static void main(String[] args) throws Exception {
        // Load MATSim config
        Config config = ConfigUtils.loadConfig("scenarios/equil/config.xml");

        // Set up unique output directory
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String uniqueOutputDir = "scenarios/equil/output/run_" + timestamp;
        config.controller().setOutputDirectory(uniqueOutputDir);
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

        String shapefilePath = "/Users/adrian/IdeaProjects/matsim-example-project/original-input-data/neighborhoods_and_tracts.shp";
        String csvFilePath = "/Users/adrian/IdeaProjects/matsim-example-project/original-input-data/pop_by_tract.csv";

        String populationFilePath = "/Users/adrian/IdeaProjects/matsim-example-project/scenarios/equil/output/synthetic_population.xml";
        File populationFile = new File(populationFilePath);
        if (!populationFile.exists()) {
            System.out.println("Synthetic population file not found. Generating new synthetic population...");

            // create temp scenario
            Scenario scenario = ScenarioUtils.createScenario(config);

            // Debug network
            System.out.println("Network file: " + config.network().getInputFile());


            // Process population data and generate the synthetic population
            Map<String, DistrictData> districtDataMap = ShapefileProcessor.loadDistrictData(shapefilePath);
            PopulationDataProcessor.processPopulationData(csvFilePath, districtDataMap);
            GeneratePopulation.createPopulation(scenario, districtDataMap);

            // Write synthetic population to file

            new PopulationWriter(scenario.getPopulation()).write(populationFilePath);
            System.out.println("Synthetic population written to: " + populationFilePath);
        } else {
            System.out.println("Synthetic population file found: " + populationFilePath);
        }
        // Update config with new plans file
        // config.plans().setInputFile(populationFilePath);

        // Reload scenario with updated plans file
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Verify scenario has population loaded
        System.out.println("Scenario now has population: " + scenario.getPopulation().getPersons().size());
        System.out.println("Loaded network nodes: " + scenario.getNetwork().getNodes().size());
        System.out.println("Loaded network links: " + scenario.getNetwork().getLinks().size());
        // Run the simulation
        Controler controler = new Controler(scenario);
        controler.run();
    }
}