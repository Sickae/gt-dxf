/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.data.dxf.header;

import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;


import org.geotools.data.dxf.entities.DXFEntity;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.data.dxf.parser.DXFConstants;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFUnivers;

/**
 *
 * @author Gertjan
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/build/maven/javadoc/../../../modules/unsupported/dxf/src/main/java/org/geotools/data/dxf/header/DXFBlockRecord.java $
 */
public class DXFBlockRecord extends DXFBlock {
    /*
    public Vector<DXFEntity> theEntities = new Vector<DXFEntity>();
    public DXFPoint _point = new DXFPoint();
    public String _name;
    public int _flag;
     */

    // TODO GJ DELETE WHOLE UNUSED CLASS???
    public DXFBlockRecord(double x, double y, double z, int flag, String name, Vector<DXFEntity> ent, int c, DXFLayer l) {
        super(x, y, z, flag, name, ent, c, l);
    /*
    _point = new DXFPoint(x, y, c, l, 0, 1);
    _name = name;
    _flag = flag;

    if (ent == null) {
    ent = new Vector<DXFEntity>();
    }
    theEntities = ent;
     * */
    }

    public static DXFBlockRecord read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        Vector<DXFEntity> sEnt = new Vector<DXFEntity>();
        String name = "";
        double x = 0, y = 0, z = 0;
        int flag = 0;
        DXFLayer l = null;

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

            /// TODO GJ Delete Block::read println
            //System.out.println("Block: read " + gc);


            switch (gc) {
                case TYPE:
                    String type = cvp.getStringValue();
                    if (DXFConstants.ENDBLK.equals(type)) {
                        doLoop = false;
                    } else if (DXFConstants.ENDSEC.equals(type)) {
                        // hack voor als ENDBLK ontbreekt
                        doLoop = false;
                        br.reset();
                    }
                    break;
                case NAME:
                    name = cvp.getStringValue();
                    break;
                default:
                    //myLog.writeLog("Unknown :" + ligne_temp + " (" + ligne + ")");
                    break;
            }
        }
        DXFBlockRecord e = new DXFBlockRecord(x, y, z, flag, name, sEnt, DXFColor.getDefaultColorIndex(), l);
        return e;
    }
    /*
    public String toString(double x, double y, int flag, String name, int numEntities, int c) {
    StringBuffer s = new StringBuffer();
    s.append("DXFBlock [");
    s.append("x: ");
    s.append(x + ", ");
    s.append("y: ");
    s.append(y + ", ");
    s.append("flag: ");
    s.append(flag + ", ");
    s.append("name: ");
    s.append(name + ", ");
    s.append("color: ");
    s.append(c + ", ");
    s.append("numEntities: ");
    s.append(numEntities);
    s.append("]");
    return s.toString();
    }

    @Override
    public DXFEntity translate(double x, double y) {
    return this;
    }
     */
}

