-keep class app.funput.funput.ime.nativebridge.FunputNative {
    native <methods>;
}

-keep class app.funput.funput.ime.nativebridge.PersonalSuggestionNative {
    native <methods>;
}

# ML Kit finds its component registrars through manifest meta-data and creates them
# reflectively with the no-arg constructor. firebase-components 16.1.0 only ships
# `-keep class * implements ComponentRegistrar`, which in R8 full mode keeps the class
# but not its constructor, so release builds lost SharedPrefManager and every ML Kit
# speech client failed with a NullPointerException.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
