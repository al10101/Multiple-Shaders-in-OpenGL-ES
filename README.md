# Multiple-Shaders-in-OpenGL-ES
A project that implements three different OpenGL shaders in Android system, using Kotlin.

![alt text](https://github.com/Al10101/Multiple-Shaders-in-OpenGL-ES/blob/main/sampleImage.png?raw=true)

I did not find a project that implements different shaders in OpenGL ES for Android system, so I decided to upload my implementation once I understood how to do it. I hope that this repo is useful for people that do not know how to use different shaders in Android.

## Overview

The main code is inside [app/src/main/java/com/albertocabrera/multipleshaders](app/src/main/java/com/albertocabrera/multipleshaders) (sorry to make it so "branchy", I forgot the ```gitignore``` file in the initial commit and the complexity of correcting it is not worth it). 

The main renderer, Renderer.kt, sets three spheres rotating over the y axis. It calls three different classes: ```ShinnySphere``` (green), ```OpaqueSphere``` (blue) and ```LowPolySphere``` (red). All these classes are child from ```Model3D```, an "empty" class that I created only to override common functions while calling the objects inside ```onDrawFrame()```.

Actually, the same renderer could be achieved by defining various *if* statements or implementing a little bit more uniforms inside a global shader, but my goal was to understand how to use different shaders inside a common renderer, so I decided to keep very simple all the shaders definitions.

All spheres, refered from now on as models, are defined from scratch, computing every point as a function of *theta* and *phi*. ```LowPolySphere``` has a gradient to emulate the light reflection and make it a little more interesting. To update the light's direction while rotating the models, it also overrides a function ```setMovement()``` a little bit more complex than ```OpaqueSphere``` and ```LowPolySphere```, but it's easy to get once you inderstand the model matrix's transformtions.

The folder [utils](app/src/main/java/com/albertocabrera/multipleshaders/utils) inside the [main folder](app/src/main/java/com/albertocabrera/multipleshaders) has common functions and constants that are used throughout the whole application. I suggest to check them all before reading the methods used in each model.

Finally, all three rendered models use different shaders, defined inside [app/src/main/res/raw](app/src/main/res/raw). The most important part to make everything work is to use the corresponding program for each model and setting the correct uniforms and attributes per frame, all of which is inside the ```onDrawFrame()``` function in the renderer:

```
override fun onDrawFrame() {
  // ...
  // Render all models
  for (model in models) {
    // Define movement
    model.setMovement()
    // Draw
    model.useProgram()
    model.fragmentUniforms()
    model.bindData()
    model.drawPrimitives()
  }
}
```

The purpose of this repository is to serve as a guide for those that are having problems implementing various shaders inside a same project, hopefully it will. 
