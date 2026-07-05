package app.funput.funput.keyboard.ui

import android.content.Context
import android.content.Intent

private const val FunputAppPackage = "app.funput.funput"
private const val FunputMainActivity = "app.funput.funput.MainActivity"

internal fun Context.openFunputSettings() {
    startActivity(
        Intent(Intent.ACTION_MAIN).apply {
            setClassName(FunputAppPackage, FunputMainActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        },
    )
}
