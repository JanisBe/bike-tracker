package com.biketracker.ui.map

import android.content.Context
import android.graphics.Color
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Base map source styles available across the application.
 */
enum class MapBaseSource(
    val id: String,
    val displayName: String,
    val badge: String,
    val subtitle: String
) {
    OSM_STANDARD(
        id = "osm-standard",
        displayName = "OpenStreetMap",
        badge = "Jasna",
        subtitle = "Klasyczny podkład OSM"
    ),
    OSM_DARK(
        id = "osm-dark",
        displayName = "OpenStreetMap",
        badge = "Ciemna",
        subtitle = "Kontrastowy styl ciemny"
    ),
    CYCLOSM(
        id = "cyclosm",
        displayName = "CyclOSM",
        badge = "Rowerowa",
        subtitle = "Ścieżki i pasy rowerowe, nawierzchnie"
    ),
    OPEN_TOPO_MAP(
        id = "opentopomap",
        displayName = "OpenTopoMap",
        badge = "Topo",
        subtitle = "Poziomice, rzeźba terenu, szlaki"
    ),
    UMP_PCPL(
        id = "ump",
        displayName = "UMP-pcPL",
        badge = "Polska",
        subtitle = "Siatka dróg i szlaków UMP Polska"
    );

    companion object {
        fun fromId(id: String?): MapBaseSource {
            return entries.firstOrNull { it.id == id } ?: OSM_STANDARD
        }
    }
}

/**
 * Outdoor trail overlay layers that can be rendered over any base map.
 */
enum class MapOverlaySource(
    val id: String,
    val displayName: String,
    val badge: String,
    val subtitle: String
) {
    WAYMARKED_CYCLING(
        id = "waymarked-cycling",
        displayName = "Szlaki rowerowe",
        badge = "Waymarked",
        subtitle = "Oznakowane trasy rowerowe i EV"
    ),
    WAYMARKED_HIKING(
        id = "waymarked-hiking",
        displayName = "Szlaki piesze",
        badge = "Waymarked",
        subtitle = "Kolorowe szlaki turystyczne i górskie"
    ),
    WAYMARKED_MTB(
        id = "waymarked-mtb",
        displayName = "Trasy MTB",
        badge = "Waymarked",
        subtitle = "Dedykowane trasy kolarstwa górskiego"
    );

    companion object {
        fun fromId(id: String?): MapOverlaySource? {
            return entries.firstOrNull { it.id == id }
        }
    }
}

/**
 * Custom tile sources and helper definitions for osmdroid.
 */
object MapTileSources {

    val CYCLOSM: ITileSource = XYTileSource(
        "CyclOSM",
        0, 20, 256, ".png",
        arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
        ),
        "CyclOSM | OpenStreetMap contributors"
    )

    val OPEN_TOPO_MAP: ITileSource = XYTileSource(
        "OpenTopoMap",
        0, 17, 256, ".png",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ),
        "OpenTopoMap | OpenStreetMap contributors"
    )

    val UMP_PCPL: ITileSource = XYTileSource(
        "UMP-pcPL",
        0, 18, 256, ".png",
        arrayOf(
            "https://tiles.ump.waw.pl/ump_tiles/"
        ),
        "UMP-pcPL contributors"
    )

    val WAYMARKED_CYCLING: ITileSource = XYTileSource(
        "WaymarkedCycling",
        0, 18, 256, ".png",
        arrayOf(
            "https://tile.waymarkedtrails.org/cycling/"
        ),
        "Waymarked Trails Cycling"
    )

    val WAYMARKED_HIKING: ITileSource = XYTileSource(
        "WaymarkedHiking",
        0, 18, 256, ".png",
        arrayOf(
            "https://tile.waymarkedtrails.org/hiking/"
        ),
        "Waymarked Trails Hiking"
    )

    val WAYMARKED_MTB: ITileSource = XYTileSource(
        "WaymarkedMTB",
        0, 18, 256, ".png",
        arrayOf(
            "https://tile.waymarkedtrails.org/mtb/"
        ),
        "Waymarked Trails MTB"
    )

    fun getBaseTileSource(source: MapBaseSource): ITileSource {
        return when (source) {
            MapBaseSource.OSM_DARK,
            MapBaseSource.OSM_STANDARD -> TileSourceFactory.MAPNIK

            MapBaseSource.CYCLOSM -> CYCLOSM
            MapBaseSource.OPEN_TOPO_MAP -> OPEN_TOPO_MAP
            MapBaseSource.UMP_PCPL -> UMP_PCPL
        }
    }

    fun getOverlayTileSource(source: MapOverlaySource): ITileSource {
        return when (source) {
            MapOverlaySource.WAYMARKED_CYCLING -> WAYMARKED_CYCLING
            MapOverlaySource.WAYMARKED_HIKING -> WAYMARKED_HIKING
            MapOverlaySource.WAYMARKED_MTB -> WAYMARKED_MTB
        }
    }
}

/**
 * Dedicated overlay wrapper for Waymarked Trails tile overlays in osmdroid.
 */
class TrailTilesOverlay(
    context: Context,
    val source: MapOverlaySource
) : TilesOverlay(
    MapTileProviderBasic(context, MapTileSources.getOverlayTileSource(source)),
    context
) {
    init {
        loadingBackgroundColor = Color.TRANSPARENT
        loadingLineColor = Color.TRANSPARENT
    }
}

/**
 * Applies the selected base map tile source, color filter and trail overlays to a MapView.
 */
fun applyMapConfiguration(
    mapView: MapView,
    baseSource: MapBaseSource,
    activeOverlays: Set<MapOverlaySource>
) {
    val ctx = mapView.context

    // 1. Base tile source
    val targetTileSource = MapTileSources.getBaseTileSource(baseSource)
    if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
        mapView.setTileSource(targetTileSource)
    }

    // 2. Dark mode color filter (only applied to OSM_DARK)
    if (baseSource == MapBaseSource.OSM_DARK) {
        mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    } else {
        mapView.overlayManager.tilesOverlay.setColorFilter(null)
    }

    // 3. Trail tile overlays
    val currentTrailOverlays = mapView.overlays.filterIsInstance<TrailTilesOverlay>()
    val currentOverlaySources = currentTrailOverlays.map { it.source }.toSet()

    if (currentOverlaySources != activeOverlays) {
        mapView.overlays.removeAll(currentTrailOverlays)

        // Insert trail overlays at the beginning so polylines and markers render above them
        activeOverlays.forEachIndexed { index, overlaySource ->
            val overlay = TrailTilesOverlay(ctx, overlaySource)
            mapView.overlays.add(index, overlay)
        }
        mapView.invalidate()
    }
}
