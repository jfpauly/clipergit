# clipergit
Cliper/Java3d
changes to objectfile
line 312 add int grpnum
line 468 in the else clause add :
if (st.sval.contains("null")) {    
                 curGroup = "group" + grpnum;
                 grpnum++;
             }
after line 486 add :
curGroup = "group" + grpnum;    
                 grpnum++;  
method void readFile(ObjectFileParser st) .... :
Add O token (object) which was unknown and made a crash with some files.
Add else statement to prevent crashes due to unknown tokens.


changes to objectfileMaterials
assignMaterial  : always use LightingEnable and specular color
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
    
    
readMapKd :   Change structure and remove code for image files other than usual ones
              change test for null texture image name
readMaterialFile : Add test to skip unknown tokens line 354 :
 else st.skipToNextLine(); 
