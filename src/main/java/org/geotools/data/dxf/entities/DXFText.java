package org.geotools.data.dxf.entities;

import org.geotools.database.GeometryType;
import org.geotools.data.dxf.header.DXFLineType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.awt.geom.Rectangle2D;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFTables;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFText extends DXFEntity {
    public DXFPoint _point = new DXFPoint(); // 10 ,20
    public String _value = ""; // 1
    public double _height = 0; // 40
    public double _rotation = 0; // 50
    public float _align = 0; // 72
    public float _align2 = 0; // 73
    public String _style = ""; // 7
    public double _angle = 0; // 51
    public double _zoomfactor = 1; // 41
    
    public Rectangle2D.Double _r = new Rectangle2D.Double();

    public DXFText(DXFText newText) {
        this(newText._point.X(), newText._point.Y(), newText._point.Z(), newText._value,
                newText._rotation, newText.getThickness(), newText._height,
                newText._align, newText._align2, newText._style, newText.getColor(),
                newText.getRefLayer(), newText._angle, newText._zoomfactor,
                newText.visibility, newText.getLineType());

        setType(newText.getType());
        setUnivers(newText.getUnivers());
    }

    public DXFText(double x, double y, double z, String value, double rotation, double thickness, double height, float align, float align2, String style, int color, DXFLayer l, double angle, double zoomFactor, int visibility, DXFLineType lineType) {
        super(color, l, visibility, lineType, thickness);
        _point = new DXFPoint(x, y, z, color, l, visibility, thickness);
        _value = value;
        _rotation = rotation;
        _height = height;
        _align = align;
        _align2 = align2;
        _style = style;
        _angle = angle;
        _zoomfactor = zoomFactor;
    }

    public static DXFText read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        DXFLayer l = null;
        String value = "", style = "STANDARD";
        int visibility = 0, c = -1;
        float align = 0, align2 = 0;
        DXFLineType lineType = null;
        double x = 0,
                y = 0,
                z = 0,
                angle = 0,
                rotation = 0,
                zoomfactor = 1,
                thickness = DXFTables.defaultThickness,
                height = 0;

        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;
        Map<String, List<String>> xdata = null;

        boolean doLoop = true;
        while (doLoop) {
            cvp = new DXFCodeValuePair();
            try {
                gc = cvp.read(br);
            } catch (DXFParseException ex) {
                throw new IOException("DXF parse error" + ex.getLocalizedMessage());
            } catch (EOFException e) {
                doLoop = false;
                break;
            }

            switch (gc) {
                case TYPE:
                    String type = cvp.getStringValue();
                    // geldt voor alle waarden van type
                    br.reset();
                    doLoop = false;
                    break;
                case X_1: //"10"
                    x = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    y = cvp.getDoubleValue();
                    break;
                case Z_1: //"30"
                    z = cvp.getDoubleValue();
                    break;
                case TEXT_OR_NAME_2: { //"3"
                    String temp = cvp.getStringValue();
                    if (temp.startsWith("{") && temp.endsWith("}") && temp.contains("|")) {
                        temp = temp.substring(1, temp.length() - 1);
                        temp.substring(temp.lastIndexOf('|'));
                    }
                    temp = temp.replace("\\P", "\n");
                    temp = processText(temp);
                    temp = processText2(temp);
                    value += temp;
                    break;
                }
                case TEXT: { //"1"
                    String temp = cvp.getStringValue();
                    if (temp.startsWith("{") && temp.endsWith("}") && temp.contains("|")) {
                        temp = temp.substring(1, temp.length() - 1);
                        temp.substring(temp.lastIndexOf('|'));
                    }
                    temp = temp.replace("\\P", "\n");
                    temp = processText(temp);
                    temp = processText2(temp);
                    value += temp;
                    break;
                }
                case ANGLE_1: //"50"
                    rotation = cvp.getDoubleValue();
                    break;
                case THICKNESS: //"39"
                    thickness = cvp.getDoubleValue();
                    break;
                case DOUBLE_1: //"40"
                    height = cvp.getDoubleValue() + 1;
                    break;
                case INT_3: //"72"
                    align = cvp.getShortValue();
                    break;
                case INT_4: //"73"
                    align2 = cvp.getShortValue();
                    break;
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case TEXT_STYLE_NAME: //"7"
                    style = cvp.getStringValue();
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                case XDATA_APPLICATION_NAME:
                    xdata = readXdata(cvp.getStringValue(), br, univers, xdata);
                    break;
                default:
                    break;
            }

        }

        switch ((int)align) {
            case 1:
            case 2:
            case 3:
                align2 = 1;
                break;
            case 4:
            case 5:
            case 6:
                align2 = 0.5f;
                break;
            default:
                align2 = 0f;
                break;
        }
        align = 0;

        value = value.replace("(?m)^\\s*$", "");
        if (!value.trim().isEmpty()) {
            DXFText e = new DXFText(x, y, z, value.trim(), rotation, thickness, height, align, align2, style, c, l, angle, zoomfactor, visibility, lineType);
            e.setType(GeometryType.POINT);
            e.setUnivers(univers);
            e.setXData(xdata);
            return e;
        }
        else
            return null;
    }
    
    /**
     * Removes font settings from the string (\f .... ;) and replaces characters
     * encoded in %%XXX (decimal) form with the right characters.
     * 
     * @param text
     * @return modified text
     */
    protected static String processText(String text) {
        text = text.replaceAll("\\\\f[^;]+;", "");
        Pattern pattern = Pattern.compile("%%(\\d\\d\\d)");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(sb, Character.toString((char)Integer.parseInt(matcher.group(1), 10)));
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replaces character encoded in \ uXXXX (hex) form with the right
     * characters.
     * 
     * @param text
     * @return modified text
     */
    protected static String processText2(String text) {
        Pattern pattern = Pattern.compile("\\\\u|U\\+([a-fA-F0-9]{4})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(sb, Character.toString((char)Integer.parseInt(matcher.group(1), 16)));
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public Geometry getGeometry() {
        if (geometry == null) {
        }
        return super.getGeometry();
    }

    @Override
    public void updateGeometry() {
        geometry = getUnivers().getGeometryFactory().createPoint(toCoordinate());
    }

    public Coordinate toCoordinate() {
        if (_point == null || _point._point == null) {
            return null;
        }
        return rotateAndPlace(new Coordinate(_point._point.getX(), _point._point.getY()));
    }

    public String toString(double x, double y, String value, double rotation, double thickness, double height, double align, String style, int c, double angle, double zoomfactor, int visibility) {
        StringBuilder s = new StringBuilder();
        s.append("DXFText [");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("value: ");
        s.append(value + ", ");
        s.append("rotation: ");
        s.append(rotation + ", ");
        s.append("thickness: ");
        s.append(thickness + ", ");
        s.append("height: ");
        s.append(height + ", ");
        s.append("align: ");
        s.append(align + ", ");
        s.append("style: ");
        s.append(style + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("angle: ");
        s.append(angle + ", ");
        s.append("zoomfactor: ");
        s.append(zoomfactor + ", ");
        s.append("visibility: ");
        s.append(visibility);
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity clone() {
        return new DXFText(this);
    }
}
