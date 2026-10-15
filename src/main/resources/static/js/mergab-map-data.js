
/* ==========================================
   MERGAB | SHARED MAP DATA
   Zones, Boundaries, Routes & Directions
   ========================================== */

(function (global) {
    "use strict";

    const SVG_NS = "http://www.w3.org/2000/svg";

    const MAP_IMAGE =
        "/img/map/expo-map-concept.jpg";

    // ======================================
    // FETCH DATABASE DATA
    // ======================================

    async function getList(path) {

        const response = await fetch("/api/v1" + path, {
            headers: {
                Accept: "application/json"
            },
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(
                `Map API ${response.status}: ${path}`
            );
        }

        const data = await response.json();

        if (Array.isArray(data)) return data;

        if (Array.isArray(data?.data)) {
            return data.data;
        }

        if (Array.isArray(data?.content)) {
            return data.content;
        }

        throw new Error(
            "Invalid map response: " + path
        );
    }

    // ======================================
    // MAP COORDINATES
    // ======================================

    function validPosition(zone) {

        const x = Number(zone?.mapX);
        const y = Number(zone?.mapY);

        return (
            zone?.id != null &&
            zone.mapX != null &&
            zone.mapY != null &&
            Number.isFinite(x) &&
            Number.isFinite(y) &&
            x >= 0 &&
            x <= 100 &&
            y >= 0 &&
            y <= 100
        );
    }

    function toMarkers(zones) {

        return zones
            .filter(validPosition)
            .sort((a, b) =>
                Number(a.id) - Number(b.id)
            )
            .map(zone => ({
                key: String(zone.id),
                id: zone.id,

                name: {
                    ar: String(zone.name || ""),
                    en: String(zone.name || "")
                },

                x: Number(zone.mapX),
                y: Number(zone.mapY)
            }));
    }

    function zoneId(value) {
        return value?.id ?? value ?? null;
    }

    // Map-engine graph connections
    function toEdges(connections, markers) {

        const ids = new Set(
            markers.map(marker => marker.key)
        );

        const seen = new Set();
        const edges = [];

        for (const connection of connections) {

            const from = zoneId(
                connection?.fromZone ??
                connection?.fromZoneId
            );

            const to = zoneId(
                connection?.toZone ??
                connection?.toZoneId
            );

            if (from == null || to == null) {
                continue;
            }

            const a = String(from);
            const b = String(to);

            if (
                a === b ||
                !ids.has(a) ||
                !ids.has(b)
            ) {
                continue;
            }

            const key = [a, b]
                .sort()
                .join(":");

            if (seen.has(key)) continue;

            seen.add(key);
            edges.push([a, b]);
        }

        return edges;
    }

    // ======================================
    // SAVED POINTS
    // ======================================

    function parsePoints(value, minimum = 2) {

        try {

            const points =
                typeof value === "string"
                    ? JSON.parse(value)
                    : value;

            if (
                !Array.isArray(points) ||
                points.length < minimum
            ) {
                return [];
            }

            const valid = points.every(point =>
                Array.isArray(point) &&
                point.length === 2 &&
                point.every(number =>
                    typeof number === "number" &&
                    Number.isFinite(number) &&
                    number >= 0 &&
                    number <= 100
                )
            );

            return valid ? points : [];

        } catch {
            return [];
        }
    }

    // ======================================
    // SVG HELPERS
    // ======================================

    function svgElement(parent, tag, attributes) {

        const element =
            document.createElementNS(
                SVG_NS,
                tag
            );

        Object.entries(attributes).forEach(
            ([key, value]) => {
                element.setAttribute(
                    key,
                    String(value)
                );
            }
        );

        parent.appendChild(element);

        return element;
    }

    function distanceSquared(point, zone) {

        return (
            (point[0] - Number(zone.mapX)) ** 2 +
            (point[1] - Number(zone.mapY)) ** 2
        );
    }

    // Keep arrows pointing from fromZone to toZone
    // even if the saved drawing was reversed.

    function orientPoints(
        points,
        connection,
        zoneById
    ) {

        const fromId = zoneId(
            connection.fromZone ??
            connection.fromZoneId
        );

        const toId = zoneId(
            connection.toZone ??
            connection.toZoneId
        );

        const from = zoneById.get(
            String(fromId)
        );

        const to = zoneById.get(
            String(toId)
        );

        if (
            !from ||
            !to ||
            !validPosition(from) ||
            !validPosition(to)
        ) {
            return points;
        }

        const first = points[0];
        const last = points[points.length - 1];

        const forward =
            distanceSquared(first, from) +
            distanceSquared(last, to);

        const backward =
            distanceSquared(first, to) +
            distanceSquared(last, from);

        return backward < forward
            ? [...points].reverse()
            : points;
    }

    // ======================================
    // SHARED SAVED-GEOMETRY OVERLAY
    // ======================================

    function drawSavedGeometry(
        target,
        zones = [],
        connections = []
    ) {

        const root =
            typeof target === "string"
                ? document.querySelector(target)
                : target;

        const svg = root?.querySelector(
            ".mqm-isvg"
        );

        if (!svg) return false;

        // Remove previously drawn saved geometry.
        svg.querySelector(
            "[data-mergab-saved-geometry]"
        )?.remove();

        const layer = svgElement(
            svg,
            "g",
            {
                "data-mergab-saved-geometry": "true",
                "pointer-events": "none"
            }
        );

        // Keep saved geometry beneath interactive routes.
        svg.prepend(layer);

        // ==================================
        // ZONE BOUNDARIES
        // ==================================

        for (const zone of zones) {

            const points = parsePoints(
                zone.boundaryPoints,
                3
            );

            if (!points.length) continue;

            svgElement(layer, "polygon", {

                points: points
                    .map(point => point.join(","))
                    .join(" "),

                fill: "#247C59",
                "fill-opacity": 0.18,

                stroke: "#247C59",
                "stroke-opacity": 0.85,
                "stroke-width": 0.5,
                "stroke-linejoin": "round"
            });
        }

        // ==================================
        // SAVED ROUTES
        // ==================================

        const zoneById = new Map(
            zones.map(zone => [
                String(zone.id),
                zone
            ])
        );

        for (const connection of connections) {

            const rawPoints = parsePoints(
                connection.pathPoints,
                2
            );

            // Do not invent a route when no
            // drawing was saved in the database.
            if (!rawPoints.length) continue;

            const points = orientPoints(
                rawPoints,
                connection,
                zoneById
            );

            const pathData = points
                .map((point, index) => {
                    const command =
                        index === 0 ? "M" : "L";

                    return (
                        `${command} ` +
                        `${point[0]} ${point[1]}`
                    );
                })
                .join(" ");

            // White outline
            svgElement(layer, "path", {

                d: pathData,

                fill: "none",
                stroke: "#FFFFFF",
                "stroke-opacity": 0.95,
                "stroke-width": 1.25,

                "stroke-linecap": "round",
                "stroke-linejoin": "round"
            });

            // Gold route
            const routePath = svgElement(
                layer,
                "path",
                {
                    d: pathData,

                    fill: "none",
                    stroke: "#B6863C",
                    "stroke-width": 0.72,

                    "stroke-linecap": "round",
                    "stroke-linejoin": "round"
                }
            );

            // ==============================
            // DIRECTION ARROWS
            // ==============================

            const length =
                routePath.getTotalLength();

            if (
                !Number.isFinite(length) ||
                length < 2
            ) {
                continue;
            }

            const positions =
                length >= 15
                    ? [0.35, 0.70]
                    : [0.50];

            for (const position of positions) {

                const distance =
                    length * position;

                const center =
                    routePath.getPointAtLength(
                        distance
                    );

                const before =
                    routePath.getPointAtLength(
                        Math.max(
                            0,
                            distance - 0.5
                        )
                    );

                const after =
                    routePath.getPointAtLength(
                        Math.min(
                            length,
                            distance + 0.5
                        )
                    );

                const angle =
                    Math.atan2(
                        after.y - before.y,
                        after.x - before.x
                    ) * 180 / Math.PI;

                const arrow = svgElement(
                    layer,
                    "polygon",
                    {
                        points:
                            "0,0 -1.25,0.56 -1.25,-0.56",

                        fill: "#B6863C",
                        stroke: "#FFFFFF",
                        "stroke-width": 0.16,

                        "stroke-linejoin": "round"
                    }
                );

                arrow.setAttribute(
                    "transform",
                    `translate(${center.x},${center.y}) rotate(${angle})`
                );
            }
        }

        return true;
    }

    // ======================================
    // SHARED LIVE MAP
    // ======================================

    function mount(target, options = {}) {

        const element =
            typeof target === "string"
                ? document.querySelector(target)
                : target;

        if (!element) {
            throw new Error(
                "Map container not found"
            );
        }

        if (!global.MirqabMap) {
            throw new Error(
                "Load mirqab-map.js first"
            );
        }

        const imageSrc =
            options.imageSrc || MAP_IMAGE;

        const refreshMs =
            options.refreshMs === undefined
                ? 30000
                : options.refreshMs;

        let instance = null;
        let layoutSignature = null;
        let selectedKey = null;

        let stopped = false;
        let busy = false;

        let savedConnections = [];

        function showMessage(message) {

            element.replaceChildren();

            const image =
                document.createElement("img");

            image.src = imageSrc;
            image.alt = "خريطة إكسبو";

            image.style.cssText =
                "display:block;width:100%;" +
                "max-height:450px;" +
                "object-fit:contain;";

            const paragraph =
                document.createElement("p");

            paragraph.style.cssText =
                "padding:15px;" +
                "text-align:center;" +
                "color:#43564d;";

            paragraph.textContent = message;

            element.append(
                image,
                paragraph
            );
        }

        // ==================================
        // LOAD LATEST DATABASE STATE
        // ==================================

        async function refresh() {

            if (stopped || busy) return;

            busy = true;

            try {

                const zones = await getList(
                    "/zone/get"
                );

                try {

                    savedConnections =
                        await getList(
                            "/zone-connection/get"
                        );

                } catch (error) {

                    console.warn(
                        "Unable to refresh zone connections:",
                        error
                    );
                }

                if (stopped) return;

                const markers = toMarkers(zones);

                const edges = toEdges(
                    savedConnections,
                    markers
                );

                const currentLang =
                    options.lang ||
                    (
                        document.documentElement.lang === "en"
                            ? "en"
                            : "ar"
                    );

                const signature =
                    JSON.stringify({
                        markers,
                        edges,
                        currentLang
                    });

                // Rebuild only when the zone layout
                // or connection graph changes.

                if (signature !== layoutSignature) {

                    if (instance) {
                        instance.destroy();
                    }

                    instance = null;
                    layoutSignature = signature;

                    if (!markers.length) {

                        showMessage(
                            options.emptyMessage ||
                            "لا توجد مناطق محددة على الخريطة حاليًا."
                        );

                    } else {

                        const start = markers.some(
                            marker =>
                                marker.key === selectedKey
                        )
                            ? selectedKey
                            : markers[0].key;

                        instance =
                            global.MirqabMap.create(
                                element,
                                {
                                    mode: "image",

                                    lang: currentLang,

                                    demo: false,

                                    matchBy: "id",

                                    imageFit: "contain",

                                    controls:
                                        options.controls !== false,

                                    start,

                                    imageLayout: {
                                        src: imageSrc,
                                        zones: markers,
                                        edges,
                                        hub: null,
                                        metersPerUnit: 8
                                    },

                                    onSelect(info) {

                                        selectedKey =
                                            info.key;

                                        if (
                                            typeof options.onSelect ===
                                            "function"
                                        ) {

                                            options.onSelect(
                                                info,
                                                zones.find(zone =>
                                                    String(zone.id) ===
                                                    String(info.id)
                                                )
                                            );
                                        }
                                    }
                                }
                            );
                    }
                }

                // Saved geometry gets redrawn on
                // every refresh, including updates
                // that don't change zone positions.

                if (instance) {
                    if (options.autoOccupancy !== false) {
                        instance.setOccupancy(zones);
                    }


                    drawSavedGeometry(
                        element,
                        zones,
                        savedConnections
                    );

                    if (options.hideLocation) {

                        element.querySelectorAll(
                            ".mqm-ime, .mqm-ime-label"
                        ).forEach(marker => {
                            marker.style.display =
                                "none";
                        });
                    }
                }

                if (
                    typeof options.onData ===
                    "function"
                ) {

                    options.onData(
                        zones,
                        savedConnections,
                        instance
                    );
                }

            } catch (error) {

                console.error(
                    "Mergab map:",
                    error
                );

                if (
                    !instance &&
                    layoutSignature == null
                ) {

                    showMessage(
                        options.errorMessage ||
                        "الخريطة غير متاحة مؤقتًا."
                    );
                }

                if (
                    typeof options.onError ===
                    "function"
                ) {

                    options.onError(error);
                }

            } finally {

                busy = false;
            }
        }

        // ==================================
        // AUTO REFRESH
        // ==================================

        const timer = refreshMs > 0
            ? setInterval(
                refresh,
                refreshMs
            )
            : null;

        refresh();

        return {
            refresh,

            getMap: () => instance,

            destroy() {

                stopped = true;

                if (timer) {
                    clearInterval(timer);
                }

                if (instance) {
                    instance.destroy();
                }

                instance = null;
            }
        };
    }

    // ======================================
    // PUBLIC SHARED API
    // ======================================

    global.MergabMap = {
        mount,
        getList,
        toMarkers,
        toEdges,
        parsePoints,
        drawSavedGeometry
    };

})(window);
