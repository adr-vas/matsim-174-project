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
                .setIncludeLinkAtCoordWithHierarchy((coord, hierarchyLevel) -> hierarchyLevel <= LinkProperties.LEVEL_PRIMARY
                        || ShpGeometryUtils.isCoordInPreparedGeometries(coord, filterGeometries))
                .setAfterLinkCreated((link, osmTags, direction) -> {
                    Set<String> modes = new HashSet<>(link.getAllowedModes());
                    modes.add(TransportMode.car);
                    if (osmTags.containsKey(OsmTags.CYCLEWAY)) {
                        modes.add(TransportMode.bike);
                    }
                    link.setAllowedModes(modes);
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