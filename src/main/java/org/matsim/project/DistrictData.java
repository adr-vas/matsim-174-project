package org.matsim.project;

import org.locationtech.jts.geom.Geometry;

public class DistrictData {
    private final String tractId;
    private Long population;
    private final Geometry geometry;
    private final String neighborhood;

    // Constructor
    public DistrictData(String tractId, Long population, Geometry geometry, String neighborhood) {
        this.tractId = tractId;
        this.population = population;
        this.geometry = geometry;
        this.neighborhood = neighborhood;
    }

    // Getters
    public String getTractId() {
        return tractId;
    }

    public Long getPopulation() {
        return population;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    // Setters
    public void setPopulation(Long population) {
        this.population = population;
    }
}