package org.geotools.data.dxf.parser;

import org.geotools.data.dxf.header.DXFBlock;
import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFBlocks;
import org.geotools.data.dxf.header.DXFEntities;
import org.geotools.data.dxf.header.DXFHeader;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFTables;
import org.geotools.data.dxf.header.DXFLineType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import org.geotools.data.dxf.entities.DXFEntity;

public class DXFUnivers implements DXFConstants {
    public static final PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
    public static final int NUM_OF_SEGMENTS = 16; // Minimum number of segments for a circle (also used for arc)
    public static final double MIN_ANGLE = 2 * Math.PI / NUM_OF_SEGMENTS; // Minimum number of segments for a circle (also used for arc)
    private Vector<DXFBlockReference> _entForUpdate = new Vector<DXFBlockReference>();
    public Vector<DXFTables> theTables = new Vector<DXFTables>();
    public Vector<DXFBlock> theBlocks = new Vector<DXFBlock>();
    public Vector<DXFEntity> theEntities = new Vector<DXFEntity>();
    private DXFHeader _header;
    private GeometryFactory geometryFactory = null;
    private Geometry errorGeometry = null;
    private HashMap insertsFound = new HashMap();
    private ArrayList dxfInsertsFilter;
    private String info = ""; // Used for getInfo();  returns this string with information about the file

    public DXFUnivers(ArrayList dxfInsertsFilter) {
        this.dxfInsertsFilter = dxfInsertsFilter;
    }

    public boolean isFilteredInsert(String blockName) {
        return dxfInsertsFilter.contains(blockName);
    } 

    public void read(DXFLineNumberReader br) throws IOException {
        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;

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
                    if (type.equals(SECTION)) {
                        readSection(br);
                    }
                    break;
                default:
                    break;
            }
        }
        //updateRefBlock();
        
        // Update coordinates according to the UCS
        //for (DXFEntity e : theEntities)
        //    e.translate(-_header._UCSORG.X(), -_header._UCSORG.Y());
    }

    public void readSection(DXFLineNumberReader br) throws IOException {
        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;

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
                    if (type.equals(ENDSEC)) {
                        doLoop = false;
                        break;
                    }
                    break;
                case NAME:
                    String name = cvp.getStringValue();
                    if (name.equals(HEADER)) {
                        _header = DXFHeader.read(br);
                        if (_header._EXTMAX == null || _header._EXTMIN == null) {
                            _header = new DXFHeader();
                        }
                        /* construct geometry factory */
                        geometryFactory = new GeometryFactory(precisionModel, _header._SRID);
                    } else if (name.equals(TABLES)) {
                        DXFTables at = DXFTables.readTables(br, this);
                        theTables.add(at);
                    } else if (name.equals(BLOCKS)) {
                        DXFBlocks ab = DXFBlocks.readBlocks(br, this);
                        theBlocks.addAll(ab.theBlocks);
                    } else if (name.equals(ENTITIES)) {
                        DXFEntities dxfes = DXFEntities.readEntities(br, this);
                        theEntities.addAll(dxfes.theEntities);
                    // toevoegen aan layer doen we even niet, waarschijnlijk niet nodig
                    //if (o != null && o._refLayer != null) {
                    //    o._refLayer.theEnt.add(o);
                    //}
                    }
                    break;
                default:
                    break;
            }

        }
    }

    public DXFBlock findBlock(String nom) {
        DXFBlock b = null;
        for (int i = 0; i < theBlocks.size(); i++) {
            if (theBlocks.elementAt(i)._name.equals(nom)) {
                insertsFound.put(nom, true);
                return theBlocks.elementAt(i);
            }
        }
        return b;
    }

    public DXFLayer findLayer(String nom) {
        DXFLayer l = null;
        for (int i = 0; i < theTables.size(); i++) {
            for (int j = 0; j < theTables.elementAt(i).theLayers.size(); j++) {
                if (theTables.elementAt(i).theLayers.elementAt(j).getName().equals(nom)) {
                    l = theTables.elementAt(i).theLayers.elementAt(j);
                    return l;
                }
            }
        }

        l = new DXFLayer(nom, DXFColor.getDefaultColorIndex());

        if (theTables.size() < 1) {
            theTables.add(new DXFTables());
        }

        theTables.elementAt(0).theLayers.add(l);
        return l;
    }

    public DXFLineType findLType(String name) {
        for (int i = 0; i < theTables.size(); i++) {
            for (int j = 0; j < theTables.elementAt(i).theLineTypes.size(); j++) {
                if (theTables.elementAt(i).theLineTypes.elementAt(j)._name.equals(name)) {
                    return theTables.elementAt(i).theLineTypes.elementAt(j);
                }
            }
        }
        return null;
    }

    public String getInfo() {
        return info;
    }

    public GeometryFactory getGeometryFactory() {
        if (geometryFactory == null) {
            geometryFactory = new GeometryFactory(precisionModel);
        }
        return geometryFactory;
    }

    public void setGeometryFactory(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    public Geometry getErrorGeometry() {
        if (errorGeometry == null && geometryFactory != null) {
            errorGeometry = geometryFactory.createPoint(new Coordinate(0.0, 0.0));
        }
        return errorGeometry;
    }

    public void setErrorGeometry(Geometry errorGeometry) {
        this.errorGeometry = errorGeometry;
    }

    public static String makeTabs(String text, int length) {
        for (int i = text.length() - 1; i < length; i++) {
            text += " ";
        }
        return text;
    }
    
    public DXFHeader getHeader() {
        return _header;
    }
}
