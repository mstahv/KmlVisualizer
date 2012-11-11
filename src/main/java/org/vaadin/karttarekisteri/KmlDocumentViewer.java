package org.vaadin.karttarekisteri;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.vaadin.vol.Area;
import org.vaadin.vol.Bounds;
import org.vaadin.vol.OpenLayersMap;
import org.vaadin.vol.Point;
import org.vaadin.vol.Style;
import org.vaadin.vol.StyleMap;
import org.vaadin.vol.VectorLayer.SelectionMode;
import org.vaadin.vol.VectorLayer.VectorSelectedEvent;
import org.vaadin.vol.VectorLayer.VectorSelectedListener;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.UriFragmentUtility;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

public class KmlDocumentViewer extends CssLayout implements ComponentContainer,
        ClickListener {

    private static Document doc;
    static {
        loadDocument();
    }

    private static void loadDocument() {
        Kml unmarshal;
        try {
            unmarshal = Kml
                    .unmarshal(new URL(
                            "http://karttarekisteri.fi/karttarekisteri2/ssl_kml_lataus_testi.php")
                            .openStream());
            // For offline example, use
            // KmlDocumentViewer.class.getResourcesAsStream("test.kml");
            doc = (Document) unmarshal.getFeature();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static final String TM35 = "EPSG:3067";
    private OpenLayersMap map = new OpenLayersMap();
    private org.vaadin.vol.OpenStreetMapLayer osm = new org.vaadin.vol.OpenStreetMapLayer();
    private org.vaadin.vol.VectorLayer vectors = new org.vaadin.vol.VectorLayer();
    private NativeButton showAll = new NativeButton("Show all", this);
    private NativeButton reloadData = new NativeButton("Reload", this);

    private UriFragmentUtility ufu = new UriFragmentUtility();

    public KmlDocumentViewer(String focusedFeature) {
        addComponent(map);
        addComponent(ufu);
        addComponent(showAll);
        reloadData.setDescription("Refresh data from remote server");
        addComponent(reloadData);
        setSizeFull();
        map.addLayer(osm);
        map.addLayer(vectors);
        map.setSizeFull();
        map.setApiProjection(TM35);
        map.setImmediate(true);

        extractStyles(doc);

        displayFeatures(focusedFeature);

        vectors.setSelectionMode(SelectionMode.SIMPLE);
        vectors.setImmediate(true);
        vectors.addListener(new VectorSelectedListener() {
            public void vectorSelected(VectorSelectedEvent event) {
                final Area component = (Area) event.getVector();
                final String data = (String) component.getData();

                ExternalResource externalResource = new ExternalResource(
                        "http://www.karttarekisteri.fi/karttarekisteri2/www_visualisointi/tiedot.php?t="
                                + data);
                externalResource.setMIMEType("text/html");

                final Window window = new Window("Details");
                window.getContent().setSizeFull();
                window.setHeight("50%");
                window.setWidth("50%");

                Button button = new Button("Focus this feature");
                button.addListener(new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        ufu.setFragment(data);
                        displayFeatures(data);
                        window.getParent().removeWindow(window);
                    }
                });
                window.addComponent(button);
                Embedded embedded = new Embedded(null, externalResource);
                embedded.setType(Embedded.TYPE_BROWSER);
                embedded.setSizeFull();
                window.addComponent(embedded);
                ((VerticalLayout) window.getContent()).setExpandRatio(embedded,
                        1);
                window.setClosable(true);
                window.center();
                getWindow().addWindow(window);
                vectors.setSelectedVector(null);
            }
        });

    }

    private void displayFeatures(String focusedFeature) {
        vectors.removeAllComponents();
        boolean focusFeature = focusedFeature != null
                && !focusedFeature.isEmpty();
        showAll.setVisible(focusFeature);
        List<Feature> feature = doc.getFeature();
        Bounds b = null;

        for (Feature feature2 : feature) {
            Placemark area = (Placemark) feature2;
            String name = area.getName();
            if (focusFeature && !name.equals(focusedFeature)) {
                continue;
            }
            Polygon geometry = (Polygon) area.getGeometry();

            List<Coordinate> coords = geometry.getOuterBoundaryIs()
                    .getLinearRing().getCoordinates();
            if (coords.size() == 0) {
                continue;
            }
            Point[] points = new Point[coords.size()];
            for (int i = 0; i < points.length; i++) {
                Coordinate coordinate = coords.get(i);
                points[i] = new Point(coordinate.getLongitude(),
                        coordinate.getLatitude());
            }
            if (b == null) {
                b = new Bounds();
                b.setTop(points[0].getLat());
                b.setBottom(points[0].getLat());
                b.setLeft(points[0].getLon());
                b.setRight(points[0].getLon());
            }
            b.extend(points);

            Area area2 = new Area();
            area2.setRenderIntent(area.getStyleUrl().substring(1));
            area2.setProjection(TM35);
            area2.setPoints(points);
            vectors.addComponent(area2);
            area2.setData(name);

        }
        map.zoomToExtent(b);
    }

    private void extractStyles(Document doc) {
        List<StyleSelector> styleSelector = doc.getStyleSelector();
        StyleMap stylemap = new StyleMap();
        stylemap.setExtendDefault(true);
        for (StyleSelector s : styleSelector) {
            de.micromata.opengis.kml.v_2_2_0.Style s2 = (de.micromata.opengis.kml.v_2_2_0.Style) s;
            String id = s.getId();
            Style style = new Style(id);
            LineStyle lineStyle = s2.getLineStyle();
            style.setStrokeColor(kmlGRBtoRGB(lineStyle.getColor()));
            PolyStyle polyStyle = s2.getPolyStyle();
            style.setFillColor(kmlGRBtoRGB(polyStyle.getColor()));
            stylemap.setStyle(id, style);
        }

        vectors.setStyleMap(stylemap);
    }

    private String kmlGRBtoRGB(String color) {
        return "#" + color.substring(4, 6) + color.substring(2, 4)
                + color.substring(0, 2);
    }

    @Override
    public void buttonClick(ClickEvent event) {
        if (event.getButton() == showAll) {
            vectors.removeAllComponents();
            displayFeatures(null);
        } else if(event.getButton() == reloadData) {
            loadDocument();
            displayFeatures(null);
        }
    }

    @Override
    protected String getCss(Component c) {
        if (showAll == c) {
            return "position: absolute; right:5px; top:5px; z-index:1000;";
        } else if (reloadData == c) {
            return "position: absolute; right:5px; bottom:5px; z-index:1000;";
        }
        return super.getCss(c);
    }

}
