
/* =========================================
   MERGAB | ORGANIZER DASHBOARD
   Real Backend + Interactive Expo Map
   ========================================= */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ORGANIZER"]);
    if (!session) return;

    const $ = id => document.getElementById(id);

    let expoMap = null;

    // Account & logout
    $("organizerUserName").textContent =
        session.fullName || "حسابي";

    $("organizerWelcome").textContent =
        `مرحبًا، ${session.fullName || "منظّم مِرقاب"}`;

    $("organizerLogoutButton").addEventListener("click", () => {
        MergabAuth.logout();
    });

    async function getJSON(url, protectedApi = false) {
        const headers = {
            Accept: "application/json",
            "X-Requested-With": "XMLHttpRequest"
        };

        if (protectedApi) {
            headers.Authorization =
                MergabAuth.getAuthorizationHeader();
        }

        const response = await fetch(url, {
            headers,
            cache: "no-store"
        });

        if (response.status === 401 && protectedApi) {
            MergabAuth.logout();
            throw new Error("UNAUTHORIZED");
        }

        if (!response.ok) {
            throw new Error(`HTTP_${response.status}`);
        }

        return response.json();
    }

    function showEmpty(container, text) {
        const paragraph = document.createElement("p");
        paragraph.className = "organizer-empty";
        paragraph.textContent = text;
        container.replaceChildren(paragraph);
    }

    function occupancyColor(percent) {
        if (percent >= 90) return "#C0392B";
        if (percent >= 75) return "#E8964A";
        if (percent >= 50) return "#D9B452";
        return "#4F9A74";
    }

    // Display real occupancy statistics
    function renderOccupancy(zones, counts) {
        const container = $("zoneOccupancy");
        container.replaceChildren();

        if (!zones.length) {
            showEmpty(container, "لا توجد مناطق مضافة بعد.");
            return;
        }

        const ordered = [...zones].sort((a, b) => {
            const aCount = counts.get(String(a.id)) || 0;
            const bCount = counts.get(String(b.id)) || 0;

            return (bCount / (b.capacity || 1)) -
                (aCount / (a.capacity || 1));
        });

        ordered.forEach(zone => {
            const count = counts.get(String(zone.id)) || 0;
            const capacity = Number(zone.capacity) || 1;

            const percent = Math.round(
                (count / capacity) * 100
            );

            const row = document.createElement("div");
            row.style.padding = "9px 0";

            const heading = document.createElement("div");
            heading.style.display = "flex";
            heading.style.justifyContent = "space-between";
            heading.style.gap = "12px";
            heading.style.marginBottom = "9px";

            const name = document.createElement("strong");
            name.textContent = zone.name;
            name.style.fontSize = "12px";

            const percentage = document.createElement("span");
            percentage.textContent = `${percent}%`;
            percentage.style.fontSize = "12px";
            percentage.style.fontWeight = "700";
            percentage.style.color = occupancyColor(percent);

            heading.append(name, percentage);

            const track = document.createElement("div");
            track.style.height = "8px";
            track.style.background = "#EDF1ED";
            track.style.borderRadius = "20px";
            track.style.overflow = "hidden";

            const bar = document.createElement("div");
            bar.style.height = "100%";
            bar.style.width = `${Math.min(100, percent)}%`;
            bar.style.borderRadius = "20px";
            bar.style.background = occupancyColor(percent);

            track.appendChild(bar);
            row.append(heading, track);
            container.appendChild(row);
        });
    }


    // Read saved zone boundaries from the database
    function parseBoundary(value) {
        try {
            const points = typeof value === "string"
                ? JSON.parse(value)
                : value;

            if (!Array.isArray(points) || points.length < 3) {
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
        } catch (error) {
            return [];
        }
    }

    // Draw real saved polygons over the interactive map
    function drawZoneBoundaries(zones) {
        const container = $("organizerExpoMap");
        const svg = container.querySelector(".mqm-isvg");

        if (!svg) return;

        svg.querySelector("#dashboardBoundaryLayer")?.remove();

        const namespace = "http://www.w3.org/2000/svg";
        const layer = document.createElementNS(namespace, "g");

        layer.id = "dashboardBoundaryLayer";
        layer.setAttribute("pointer-events", "none");

        svg.prepend(layer);

        zones.forEach(zone => {
            const points = parseBoundary(zone.boundaryPoints);

            if (points.length < 3) return;

            const polygon = document.createElementNS(
                namespace,
                "polygon"
            );

            polygon.setAttribute(
                "points",
                points.map(point => point.join(",")).join(" ")
            );

            polygon.setAttribute("fill", "#247C59");
            polygon.setAttribute("fill-opacity", "0.40");
            polygon.setAttribute("stroke", "#247C59");
            polygon.setAttribute("stroke-width", "1.2");
            polygon.setAttribute("stroke-linejoin", "round");
            polygon.setAttribute(
                "vector-effect",
                "non-scaling-stroke"
            );
            polygon.setAttribute("pointer-events", "none");

            layer.appendChild(polygon);
        });
    }

    // Build the map from DB coordinates, not fixed demo zones
    function renderMap(zones, counts, liveAvailable) {
        const container = $("organizerExpoMap");

        if (expoMap) {
            expoMap.destroy();
            expoMap = null;
        }

        container.replaceChildren();

        const positionedZones = zones.filter(zone =>
            zone.mapX != null &&
            zone.mapY != null &&
            Number.isFinite(Number(zone.mapX)) &&
            Number.isFinite(Number(zone.mapY))
        );


        const mapZones = positionedZones.map(zone => ({
            key: `zone-${zone.id}`,
            id: zone.id,
            name: {
                ar: zone.name,
                en: zone.name
            },
            x: Number(zone.mapX),
            y: Number(zone.mapY)
        }));

        expoMap = MirqabMap.create("#organizerExpoMap", {
            mode: "image",
            lang: "ar",
            demo: false,
            heat: true,
            matchBy: "id",
            fields: {
                count: "liveVisitors"
            },
            imageFit: "contain",
            imageLayout: {
                src: "/img/map/expo-map-concept.jpg",
                zones: mapZones,
                hub: null,
                edges: [],
                metersPerUnit: 8
            },
            onSelect: () => {
                window.location.href = "/organizer/zones";
            }
        });

        // Organizer location is unknown; don't show "You are here"
        container.querySelectorAll(
            ".mqm-ime, .mqm-ime-label"
        ).forEach(element => {
            element.style.display = "none";
        });

        // Only display percentages when live data was retrieved
        if (liveAvailable) {
            expoMap.setOccupancy(
                positionedZones.map(zone => ({
                    ...zone,
                    liveVisitors:
                        counts.get(String(zone.id)) ?? 0
                }))
            );
        }
        // Draw the saved zone areas
        drawZoneBoundaries(zones);
    }

    // Load actual active QR check-ins for every zone
    async function loadZoneData(zones) {
        const results = await Promise.allSettled(
            zones.map(zone =>
                getJSON(
                    `/api/v1/zone-visit/occupancy/${zone.id}`
                )
            )
        );

        const allAvailable = results.every(
            result => result.status === "fulfilled"
        );

        const counts = new Map();

        results.forEach((result, index) => {
            if (result.status === "fulfilled") {
                counts.set(
                    String(zones[index].id),
                    Number(result.value) || 0
                );
            }
        });

        renderMap(zones, counts, allAvailable);

        if (allAvailable) {
            const total = [...counts.values()].reduce(
                (sum, count) => sum + count, 0
            );

            $("totalVisitors").textContent =
                total.toLocaleString("ar-SA");

            renderOccupancy(zones, counts);
        } else {
            $("totalVisitors").textContent = "—";

            showEmpty(
                $("zoneOccupancy"),
                "تعذر تحميل بيانات الإشغال الحالية."
            );
        }
    }

    function renderEvents(events) {
        const container = $("upcomingEvents");
        container.replaceChildren();

        if (!events.length) {
            showEmpty(container, "لا توجد فعاليات قادمة.");
            return;
        }

        const sorted = [...events].sort((a, b) =>
            new Date(a.startDateTime) -
            new Date(b.startDateTime)
        );

        sorted.slice(0, 5).forEach(event => {
            const row = document.createElement("article");
            row.style.padding = "12px 0";
            row.style.borderBottom = "1px solid #EDF1ED";

            const name = document.createElement("strong");
            name.textContent = event.name;
            name.style.display = "block";
            name.style.fontSize = "13px";

            const details = document.createElement("small");
            details.style.display = "block";
            details.style.marginTop = "6px";
            details.style.color = "#6B7D72";

            const date = new Date(event.startDateTime);

            const formattedDate = Number.isNaN(date.getTime())
                ? "موعد غير متاح"
                : date.toLocaleString("ar-SA", {
                    dateStyle: "medium",
                    timeStyle: "short"
                });

            details.textContent =
                `${formattedDate} · ${event.zone?.name || "بدون منطقة"}`;

            row.append(name, details);
            container.appendChild(row);
        });
    }

    async function loadDashboard() {
        const [zoneResult, eventResult, simResult] =
            await Promise.allSettled([
                getJSON("/api/v1/zone/get"),
                getJSON("/api/v1/expo-event/get-upcoming"),
                getJSON("/api/v1/simulation/get-all", true)
            ]);

        // Zones & map
        if (zoneResult.status === "fulfilled") {
            const zones = zoneResult.value;

            $("totalZones").textContent =
                zones.length.toLocaleString("ar-SA");

            await loadZoneData(zones);
        } else {
            $("totalZones").textContent = "—";
            $("totalVisitors").textContent = "—";

            renderMap([], new Map(), false);

            showEmpty(
                $("zoneOccupancy"),
                "تعذر تحميل المناطق."
            );
        }

        // Events
        if (eventResult.status === "fulfilled") {
            const events = eventResult.value;

            $("totalEvents").textContent =
                events.length.toLocaleString("ar-SA");

            renderEvents(events);
        } else {
            $("totalEvents").textContent = "—";

            showEmpty(
                $("upcomingEvents"),
                "تعذر تحميل الفعاليات."
            );
        }

        // Simulations
        if (simResult.status === "fulfilled") {
            $("totalSimulations").textContent =
                simResult.value.length.toLocaleString("ar-SA");
        } else {
            $("totalSimulations").textContent = "—";
        }
    }

    // Always display the Expo map, even with no zones
    renderMap([], new Map(), false);

    loadDashboard().catch(error => {
        console.error("Organizer dashboard failed:", error);
    });
});
