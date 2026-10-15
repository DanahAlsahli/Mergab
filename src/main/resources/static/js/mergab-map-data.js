/* Mergab – Shared live map adapter for the existing MirqabMap engine.
   Include /map/mirqab-map.js before this file.
   Usage: const controller = MergabMap.mount('#map', { onSelect(info, zone) {} });
*/
(function (global) {
    'use strict';

    async function getList(path) {
        const response = await fetch('/api/v1' + path, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
            cache: 'no-store'
        });
        if (!response.ok) throw new Error('Map API ' + response.status + ': ' + path);
        const data = await response.json();
        if (Array.isArray(data)) return data;
        if (Array.isArray(data?.data)) return data.data;
        if (Array.isArray(data?.content)) return data.content;
        throw new Error('Map API response is not an array: ' + path);
    }

    function isMappable(zone) {
        if (zone?.id == null || zone.mapX == null || zone.mapY == null) return false;
        const x = Number(zone.mapX);
        const y = Number(zone.mapY);
        return Number.isFinite(x) && Number.isFinite(y) &&
            x >= 0 && x <= 100 && y >= 0 && y <= 100;
    }

    function toMarkers(zones) {
        return zones.filter(isMappable).sort((a, b) => Number(a.id) - Number(b.id)).map(zone => ({
            key: String(zone.id),
            id: zone.id,
            name: { ar: String(zone.name || ''), en: String(zone.name || '') },
            x: Number(zone.mapX),
            y: Number(zone.mapY)
        }));
    }

    function toEdges(connections, markers) {
        const ids = new Set(markers.map(zone => zone.key));
        const seen = new Set();
        const edges = [];
        for (const connection of connections) {
            const from = connection?.fromZone?.id;
            const to = connection?.toZone?.id;
            if (from == null || to == null) continue;
            const a = String(from);
            const b = String(to);
            if (a === b || !ids.has(a) || !ids.has(b)) continue;
            const key = [a, b].sort().join(':');
            if (seen.has(key)) continue;
            seen.add(key);
            edges.push([a, b]);
        }
        return edges.sort((a, b) => a.join(':').localeCompare(b.join(':')));
    }

    function mount(target, options = {}) {
        const element = typeof target === 'string' ? document.querySelector(target) : target;
        if (!element) throw new Error('Map container not found');
        if (!global.MirqabMap) throw new Error('Load /map/mirqab-map.js before /js/mergab-map-data.js');

        let instance = null;
        let layoutSignature = null;
        let selectedKey = null;
        let stopped = false;
        let busy = false;
        const refreshMs = options.refreshMs === undefined ? 30000 : options.refreshMs;
        const imageSrc = options.imageSrc || '/img/map/expo-map-concept.jpg';

        function showMessage(message, showImage = false) {
            element.textContent = '';
            const paragraph = document.createElement('p');
            paragraph.style.cssText = 'padding:24px;text-align:center;color:#43564d;';
            paragraph.textContent = message;
            if (showImage) {
                const background = document.createElement('img');
                background.src = imageSrc;
                background.alt = 'خريطة إكسبو';
                background.style.cssText = 'display:block;width:100%;max-height:450px;object-fit:contain;';
                element.appendChild(background);
            }
            element.appendChild(paragraph);
        }

        async function refresh() {
            if (stopped || busy) return;
            busy = true;
            try {
                const zones = await getList('/zone/get');

                let connections = [];

                try {
                    connections = await getList('/zone-connection/get');
                } catch (error) {
                    console.warn('Unable to load zone connections:', error);
                }
                if (stopped) return;

                const markers = toMarkers(zones);
                const edges = toEdges(connections, markers);
                const currentLang = options.lang || (document.documentElement.lang === 'en' ? 'en' : 'ar');
                const signature = JSON.stringify({ markers, edges, currentLang });

                if (signature !== layoutSignature) {
                    if (instance) {
                        instance.destroy();
                        instance = null;
                    }
                    layoutSignature = signature;
                    if (markers.length === 0) {
                        showMessage(options.emptyMessage || 'لا توجد مناطق محددة على الخريطة حاليًا.', true);
                    } else {
                        const start = markers.some(z => z.key === selectedKey)
                            ? selectedKey : markers[0].key;
                        instance = global.MirqabMap.create(element, {
                            mode: 'image',
                            lang: currentLang,
                            demo: false,
                            matchBy: 'id',
                            imageFit: 'contain',
                            controls: options.controls !== false,
                            start,
                            imageLayout: { src: imageSrc, zones: markers, edges },
                            onSelect: info => {
                                selectedKey = info.key;
                                if (typeof options.onSelect === 'function') {
                                    options.onSelect(info, zones.find(z => String(z.id) === String(info.id)));
                                }
                            }
                        });
                    }
                }

                if (instance) instance.setOccupancy(zones);
                if (typeof options.onData === 'function') options.onData(zones, connections, instance);
            } catch (error) {
                console.error('Mergab map:', error);
                if (!instance && layoutSignature == null) {
                    showMessage(
                        options.errorMessage || 'الخريطة غير متاحة مؤقتًا، يرجى المحاولة لاحقًا.',
                        true
                    );
                }
                if (typeof options.onError === 'function') options.onError(error);
            } finally {
                busy = false;
            }
        }

        const timer = refreshMs > 0 ? setInterval(refresh, refreshMs) : null;
        refresh();
        return {
            refresh,
            getMap: () => instance,
            destroy() {
                stopped = true;
                if (timer) clearInterval(timer);
                if (instance) instance.destroy();
                instance = null;
            }
        };
    }

    global.MergabMap = { mount, toMarkers, toEdges };
})(window);
