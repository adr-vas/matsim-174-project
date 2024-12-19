package org.matsim.project;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Map;
import java.util.Random;

public class GeneratePopulation {
    public static void createPopulation(Scenario scenario, Map<String, DistrictData> districtDataMap) {
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();
        Random random = new Random();

        for (String tractID : districtDataMap.keySet()) {
            DistrictData districtData = districtDataMap.get(tractID);

            if (districtData == null || districtData.getGeometry() == null) {
                System.err.println("No geometry found for tract: " + tractID);
                continue;
            }

            Geometry geometry = districtData.getGeometry();
            long populationCount = districtData.getPopulation();

            for (int i = 0; i < populationCount; i++) {
                Person person = factory.createPerson(Id.createPersonId(tractID + "_" + i));
                Plan plan = factory.createPlan();

                // Generate a random home location within the tract
                Point homePoint = getRandomPointWithinGeometry(geometry, random);
                Activity home = factory.createActivityFromCoord("home", CoordUtils.createCoord(homePoint.getX(), homePoint.getY()));
                home.setEndTime(8 * 3600); // Leave home at 8:00 AM
                plan.addActivity(home);

                // Generate a random work location within the tract for simplicity
                Point workPoint = getRandomPointWithinGeometry(geometry, random);
                Activity work = factory.createActivityFromCoord("work", CoordUtils.createCoord(workPoint.getX(), workPoint.getY()));
                plan.addActivity(work);

                person.addPlan(plan);
                population.addPerson(person);
            }
        }
    }

    private static Point getRandomPointWithinGeometry(Geometry geometry, Random random) {
        while (true) {
            double x = geometry.getEnvelopeInternal().getMinX() + random.nextDouble() * geometry.getEnvelopeInternal().getWidth();
            double y = geometry.getEnvelopeInternal().getMinY() + random.nextDouble() * geometry.getEnvelopeInternal().getHeight();
            Point point = geometry.getFactory().createPoint(new org.locationtech.jts.geom.Coordinate(x, y));
            if (geometry.contains(point)) return point;
        }
    }
}