package org.matsim.project;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ShapefileProcessor {
    public static Map<String, DistrictData> loadDistrictData(String shapefilePath) throws Exception {
        Map<String, DistrictData> districtDataMap = new HashMap<>();
        FileDataStore store = FileDataStoreFinder.getDataStore(new File(shapefilePath));
        SimpleFeatureCollection features = store.getFeatureSource().getFeatures();

        try (SimpleFeatureIterator iterator = features.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                String tractId = (String) feature.getAttribute("name");
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                districtDataMap.put(tractId, new DistrictData(tractId, null, geometry, ""));
            }
        }
        return districtDataMap;
    }
}
