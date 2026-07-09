# R8 with the AndroidX/Compose consumer rules handles almost everything.
# The one thing not covered: ViewModels are instantiated by reflection through
# viewModel(), so keep their Application constructor.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(android.app.Application);
}

# Kotlin coroutines ships its own rules; this just silences the debug-probes warning.
-dontwarn kotlinx.coroutines.debug.**
