package org.matsim.project;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GeneratePopulationStandalone {

    private Map<String, Coord> zoneGeometries = new HashMap<>();
    private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32610");
    private Random random = new Random();
    private Scenario scenario;
    private Population population;

    public static void main(String[] args) throws Exception {
        // File paths
        String configFilePath = "scenarios/equil/config.xml";
        String networkFilePath = "scenarios/equil/output/sf_network.xml.gz";
        String shapefilePath = "original-input-data/neighborhoods_and_tracts.shp";
        String csvFilePath = "original-input-data/pop_by_tract.csv";
        String outputPopulationFilePath = "scenarios/equil/output/synthetic_population.xml";

        // Load configuration and create scenario
        Config config = ConfigUtils.loadConfig(configFilePath);
        Scenario scenario = ScenarioUtils.createScenario(config);

        // Load network into the scenario
        Network network = scenario.getNetwork();
        NetworkUtils.readNetwork(network, networkFilePath);

        System.out.println("Loaded network nodes: " + network.getNodes().size());
        System.out.println("Loaded network links: " + network.getLinks().size());

        // Load district data
        Map<String, DistrictData> districtDataMap = ShapefileProcessor.loadDistrictData(shapefilePath);
        PopulationDataProcessor.processPopulationData(csvFilePath, districtDataMap);

        // Generate synthetic population
        GeneratePopulationStandalone generator = new GeneratePopulationStandalone();
        generator.init(scenario, network, districtDataMap);
        generator.generatePopulation(districtDataMap);

        // Write population to file
        new PopulationWriter(scenario.getPopulation()).write(outputPopulationFilePath);
        System.out.println("Synthetic population written to: " + outputPopulationFilePath);
    }

    public void init(Scenario scenario, Network network, Map<String, DistrictData> districtDataMap) {
        this.scenario = scenario;
        this.population = scenario.getPopulation();

        // Fill zone geometries from district data
        for (Map.Entry<String, DistrictData> entry : districtDataMap.entrySet()) {
            Geometry geometry = entry.getValue().getGeometry();
            if (geometry != null) {
                Coord center = CoordUtils.createCoord(geometry.getCentroid().getX(), geometry.getCentroid().getY());
                zoneGeometries.put(entry.getKey(), center);
                for (String key : zoneGeometries.keySet()) {
                    System.out.println("Zone key: " + key);
                }
            }
        }
    }
    public void generatePopulation(Map<String, DistrictData> districtDataMap) {
        // Get all tract IDs
        List<String> tractIds = new ArrayList<>(districtDataMap.keySet());
         
        System.out.println("Before clearing: " + population.getPersons().size());
        population.getPersons().clear();    
        System.out.println("After clearing: " + population.getPersons().size());

        // Loop through each tract to generate trips
        for (String homeTract : tractIds) {
            DistrictData homeData = districtDataMap.get(homeTract);
            if (homeData == null || homeData.getPopulation() == null || homeData.getPopulation() <= 0) {
                continue; // Skip tracts with no population
            }
    
            long scaledPopulation = Math.round(homeData.getPopulation() * 0.01);
    
            // Randomly assign work locations
            for (int i = 0; i < scaledPopulation; i++) {
                String workTract = getRandomWorkTract(homeTract, tractIds); // Ensure work tract is different from home tract
                generateHomeWorkHomeTrips(homeTract, workTract, 1);
            }

        }
    }
        
    private String getRandomWorkTract(String homeTract, List<String> tractIds) {
        Random random = new Random();
        String workTract;
        do {
            workTract = tractIds.get(random.nextInt(tractIds.size()));
        } while (workTract.equals(homeTract)); // Ensure work tract is not the same as home tract
        return workTract;
    }

    private void generateHomeWorkHomeTrips(String from, String to, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            Coord source = zoneGeometries.get(from);
            Coord sink = zoneGeometries.get(to);
             
            if (source == null || sink == null) {
                throw new IllegalArgumentException("Source or sink zone is missing from zoneGeometries: " + from + ", " + to);
            }

            Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.car));
            Plan plan = population.getFactory().createPlan();

            Coord homeLocation = blurCoordinates(ct.transform(source));
            Coord workLocation = blurCoordinates(ct.transform(sink));

            plan.addActivity(createHome(homeLocation));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createWork(workLocation));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createHome(homeLocation));

            person.addPlan(plan);
            population.addPerson(person);
        }
    }

    private Leg createDriveLeg() {
        return population.getFactory().createLeg(TransportMode.car);
    }

    private Coord blurCoordinates(Coord coord) {
        double xOffset = random.nextDouble() * 100 - 50; // +/- 50 meters
        double yOffset = random.nextDouble() * 100 - 50; // +/- 50 meters
        return CoordUtils.createCoord(coord.getX() + xOffset, coord.getY() + yOffset);
    }

    private Activity createHome(Coord homeLocation) {
        Activity home = population.getFactory().createActivityFromCoord("home", homeLocation);
        home.setEndTime(8 * 3600); // 8:00 AM
        return home;
    }

    private Activity createWork(Coord workLocation) {
        Activity work = population.getFactory().createActivityFromCoord("work", workLocation);
        work.setEndTime(17 * 3600); // 5:00 PM
        return work;
    } 
    private static int globalPersonCounter = 0;

    private Id<Person> createId(String source, String sink, int i, String transportMode) {
        return Id.create(transportMode + "_" + source + "_" + sink + "_" + globalPersonCounter++, Person.class);
    }
}