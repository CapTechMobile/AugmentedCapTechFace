package com.captech.augmentedcaptechface

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import java.util.*


/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class ARFaceActivity : AppCompatActivity() {
    private val MIN_OPENGL_VERSION = 3.0
    private lateinit var arFragment: ArFragment
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_face_layout)
        if (!checkIsSupportedDeviceOrFinish())
            return
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        val sceneView = arFragment.arSceneView

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST

        val scene = sceneView.scene

        scene.addOnUpdateListener {
            val faceList = sceneView.session!!.getAllTrackables(AugmentedFace::class.java)
            // Make new AugmentedFaceNodes for any new faces.
            for (face in faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    val faceNode = AugmentedFaceNode(face)
                    faceNode.setParent(scene)

                    //add light bulb above head
                    ViewRenderable.builder().setView(this, R.layout.idea_view).build()
                        .thenAccept {
                            val lightBulb = Node()
                            val localPosition = Vector3()
                            //lift the light bulb to be just above your head.
                            localPosition.set(0.0f, 0.17f, 0.0f)
                            lightBulb.localPosition = localPosition
                            lightBulb.setParent(faceNode)
                            lightBulb.renderable = it

                        }

                    //give the face a little blush
                    Texture.builder()
                        .setSource(this, R.drawable.blush_texture)
                        .build()
                        .thenAccept { texture ->
                            faceNode.faceMeshTexture = texture
                        }
                    faceNodeMap[face] = faceNode
                }
            }

            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
            val faceIterator = faceNodeMap.entries.iterator()
            while (faceIterator.hasNext()) {
                val entry = faceIterator.next()
                val face = entry.key
                if (face.trackingState == TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    faceNode.children.clear()
                    faceIterator.remove()
                }
            }
        }
    }

    /*
       Method that checks if AR Core is available on this device.
    */
    private fun checkIsSupportedDeviceOrFinish(): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(this) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            finish()
            return false
        }
        val openGlVersionString = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            finish()
            return false
        }
        return true
    }
}