package org.matsim.project;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OSM2MATSim {
    public static void main(String[] args) throws Exception {
        String inputPbf = "original-input-data/SanFrancisco.osm.pbf";
        String filterShape = "original-input-data/neighborhoods_and_tracts.shp";
        String outputNetwork = "scenarios/equil/output/sf_network.xml.gz";

        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, "EPSG:32610");

        List<PreparedGeometry> filterGeometries = ShpGeometryUtils.loadPreparedGeometries(Paths.get(filterShape).toUri().toURL());

        SupersonicOsmNetworkReader reader = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .setIncludeLinkAtCoordWithHierarchy((coord, hierarchyLevel) -> hierarchyLevel <= LinkProperties.LEVEL_RESIDENTIAL
                        || ShpGeometryUtils.isCoordInPreparedGeometries(coord, filterGeometries))
                .setAfterLinkCreated((link, osmTags, direction) -> {
                    Set<String> modes = new HashSet<>(link.getAllowedModes());
                    modes.add(TransportMode.car);
                    if (osmTags.containsKey(OsmTags.CYCLEWAY)) {
                        modes.add(TransportMode.bike);
                    }
                    link.setAllowedModes(modes);
                    
                    // Increase capacity and number of lanes
                    double originalCapacity = link.getCapacity();
                    double originalNumberOfLanes = link.getNumberOfLanes();

                    // Custom scaling factor for capacity and lanes
                    double scalingFactor = 2.5;

                    link.setCapacity(originalCapacity * scalingFactor);

                    double adjustedLanes = originalNumberOfLanes * scalingFactor;
                    if (adjustedLanes < 2) {
                        link.setNumberOfLanes(2); // Ensure minimum of 1 lane
                    } else {
                        link.setNumberOfLanes(Math.round(adjustedLanes)); // Round to nearest integer
                    }

                    // Optionally adjust the length

                    double originalLength = link.getLength();
                    if (originalLength < 100) {
                        link.setLength(originalLength * 25); // Increase length by 850%
                    } else {
                        link.setLength(originalLength * 8.5); // Increase length by 850%
                    }


                    // Calculate storage capacity
                    double storageCapacity = (link.getLength() * link.getNumberOfLanes()) / 7.5;

                    // Ensure that the capacity does not exceed storage capacity
                    double adjustedCapacity = Math.min(link.getCapacity(), storageCapacity);
                    link.setCapacity(adjustedCapacity);

                    System.out.println("Link ID: " + link.getId() + " | Length: " + link.getLength() +
                                    " | Storage Capacity: " + storageCapacity + " | Adjusted Capacity: " + adjustedCapacity);
                })
                .build();

        Network network = reader.read(inputPbf);
        new NetworkCleaner().run(network);

        System.out.println("Generated network nodes: " + network.getNodes().size());
        System.out.println("Generated network links: " + network.getLinks().size());

        new NetworkWriter(network).write(outputNetwork);
        System.out.println("Network written to: " + outputNetwork);
    }
}