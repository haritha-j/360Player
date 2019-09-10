/**
 * Copyright 2016-present, Facebook, Inc.
 * All rights reserved.
 *
 * Licensed under the Creative Commons CC BY-NC 4.0 Attribution-NonCommercial
 * License (the "License"). You may obtain a copy of the License at
 * https://creativecommons.org/licenses/by-nc/4.0/.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.decodertest.gles;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.example.decodertest.R;
import com.example.decodertest.MainActivity;

/**
 * Creates and renders a sphere centered at (0, 0, 0) textured with the current video frame.
 */
public class SphericalSceneRenderer {
    public static final int SPHERE_SLICES = 180;
    private static final int SPHERE_INDICES_PER_VERTEX = 1;
    private static final float SPHERE_RADIUS = 400.0f;

    private ShaderProgram shaderProgram;

    private int aPositionLocation;
    private int uMVPMatrixLocation;
    private int uTextureMatrixLocation;
    private int aTextureCoordLocation;

    private float[] pvMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] rotationMatrixx = new float[16];
    private Sphere sphere;

    public SphericalSceneRenderer(Context context) {


        shaderProgram = new ShaderProgram(
                MainActivity.readRawTextFile(context, R.raw.video_vertex_shader),
                MainActivity.readRawTextFile(context, R.raw.video_fragment_shader));

        /**Not clear
         * But getting a specific location of those values*/
        aPositionLocation = shaderProgram.getAttribute("aPosition");
        uMVPMatrixLocation = shaderProgram.getUniform("uMVPMatrix");
        uTextureMatrixLocation = shaderProgram.getUniform("uTextureMatrix");
        aTextureCoordLocation = shaderProgram.getAttribute("aTextureCoord");
        Log.d("Debug","aPosition :" +aPositionLocation+ " uMVPMatrix: "+ uMVPMatrixLocation
        +" uTextureMatrix "+ uTextureMatrixLocation + " aTextureCoord: "+ aTextureCoordLocation);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        sphere = new Sphere(SPHERE_SLICES, 0.f, 0.f, 0.f, SPHERE_RADIUS, SPHERE_INDICES_PER_VERTEX);

        /**Program is created using the glCreateProgram. There are several functions called when
         * creting the program. Previously mentioned vertex and fragment shaders are used for the
         * program creation. This function install the created program to the rendering process in
         * in this initial step using its program handle*/
        GLES20.glUseProgram(shaderProgram.getShaderHandle());

        /**Bit unclear the remaining part. Modifying the vertex positino and texture location
         * according to the sphere coordinates defined.
         * enable the above attributes arrays as */
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLHelpers.checkGlError("glEnableVertexAttribArray");

        /**Modify the aPositionLocation vector according to the sphere created*/
        GLES20.glVertexAttribPointer(aPositionLocation, 3,
                GLES20.GL_FLOAT, false, sphere.getVerticesStride(), sphere.getVertices());

        GLHelpers.checkGlError("glVertexAttribPointer");


        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLHelpers.checkGlError("glEnableVertexAttribArray");

        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2,
                GLES20.GL_FLOAT, false, sphere.getVerticesStride(),
                sphere.getVertices().duplicate().position(3));
        GLHelpers.checkGlError("glVertexAttribPointer");
    }

    public void onDrawFrame(
            int textureId,
            float[] textureMatrix,
            float[] modelMatrix,
            float[] viewMatrix,
            float[] projectionMatrix, float[] rotationMatrix) {


        Matrix.translateM(textureMatrix, 0, 0, 1, 0);
        //Matrix.setIdentityM(rotationMatrixx,0);
       // Matrix.multiplyMM(viewMatrix, 0, viewMatrix, 0, rotationMatrix , 0);
        Matrix.multiplyMM(pvMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        //Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, pvMatrix, 0, modelMatrix , 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                textureId);

        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, textureMatrix, 0);
        GLES20.glUniformMatrix4fv(uMVPMatrixLocation, 1, false, mvpMatrix, 0);

        for (int j = 0; j < sphere.getNumIndices().length; ++j) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    sphere.getNumIndices()[j], GLES20.GL_UNSIGNED_SHORT,
                    sphere.getIndices()[j]);
        }
    }

    public void release() {
        shaderProgram.release();
    }
}
