package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RunMatsim {
    public static void main(String[] args) throws Exception {
        // Load MATSim config
        Config config = ConfigUtils.loadConfig("scenarios/equil/config.xml");

        // Set up unique output directory
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String uniqueOutputDir = "scenarios/equil/output/run_" + timestamp;
        config.controller().setOutputDirectory(uniqueOutputDir);
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

        // Verify population file exists
        String populationFilePath = "scenarios/equil/output/synthetic_population.xml";
        File populationFile = new File(populationFilePath);
        if (!populationFile.exists()) {
            System.out.println("Synthetic population file not found. Please generate it before running.");
            return;
        }

        // Load scenario (loads network and population from config.xml)
        Scenario scenario = ScenarioUtils.loadScenario(config);

        System.out.println("Loaded network nodes: " + scenario.getNetwork().getNodes().size());
        System.out.println("Loaded network links: " + scenario.getNetwork().getLinks().size());
        System.out.println("Loaded population: " + scenario.getPopulation().getPersons().size());

        // Run the simulation
        Controler controller = new Controler(scenario);

    // Run the simulation
        controller.run();
    }
}