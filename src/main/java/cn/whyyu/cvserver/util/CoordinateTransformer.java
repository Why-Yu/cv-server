package cn.whyyu.cvserver.util;

import org.locationtech.proj4j.*;

public class CoordinateTransformer {
    private static final CRSFactory crsFactory = new CRSFactory();
    private static final CoordinateReferenceSystem WGS84 = crsFactory.createFromName("epsg:4326");
    private static final CoordinateReferenceSystem GEO2000 = crsFactory.createFromName("epsg:4526");
    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CoordinateTransform wgsToGeo2000 = ctFactory.createTransform(WGS84, GEO2000);
    private static final CoordinateTransform geo2000ToWgs = ctFactory.createTransform(GEO2000, WGS84);

    /**
    WGS84转EPSG:4526
     */
    public static ProjCoordinate wgsToGeo(double lng, double lat) {
        ProjCoordinate result = new ProjCoordinate();
        wgsToGeo2000.transform(new ProjCoordinate(lng, lat), result);
        return result;
    }

    /**
     *EPSG:4526转WGS84
     */
    public static ProjCoordinate geoToWgs(double x, double y) {
        ProjCoordinate result = new ProjCoordinate();
        geo2000ToWgs.transform(new ProjCoordinate(x, y), result);
        return result;
    }

    public static void main(String[] args) {
//        System.out.println(new ProjCoordinate(114, 30));
//        System.out.println(CoordinateTransformer.wgsToGeo(114, 30));
        System.out.println(CoordinateTransformer.geoToWgs(38524070, 3388674));
//        System.out.println(CoordinateTransformer.geoToWgs(38524094 ,3388707));
    }

}
