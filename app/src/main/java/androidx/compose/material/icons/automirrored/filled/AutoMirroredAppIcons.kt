package androidx.compose.material.icons.automirrored.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser

private fun autoMirroredIcon(name: String, pathData: String): ImageVector {
    return materialIcon(name = name, autoMirror = true) {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
}

val Icons.AutoMirrored.Filled.HelpOutline: ImageVector by lazy {
    autoMirroredIcon("AutoMirrored.Filled.HelpOutline", "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z")
}

val Icons.AutoMirrored.Filled.Sort: ImageVector by lazy {
    autoMirroredIcon("AutoMirrored.Filled.Sort", "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z")
}

val Icons.AutoMirrored.Filled.ArrowForward: ImageVector by lazy {
    autoMirroredIcon("AutoMirrored.Filled.ArrowForward", "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z")
}
