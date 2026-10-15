/* Mergab — Visitor Interactive Map */

(() => {
    'use strict';

    const $ = id => document.getElementById(id);

    const escapeHtml = value =>
        String(value ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        })[c]);

    const number = value =>
        Number(value || 0).toLocaleString('en-US');

    const i18n = {
        ar: {
            eyebrow: 'استكشف إكسبو بذكاء',
            title: 'الخريطة التفاعلية',
            subtitle: 'تعرّف على المناطق، شاهد الازدحام، وخطّط مسارك.',
            mapNav: 'الخريطة',
            planNav: 'خطط مسارك',
            crowdNav: 'الازدحام',
            suggestNav: 'اقتراحات',
            refreshNotice: 'تحديث تلقائي كل 30 ثانية',
            visitors: 'عدد الزوار المسجل',
            capacity: 'السعة',
            eventsToday: 'فعاليات اليوم',
            showRoute: 'اعرض المسار',
            planTitle: 'خطّط زيارتك',
            planDesc: 'اختر نقطة البداية والمناطق اللي ودّك تزورها. المسارات تقديرية وبحسب الاتصالات المسجلة.',
            chooseZones: 'المناطق اللي تبغى تزورها',
            startZone: 'نقطة البداية',
            avoidCrowd: 'فضّل المسارات الأقل ازدحامًا',
            planButton: 'أنشئ مساري',
            routeResult: 'المسار المقترح',
            crowdTitle: 'حالة الازدحام',
            sugTitle: 'اقتراحات لزيارتك',
            sugDesc: 'اقتراحات حسب إشغال المناطق والفعاليات المسجلة.',
            loading: 'جاري تحميل بيانات الخريطة...',
            loaded: 'الخريطة متصلة بالبيانات الحالية',
            empty: 'لا توجد مناطق مضافة حتى الآن.',
            notMapped: 'لم يحدد موقعها بعد',
            loadError: 'الخريطة غير متاحة مؤقتًا، يرجى المحاولة لاحقًا.',
            emptyRoute: 'اختاري منطقة واحدة على الأقل لإنشاء المسار.',
            disconnected: 'لا يوجد مسار مسجل يربط المناطق المختارة.',
            sameStart: 'أنتِ بالفعل عند المنطقة المختارة.',
            noZonesRoute: 'لا توجد مناطق بإحداثيات صالحة لتخطيط مسار.',
            routeHint: 'مدة المشي تقريبية، وليست ملاحة ميدانية دقيقة.',
            walk: 'المشي',
            minutes: 'دقيقة',
            showOnMap: 'عرض بالخريطة',
            high: 'مكتظ',
            busy: 'مزدحم',
            medium: 'متوسط',
            low: 'هادئ',
            missing: 'لا توجد مناطق متاحة حاليًا.',
            suggestionQuiet: 'منطقة أقل ازدحامًا',
            suggestionEvent: 'فعالية قادمة',
            suggestionsEmpty: 'لا توجد اقتراحات حاليًا.',
            noConnections: 'لا يوجد مسار متصل من نقطة البداية إلى هذه المنطقة.',
            noStart: 'اختاري نقطة بداية من القائمة.'
        },
        en: {
            eyebrow: 'Explore Expo smarter',
            title: 'Interactive map',
            subtitle: 'Explore zones, check crowds and plan your route.',
            mapNav: 'Map',
            planNav: 'Plan route',
            crowdNav: 'Crowds',
            suggestNav: 'Suggestions',
            refreshNotice: 'Updates automatically every 30 seconds',
            visitors: 'Recorded visitors',
            capacity: 'Capacity',
            eventsToday: 'Events today',
            showRoute: 'Show route',
            planTitle: 'Plan your visit',
            planDesc: 'Choose where to start and which zones to visit. Routes are estimates based on recorded connections.',
            chooseZones: 'Zones to visit',
            startZone: 'Starting zone',
            avoidCrowd: 'Prefer less-crowded routes',
            planButton: 'Build my route',
            routeResult: 'Suggested route',
            crowdTitle: 'Crowd levels',
            sugTitle: 'Suggestions for you',
            sugDesc: 'Based on recorded occupancy and events.',
            loading: 'Loading map data...',
            loaded: 'Map connected to current data',
            empty: 'No zones have been added yet.',
            notMapped: 'Location not set yet',
            loadError: 'The map is temporarily unavailable. Please try again later.',            emptyRoute: 'Choose at least one destination.',
            disconnected: 'No recorded route connects the selected zones.',
            sameStart: 'You are already at the selected zone.',
            noZonesRoute: 'No zones have valid map coordinates yet.',
            routeHint: 'Walking times are approximate, not turn-by-turn navigation.',
            walk: 'Walk',
            minutes: 'min',
            showOnMap: 'Show on map',
            high: 'Critical',
            busy: 'Busy',
            medium: 'Moderate',
            low: 'Quiet',
            missing: 'No zones are currently available.',
            suggestionQuiet: 'Less-crowded zone',
            suggestionEvent: 'Upcoming event',
            suggestionsEmpty: 'No suggestions yet.',
            noConnections: 'No connected route from the starting zone.',
            noStart: 'Choose a starting zone.'
        }
    };

    const lang = () =>
        window.Mergab?.getLanguage?.() === 'en' ? 'en' : 'ar';

    const t = key => i18n[lang()][key] || key;

    const zoneId = z => String(z?.id ?? '');

    const mappable = z =>
        z?.id != null &&
        z.mapX != null &&
        z.mapY != null &&
        Number.isFinite(Number(z.mapX)) &&
        Number.isFinite(Number(z.mapY)) &&
        Number(z.mapX) >= 0 &&
        Number(z.mapX) <= 100 &&
        Number(z.mapY) >= 0 &&
        Number(z.mapY) <= 100;

    const percent = z =>
        Math.round(
            (Number(z.initialVisitors) || 0) /
            (Number(z.capacity) || 1) * 100
        );

    const crowdType = z => {
        const p = percent(z);

        if (p >= 90) return 'critical';
        if (p >= 75) return 'high';
        if (p >= 50) return 'medium';

        return 'low';
    };

    const crowdName = z => ({
        critical: t('high'),
        high: t('busy'),
        medium: t('medium'),
        low: t('low')
    })[crowdType(z)];

    const crowdColor = z => ({
        critical: '#C0392B',
        high: '#E8964A',
        medium: '#F2D27A',
        low: '#7FC49B'
    })[crowdType(z)];

    let ZONES = [];
    let EVENTS = [];
    let selectedId = null;
    let controller = null;
    let picked = new Set();
    let didOpenQuery = false;

    const byId = id =>
        ZONES.find(z => zoneId(z) === String(id));

    const usableZones = () => ZONES.filter(mappable);

    const currentMap = () => controller?.getMap() || null;

    const message = (id, content) => {
        $(id).innerHTML =
            `<p class="map-empty">${escapeHtml(content)}</p>`;
    };

    function today(value) {
        const date = new Date(value);

        return Number.isFinite(date.getTime()) &&
            date.toDateString() === new Date().toDateString();
    }

    function eventCount(zone) {
        return EVENTS.filter(event =>
            event.zone &&
            zoneId(event.zone) === zoneId(zone) &&
            today(event.startDateTime)
        ).length;
    }

    /* ---------------- Translation ---------------- */

    function translatePage() {
        document.querySelectorAll('[data-i]').forEach(element => {
            element.textContent = t(element.dataset.i);
        });
    }

    /* ---------------- Zone List ---------------- */

    function renderLayers() {
        if (!ZONES.length) {
            message('layers', t('empty'));
            return;
        }

        $('layers').innerHTML =
            `<h2>${escapeHtml(t('mapNav'))}</h2>` +

            ZONES.map(zone => {
                const canShow = mappable(zone);

                return `
                    <button
                        type="button"
                        data-zone="${escapeHtml(zone.id)}"
                        ${canShow ? '' : 'disabled'}
                        aria-pressed="${selectedId === zoneId(zone)}">

                        <span class="map-dot"
                              style="--c:${crowdColor(zone)}"></span>

                        <span class="map-layer-name">
                            ${escapeHtml(zone.name)}

                            ${canShow ? '' : `
                                <small class="map-layer-note">
                                    ${escapeHtml(t('notMapped'))}
                                </small>
                            `}
                        </span>
                    </button>
                `;
            }).join('');
    }

    /* ---------------- Crowd Levels ---------------- */

    function renderCrowd() {
        if (!ZONES.length) {
            message('crowd', t('missing'));
            return;
        }

        const sorted = ZONES.slice().sort(
            (a, b) => percent(b) - percent(a)
        );

        $('crowd').innerHTML = sorted.map(zone => `
            <button
                type="button"
                class="crowd-row"
                data-zone="${escapeHtml(zone.id)}"
                ${mappable(zone) ? '' : 'disabled'}>

                <span class="ic" style="background:#F0F5EF">
                    <span class="map-dot"
                          style="--c:${crowdColor(zone)}"></span>
                </span>

                <b>${escapeHtml(zone.name)}</b>

                <span class="meter">
                    <i style="
                        width:${Math.min(100, Math.max(0, percent(zone)))}%;
                        background:${crowdColor(zone)};
                    "></i>
                </span>

                <span class="pct" dir="ltr">
                    ${percent(zone)}%
                </span>
            </button>
        `).join('');
    }

    /* ---------------- Selected Zone ---------------- */

    function renderSelected() {
        const zone = selectedId ? byId(selectedId) : null;
        const card = $('selCard');

        if (!zone || !mappable(zone)) {
            card.hidden = true;
            selectedId = null;
            return;
        }

        card.hidden = false;

        $('sName').textContent = zone.name;

        $('sBadge').className =
            'map-badge ' + crowdType(zone);

        $('sBadge').textContent =
            crowdName(zone) + ' · ' + percent(zone) + '%';

        $('sCount').textContent = number(zone.initialVisitors);
        $('sCap').textContent = number(zone.capacity);
        $('sEvents').textContent = number(eventCount(zone));

        renderLayers();
    }

    /* ---------------- Route Pickers ---------------- */

    function renderPickers() {
        const valid = usableZones();
        const oldStart = $('startSel').value;

        const ids = new Set(valid.map(zoneId));

        picked = new Set(
            [...picked].filter(id => ids.has(id))
        );

        $('pickZones').innerHTML = valid.length
            ? valid.map(zone => `
                <button
                    type="button"
                    class="chip"
                    data-pick="${escapeHtml(zone.id)}"
                    aria-pressed="${picked.has(zoneId(zone))}">

                    ${escapeHtml(zone.name)}
                </button>
            `).join('')

            : `<p class="map-empty">
                   ${escapeHtml(t('noZonesRoute'))}
               </p>`;

        $('startSel').innerHTML = valid.map(zone => `
            <option value="${escapeHtml(zone.id)}">
                ${escapeHtml(zone.name)}
            </option>
        `).join('');

        if (ids.has(oldStart)) {
            $('startSel').value = oldStart;
        }

        if (currentMap() && $('startSel').value) {
            currentMap().setStart($('startSel').value);
        }
    }

    /* ---------------- Suggestions ---------------- */

    function renderSuggestions() {
        const suggestions = [];

        const quiet = usableZones()
            .slice()
            .sort((a, b) => percent(a) - percent(b))[0];

        if (quiet) {
            suggestions.push({
                zone: quiet,
                title: t('suggestionQuiet'),
                description:
                    quiet.name + ' · ' + crowdName(quiet)
            });
        }

        const now = Date.now();

        const upcoming = EVENTS
            .filter(event => {
                const date = new Date(event.startDateTime).getTime();

                return date >= now &&
                    date <= now + 4 * 60 * 60 * 1000;
            })
            .sort(
                (a, b) =>
                    new Date(a.startDateTime) -
                    new Date(b.startDateTime)
            )
            .find(event =>
                event.zone &&
                usableZones().some(zone =>
                    zoneId(zone) === zoneId(event.zone)
                )
            );

        if (upcoming) {
            suggestions.push({
                zone: byId(upcoming.zone.id),
                title: t('suggestionEvent'),
                description:
                    upcoming.name + ' · ' + upcoming.zone.name
            });
        }

        $('sugList').innerHTML = suggestions.length
            ? suggestions.map(item => `
                <button
                    type="button"
                    class="map-suggestion"
                    data-zone="${escapeHtml(item.zone.id)}">

                    <strong>${escapeHtml(item.title)}</strong>
                    <small>${escapeHtml(item.description)}</small>
                </button>
            `).join('')

            : `<p class="map-empty">
                   ${escapeHtml(t('suggestionsEmpty'))}
               </p>`;
    }

    /* ---------------- Route Drawing ---------------- */

    function drawRoute(targetId, startId) {
        const map = currentMap();

        if (!map) return null;

        const from = startId || $('startSel').value;

        if (!from) return null;

        map.setStart(String(from));
        map.select(String(targetId));

        return map.showRoute(String(targetId), {
            from: String(from),
            compare: false
        });
    }

    function showRouteToSelected() {
        if (!selectedId) return;

        const route = drawRoute(selectedId);

        $('routeNote').hidden = false;

        $('routeNote').textContent = route
            ? `${t('walk')}: ${number(route.best.meters)} m · ${number(route.best.minutes)} ${t('minutes')}. ${t('routeHint')}`
            : t('noConnections');
    }

    /* ---------------- Route Planner ---------------- */

    function buildPlan(event) {
        event.preventDefault();

        const map = currentMap();
        const start = $('startSel').value;

        if (!map || !start) {
            message('planSteps', t('noStart'));
            return;
        }

        if (!picked.size) {
            message('planSteps', t('emptyRoute'));
            return;
        }

        let remaining = [...picked].filter(id => id !== start);
        let from = start;

        const steps = [];
        const avoidCrowd = $('avoidChk').checked;

        while (remaining.length) {
            const next = remaining
                .map(to => ({
                    to,
                    route: map.getRoute(to, { from })
                }))
                .filter(item => item.route)
                .sort((a, b) => {
                    const score = item => {
                        const route = avoidCrowd
                            ? item.route.best
                            : (item.route.direct || item.route.best);

                        return route.minutes;
                    };

                    return score(a) - score(b);
                })[0];

            if (!next) {
                message('planSteps', t('disconnected'));
                return;
            }

            steps.push({
                from,
                to: next.to,
                route: next.route
            });

            from = next.to;

            remaining = remaining.filter(
                id => id !== next.to
            );
        }

        if (!steps.length) {
            message('planSteps', t('sameStart'));
            return;
        }

        $('planSteps').innerHTML =
            steps.map(step => {
                const zone = byId(step.to);

                const route = avoidCrowd
                    ? step.route.best
                    : (step.route.direct || step.route.best);

                return `
                    <li class="map-route-step">
                        <strong>
                            ${escapeHtml(zone?.name || '')}
                        </strong>

                        · ${number(route.minutes)}
                        ${escapeHtml(t('minutes'))}

                        <button
                            type="button"
                            data-step-to="${escapeHtml(step.to)}"
                            data-step-from="${escapeHtml(step.from)}">

                            ${escapeHtml(t('showOnMap'))}
                        </button>
                    </li>
                `;
            }).join('');

        const note = document.createElement('p');
        note.className = 'map-empty';
        note.textContent = t('routeHint');

        document.querySelector('.map-plan-result > .map-empty')?.remove();
        $('planSteps').after(note);

        drawRoute(steps[0].to, steps[0].from);

        $('map').scrollIntoView({
            behavior: 'smooth',
            block: 'center'
        });
    }

    /* ---------------- Events API ---------------- */

    async function loadEvents() {
        try {
            const response = await fetch('/api/v1/expo-event/get', {
                headers: {
                    Accept: 'application/json'
                },
                cache: 'no-store'
            });

            if (!response.ok) {
                throw new Error('Events API ' + response.status);
            }

            const data = await response.json();

            EVENTS = Array.isArray(data)
                ? data
                : Array.isArray(data?.data)
                    ? data.data
                    : [];

        } catch (error) {
            console.error('Mergab events:', error);
        }

        renderSelected();
        renderSuggestions();
        maybeSelectQuery();
    }

    /* ---------------- Search from Homepage ---------------- */

    function maybeSelectQuery() {
        if (didOpenQuery || !currentMap() || !ZONES.length) {
            return;
        }

        const params = new URLSearchParams(
            window.location.search
        );

        const query =
            params.get('zone') || params.get('q');

        if (!query) {
            didOpenQuery = true;
            return;
        }

        const term = query.trim().toLowerCase();

        const matched = ZONES.find(zone =>
            mappable(zone) &&
            (
                String(zone.name).toLowerCase().includes(term) ||
                String(zone.id) === term
            )
        ) || (
            EVENTS.find(event =>
                String(event.name).toLowerCase().includes(term)
            ) || {}
        ).zone;

        if (matched && mappable(matched)) {
            currentMap().select(String(matched.id));

            $('map').scrollIntoView({
                behavior: 'smooth',
                block: 'center'
            });

            didOpenQuery = true;
        }
    }

    /* ---------------- Live Map Data ---------------- */

    function onData(zones) {
        ZONES = zones;

        if (selectedId && !byId(selectedId)) {
            selectedId = null;
        }

        $('mapStatus').textContent = zones.length
            ? t('loaded')
            : t('empty');

        renderLayers();
        renderCrowd();
        renderPickers();
        renderSelected();
        renderSuggestions();

        maybeSelectQuery();

        loadEvents();
    }

    /* ---------------- Click Events ---------------- */

    function bindZoneClicks(container, attribute) {
        $(container).addEventListener('click', event => {
            const button = event.target.closest(
                `[${attribute}]`
            );

            if (!button || button.disabled) return;

            const map = currentMap();

            if (!map) return;

            map.select(button.getAttribute(attribute));

            $('map').scrollIntoView({
                behavior: 'smooth',
                block: 'center'
            });
        });
    }

    /* ---------------- Initialize Map ---------------- */

    if (!window.MergabMap || !window.MirqabMap) {
        $('mapStatus').textContent = t('loadError');
        return;
    }

    translatePage();

    message('planSteps', t('emptyRoute'));

    bindZoneClicks('layers', 'data-zone');
    bindZoneClicks('crowd', 'data-zone');
    bindZoneClicks('sugList', 'data-zone');

    $('goBtn').addEventListener(
        'click',
        showRouteToSelected
    );

    $('pickZones').addEventListener('click', event => {
        const button = event.target.closest('[data-pick]');

        if (!button) return;

        const id = button.dataset.pick;

        if (picked.has(id)) {
            picked.delete(id);
        } else {
            picked.add(id);
        }

        button.setAttribute(
            'aria-pressed',
            String(picked.has(id))
        );
    });

    $('startSel').addEventListener('change', event => {
        currentMap()?.setStart(event.target.value);
    });

    $('planForm').addEventListener('submit', buildPlan);

    $('planSteps').addEventListener('click', event => {
        const button = event.target.closest('[data-step-to]');

        if (!button) return;

        drawRoute(
            button.dataset.stepTo,
            button.dataset.stepFrom
        );

        $('map').scrollIntoView({
            behavior: 'smooth',
            block: 'center'
        });
    });

    /* ---------------- Backend Integration ---------------- */

    controller = window.MergabMap.mount('#map', {
        imageSrc: '/img/map/expo-map-concept.jpg',

        refreshMs: 30000,

        onSelect(info, zone) {
            selectedId = zone
                ? zoneId(zone)
                : info?.id == null
                    ? null
                    : String(info.id);

            $('routeNote').hidden = true;

            renderSelected();
        },

        onData,

        onError(error) {
            console.warn('Map sync failed:', error);

            $('mapStatus').textContent = t('loadError');
        }
    });

    /* ---------------- Language Change ---------------- */

    document.addEventListener(
        'mergab:languageChanged',
        () => {
            translatePage();
            renderLayers();
            renderSelected();
            renderSuggestions();
            renderPickers();
            renderCrowd();

            controller?.refresh();
        }
    );

})();