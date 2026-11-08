package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser

private fun icon(name: String, pathData: String): ImageVector {
    return materialIcon(name = name) {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
}

val Icons.Filled.PowerSettingsNew: ImageVector by lazy {
    icon("Filled.PowerSettingsNew", "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83z")
}

val Icons.Filled.Terminal: ImageVector by lazy {
    icon("Filled.Terminal", "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.58-2.59L7.5 9l4 4-4 4z")
}

val Icons.Filled.Update: ImageVector by lazy {
    icon("Filled.Update", "M21 10.12h-6.78l2.74-2.74c-1.87-1.87-4.36-2.9-7.06-2.88-5.32.04-9.7 4.29-9.9 9.61C-.23 19.98 4.41 24.5 10.23 24.5c4.76 0 8.77-3.3 9.82-7.75h-2.1c-.96 3.32-4.03 5.75-7.72 5.75-4.49 0-8.15-3.57-8.23-8.06-.08-4.61 3.63-8.44 8.24-8.44 2.34 0 4.46.96 6.01 2.51l-3.07 3.07H21V10.12zM12.5 8v4.25l3.5 2.08-.72 1.21L11 13V8h1.5z")
}

val Icons.Filled.ContentCopy: ImageVector by lazy {
    icon("Filled.ContentCopy", "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z")
}

val Icons.Filled.Code: ImageVector by lazy {
    icon("Filled.Code", "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z")
}

val Icons.Filled.Error: ImageVector by lazy {
    icon("Filled.Error", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z")
}

val Icons.Filled.BugReport: ImageVector by lazy {
    icon("Filled.BugReport", "M20 8h-2.81c-.45-.78-1.07-1.45-1.82-1.96L17 4.41 15.59 3l-2.17 2.17C12.96 5.06 12.49 5 12 5c-.49 0-.96.06-1.41.17L8.41 3 7 4.41l1.62 1.63C7.88 6.55 7.26 7.22 6.81 8H4v2h2.09c-.05.33-.09.66-.09 1v1H4v2h2v1c0 .34.04.67.09 1H4v2h2.81c1.04 1.79 2.97 3 5.19 3s4.15-1.21 5.19-3H20v-2h-2.09c.05-.33.09-.66.09-1v-1h2v-2h-2v-1c0-.34-.04-.67-.09-1H20V8zm-6 8h-4v-2h4v2zm0-4h-4v-2h4v2z")
}

val Icons.Filled.Stop: ImageVector by lazy {
    icon("Filled.Stop", "M6 6h12v12H6z")
}

val Icons.Filled.SwipeDown: ImageVector by lazy {
    icon("Filled.SwipeDown", "M13 13.9v-7.1c0-.5-.4-.9-.9-.9s-.9.4-.9.9v7.1l-2.6-1.1c-.2-.1-.5-.1-.7 0-.3.2-.4.5-.4.8v.3l3.7 5.1c.3.4.8.7 1.3.7h5.1c.9 0 1.6-.7 1.6-1.6l.4-4.8c.1-.8-.5-1.5-1.3-1.6l-5.3-.9z")
}

val Icons.Filled.TravelExplore: ImageVector by lazy {
    icon("Filled.TravelExplore", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z")
}

val Icons.Filled.Cloud: ImageVector by lazy {
    icon("Filled.Cloud", "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z")
}

val Icons.Filled.Layers: ImageVector by lazy {
    icon("Filled.Layers", "M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z")
}

val Icons.Filled.Public: ImageVector by lazy {
    icon("Filled.Public", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z")
}

val Icons.Filled.VpnKey: ImageVector by lazy {
    icon("Filled.VpnKey", "M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z")
}

val Icons.Filled.Star: ImageVector by lazy {
    icon("Filled.Star", "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z")
}
