/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 */
package scenemakerv2.objectfile;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;

import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.utils.image.TextureLoader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.j3d.GeometryArray;
import javax.swing.JOptionPane;

/**
 * <h3>Process lights, textures,...</h3>
 * jfp changes to the code :
 * <dl><dt>assignMaterial
 * <dd>always use LightingEnable and specular color
 * <dt>readMapKd
 * <dd>Change structure and remove code for files other than usual
 * <br>change test for null texture image name
 * <dt>readMaterialFile
 * <dd>Add test to skip unknown tokens
 * </dl>
 * <p>To get textures files, they should be in model directory.
 */
class ObjectFileMaterials extends javax.swing.JPanel implements ImageObserver {
    // DEBUG
    // 1 = Name of materials
    // 16 = Tokens
    private static final int DEBUG = 0;
    private String curName = null;
    private ObjectFileMaterial cur = null;
    private HashMap materials;  // key=String name of material
                                // value=ObjectFileMaterial
    private String basePath;
    private boolean fromUrl;

    private class ObjectFileMaterial {
        public Color3f Ka;
        public Color3f Kd;
        public Color3f Ks;
        public int illum;
        public float Ns;
        public Texture2D t;
        public boolean transparent;
        public float transparencyLevel;
    }
    
    /**
     * assign a Material to a shape3d.
     * <p>Changes to the code : force LightingEnable
     * @param matName
     * @param shape 
     */
    void assignMaterial(String matName, Shape3D shape) {
        if (DEBUG == 1) {
            System.out.println("Color " + matName);
        }
        Material mat = new Material();
        ObjectFileMaterial objFilMat = (ObjectFileMaterial) materials.get(matName);
        Appearance app = new Appearance();
        if (objFilMat != null) {
            if (objFilMat.Ka != null) {         // Set ambient & diffuse color
                mat.setAmbientColor(objFilMat.Ka);
            }
            if (objFilMat.Kd != null) {
                mat.setDiffuseColor(objFilMat.Kd);
            }
            // Change code to always set specular and LightingEnable 03/02/2017 ****
            if (objFilMat.Ks != null) {     // Set specular color. 
                mat.setSpecularColor(objFilMat.Ks);
            }
            mat.setLightingEnable(true);    // hard coded 03/02/2017 ****
            if (objFilMat.Ns != -1.0f) {
                mat.setShininess(objFilMat.Ns);
            }
            if (objFilMat.t != null) {
                app.setTexture(objFilMat.t);
                if ((((GeometryArray) shape.getGeometry()).getVertexFormat()
                        & GeometryArray.TEXTURE_COORDINATE_2) == 0) { // Create TextCoords if not present
                    TexCoordGeneration tcg = new TexCoordGeneration();
                    app.setTexCoordGeneration(tcg);
                }
            }
            if (objFilMat.transparent) {
                app.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,
                        objFilMat.transparencyLevel));
            }
        }
        app.setMaterial(mat);
        if (DEBUG == 1) {
            System.out.println(mat);
        }
        shape.setAppearance(app);
    } // End of assignMaterial

    private void readName(ObjectFileParser st) throws ParsingErrorException {
        st.getToken();
        if (st.ttype == ObjectFileParser.TT_WORD) {
            if (curName != null) {
                materials.put(curName, cur);
            }
            curName = st.sval;
            cur = new ObjectFileMaterial();
        }
        st.skipToNextLine();
    } // End of readName

    private void readAmbient(ObjectFileParser st) throws ParsingErrorException {
        Color3f p = new Color3f();
        st.getNumber();
        p.x = (float) st.nval;
        st.getNumber();
        p.y = (float) st.nval;
        st.getNumber();
        p.z = (float) st.nval;
        cur.Ka = p;
        st.skipToNextLine();
    } // End of readAmbient

    private void readDiffuse(ObjectFileParser st) throws ParsingErrorException {
        Color3f p = new Color3f();
        st.getNumber();
        p.x = (float) st.nval;
        st.getNumber();
        p.y = (float) st.nval;
        st.getNumber();
        p.z = (float) st.nval;
        cur.Kd = p;
        st.skipToNextLine();
    } // End of readDiffuse

    private void readSpecular(ObjectFileParser st) throws ParsingErrorException {
        Color3f p = new Color3f();
        st.getNumber();
        p.x = (float) st.nval;
        st.getNumber();
        p.y = (float) st.nval;
        st.getNumber();
        p.z = (float) st.nval;
        cur.Ks = p;
        st.skipToNextLine();
    } // End of readSpecular

    private void readIllum(ObjectFileParser st) throws ParsingErrorException {
        st.getNumber();
        cur.illum = (int) st.nval;
        st.skipToNextLine();
    } // End of readSpecular

    private void readTransparency(ObjectFileParser st) throws ParsingErrorException {
        st.getNumber();
        cur.transparencyLevel = (float) st.nval;
        if (cur.transparencyLevel < 1.0f) {
            cur.transparent = true;
        }
        st.skipToNextLine();
    } // End of readTransparency

    private void readShininess(ObjectFileParser st) throws ParsingErrorException {
        st.getNumber();
        cur.Ns = (float) st.nval;
        if (cur.Ns < 1.0f) {
            cur.Ns = 1.0f;
        } else if (cur.Ns > 128.0f) {
            cur.Ns = 128.0f;
        }
        st.skipToNextLine();
    } // End of readSpecular

    /**
     * build Texture2D.
     * <p>Code for TextureLoaders changed.(01/02/2017)
     * <br>test for tFile null changed (22/02/2017) 
     * @param objParse
     */
    public void readMapKd(ObjectFileParser objParse) {
        objParse.lowerCaseMode(false);      // Filenames are case sensitive
        String tFile = null;                // Get name of texture file (skip path)
        do {
            objParse.getToken();
            if (objParse.ttype == ObjectFileParser.TT_WORD) {
                tFile = objParse.sval;
            }
        } while (objParse.ttype != ObjectFileParser.TT_EOL);
        objParse.lowerCaseMode(true);
        // some material files have lines with map_kd token but no filename.  ****
        // the next test has been changed (null pointer exeception). ****
        if (tFile == null ) {
            objParse.skipToNextLine();
            return;
        }
        TextureLoader t = null;
        if (fromUrl) {  // inetrnet file
            try {
                t = new TextureLoader(new URL(basePath + tFile), "RGB", TextureLoader.GENERATE_MIPMAP, null);
            } catch (MalformedURLException ex) {
                Logger.getLogger(ObjectFileMaterials.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {        // local file
            String theFile = basePath + tFile; 
            File img = new File(theFile);
            if (img.exists()) {
                t = new TextureLoader(theFile, "RGB", TextureLoader.GENERATE_MIPMAP, null);
            } else {    // wrong file name
                JOptionPane.showMessageDialog(this, theFile + " not found");
                return;
            }
        }
        if (t == null) {
            objParse.skipToNextLine();
            JOptionPane.showMessageDialog(this, "null textureLoader");
            return;
        }
        Texture2D texture = (Texture2D) t.getTexture();
        if (texture == null) {
            objParse.skipToNextLine();
            JOptionPane.showMessageDialog(this, "null texture");
            return;
        }
        cur.t = texture;
    } // End of readMapKd
    /**
     * read material file to convert parameters to corresponding Java3D code.
     * <p>Add a line at the end of <i>if else if</i> to prevent crash for unknown tokens (22/02/2017°
     * @param st
     * @throws ParsingErrorException 
     */
    private void readMaterialFile(ObjectFileParser st) throws ParsingErrorException {
        st.getToken();
        while (st.ttype != ObjectFileParser.TT_EOF) {
            if (DEBUG == 16) {        // Print out one token for each line
                System.out.print("Token ");
                if (st.ttype == ObjectFileParser.TT_EOL) {
                    System.out.println("EOL");
                } else if (st.ttype == ObjectFileParser.TT_WORD) {
                    System.out.println(st.sval);
                } else {
                    System.out.println((char) st.ttype);
                }
            }   // end debug
            if (st.ttype == ObjectFileParser.TT_WORD) {
                if (st.sval.equals("newmtl")) {
                    readName(st);
                } else if (st.sval.equals("ka")) {
                    readAmbient(st);
                } else if (st.sval.equals("kd")) {
                    readDiffuse(st);
                } else if (st.sval.equals("ks")) {
                    readSpecular(st);
                } else if (st.sval.equals("illum")) {
                    readIllum(st);
                } else if (st.sval.equals("d")) {
                    readTransparency(st);
                } else if (st.sval.equals("ns")) {
                    readShininess(st);
                } else if (st.sval.equals("tf")) {
                    st.skipToNextLine();
                } else if (st.sval.equals("sharpness")) {
                    st.skipToNextLine();
                } else if (st.sval.equals("map_kd")) {
                    readMapKd(st);
                } else if (st.sval.equals("map_ka")) {
                    st.skipToNextLine();
                } else if (st.sval.equals("map_ks")) {
                    st.skipToNextLine();
                } else if (st.sval.equals("map_ns")) {
                    st.skipToNextLine();
                } else if (st.sval.equals("bump")) {
                    st.skipToNextLine();
                }   else st.skipToNextLine();   // skip unknown token. Added 22/02/2017
            }
            st.skipToNextLine();
            st.getToken();  // Get next token
        }
        if (curName != null) {
            materials.put(curName, cur);
        }
    } // End of readFile
    /**
     * get Material file in reader.
     * <p>Call method to read it
     * @param fromUrl
     * @param basePath
     * @param fileName
     * @throws ParsingErrorException 
     */
    void getMaterialFile(boolean fromUrl, String basePath, String fileName)
            throws ParsingErrorException {
        Reader reader;
        this.basePath = basePath;
        this.fromUrl = fromUrl;
        try {
            if (fromUrl) {
                reader = (Reader) (new InputStreamReader(
                        new BufferedInputStream(
                                (new URL(basePath + fileName).openStream()))));
            } else {
                reader = new BufferedReader(new FileReader(basePath + fileName));
            }
        } catch (IOException e) {
            // couldn't find it - ignore mtllib
            return;
        }
        //    System.out.println("Material file: " + basePath + fileName);
        ObjectFileParser st = new ObjectFileParser(reader);
        readMaterialFile(st);
    }  // End of readMaterialFile

    ObjectFileMaterials() throws ParsingErrorException {
        Reader reader = new StringReader(DefaultMaterials.materials);
        ObjectFileParser st = new ObjectFileParser(reader);
        materials = new HashMap(500);   // size increased 22/02/2017
        readMaterialFile(st);
    } // End of ObjectFileMaterials

    /**
     * Implement the ImageObserver interface. Needed to load jpeg and gif files
     * using the Toolkit.
     */
    @Override
    public boolean imageUpdate(Image img, int flags,
            int x, int y, int w, int h) {

        return (flags & (ALLBITS | ABORT)) == 0;
    } // End of imageUpdate
} // End of class ObjectFileMaterials
// End of file ObjectFileMaterials.java
