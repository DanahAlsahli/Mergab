package com.example.mergab.Service;

import com.example.mergab.DTO.EarlyWarningResponseDTO;
import com.example.mergab.Model.SimulationSnapshot;
import com.example.mergab.Model.Zone;
import com.example.mergab.Repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EarlyWarningService {

    private static final int DEFAULT_HORIZON_MINUTES = 10;

    private final SimulationSnapshotService simulationSnapshotService;
    private final ZoneRepository zoneRepository;
    private final AiPredictionService aiPredictionService;

    public EarlyWarningResponseDTO getEarlyWarnings(Integer simulationId) {

        // التحقق من وجود المحاكاة وجلب سجلاتها
        List<SimulationSnapshot> allSnapshots =
                simulationSnapshotService.getSnapshotsBySimulation(simulationId);

        List<Zone> zones = zoneRepository.findAll();

        Map<Integer, List<SimulationSnapshot>> snapshotsByZone =
                new HashMap<>();

        for (SimulationSnapshot snapshot : allSnapshots) {
            if (snapshot.getZone() != null
                    && snapshot.getZone().getId() != null) {

                snapshotsByZone
                        .computeIfAbsent(
                                snapshot.getZone().getId(),
                                key -> new ArrayList<>()
                        )
                        .add(snapshot);
            }
        }

        List<EarlyWarningResponseDTO.ZoneWarning> warnings =
                new ArrayList<>();

        int analyzedZones = 0;
        int zonesWithoutEnoughData = 0;
        int horizonMinutes = DEFAULT_HORIZON_MINUTES;

        for (Zone zone : zones) {

            Integer zoneId = zone.getId();

            List<SimulationSnapshot> zoneSnapshots =
                    snapshotsByZone.getOrDefault(
                            zoneId,
                            new ArrayList<>()
                    );

            zoneSnapshots.sort(
                    Comparator.comparing(
                            SimulationSnapshot::getMinute,
                            Comparator.nullsFirst(
                                    Comparator.naturalOrder()
                            )
                    )
            );

            // لا توجد سجلات لهذه المنطقة في المحاكاة
            if (zoneSnapshots.isEmpty()) {

                zonesWithoutEnoughData++;

                warnings.add(
                        new EarlyWarningResponseDTO.ZoneWarning(
                                zoneId,
                                zone.getName(),
                                null,
                                null,
                                zone.getCapacity(),
                                null,
                                null,
                                null,
                                "INSUFFICIENT_DATA",
                                "لا تتوفر سجلات لهذه المنطقة في المحاكاة؛ "
                                        + "يلزم توفير بيانات قبل إصدار التنبؤ."
                        )
                );

                continue;
            }

            // أحدث سجل يمثل الإشغال الحالي المسجل
            SimulationSnapshot latestSnapshot =
                    zoneSnapshots.get(zoneSnapshots.size() - 1);

            Integer currentOccupancy = latestSnapshot.getOccupancy();
            Integer currentQueueLength = latestSnapshot.getQueueLength();

            // نرسل آخر ثلاثة سجلات كحد أقصى إلى خدمة الذكاء الاصطناعي
            int startIndex = Math.max(0, zoneSnapshots.size() - 3);

            List<SimulationSnapshot> recentSnapshots =
                    new ArrayList<>(
                            zoneSnapshots.subList(
                                    startIndex,
                                    zoneSnapshots.size()
                            )
                    );

            Map<String, Object> prediction =
                    aiPredictionService.predict(
                            simulationId,
                            zoneId,
                            zone.getCapacity(),
                            recentSnapshots
                    );

            Object predictedValue =
                    prediction.get("predictedOccupancy");

            Object congestionValue =
                    prediction.get("predictedCongestionLevel");

            Object horizonValue =
                    prediction.get("horizonMinutes");

            if (!(predictedValue instanceof Number)
                    || congestionValue == null
                    || !(horizonValue instanceof Number)) {

                throw new IllegalStateException(
                        "استجابة التنبؤ غير مكتملة للمنطقة: " + zoneId
                );
            }

            int predictedOccupancy =
                    ((Number) predictedValue).intValue();

            String congestionLevel =
                    congestionValue.toString();

            horizonMinutes =
                    ((Number) horizonValue).intValue();

            Double predictedPercentage = null;

            if (zone.getCapacity() != null
                    && zone.getCapacity() > 0) {

                predictedPercentage = Math.round(
                        (predictedOccupancy * 1000.0)
                                / zone.getCapacity()
                ) / 10.0;
            }

            boolean limitedData = zoneSnapshots.size() < 2;

            String status = limitedData
                    ? "LIMITED_DATA"
                    : "PREDICTED";

            String recommendation;

            if (limitedData) {
                recommendation =
                        "البيانات محدودة؛ التنبؤ مبني على سجل واحد فقط "
                                + "ويحتاج إلى الحذر.";
            } else {
                recommendation = getRecommendation(congestionLevel);
                analyzedZones++;
            }

            warnings.add(
                    new EarlyWarningResponseDTO.ZoneWarning(
                            zoneId,
                            zone.getName(),
                            currentOccupancy,
                            currentQueueLength,
                            zone.getCapacity(),
                            predictedOccupancy,
                            predictedPercentage,
                            congestionLevel,
                            status,
                            recommendation
                    )
            );
        }

        // ترتيب المناطق حسب مستوى الخطورة
        warnings.sort(
                Comparator.comparingInt(
                        this::getRiskRank
                )
        );

        return new EarlyWarningResponseDTO(
                simulationId,
                horizonMinutes,
                zones.size(),
                analyzedZones,
                zonesWithoutEnoughData,
                warnings
        );
    }

    private String getRecommendation(String congestionLevel) {

        if (congestionLevel == null) {
            return "تعذر تحديد مستوى الازدحام؛ راجع بيانات التنبؤ.";
        }

        switch (congestionLevel.toUpperCase()) {
            case "HIGH":
                return "إنذار: يُتوقع ازدحام مرتفع؛ "
                        + "راجع تنظيم الدخول والمسارات البديلة.";

            case "MEDIUM":
                return "تنبيه: راقب الإشغال واستعد لتخفيف التدفق "
                        + "إذا استمر الارتفاع.";

            case "LOW":
                return "لا يُتوقع ازدحام مرتفع وفق البيانات الحالية؛ "
                        + "استمر في المراقبة.";

            default:
                return "راجع مستوى الازدحام والبيانات قبل اتخاذ القرار.";
        }
    }

    private int getRiskRank(
            EarlyWarningResponseDTO.ZoneWarning warning) {

        if (Objects.equals(warning.getStatus(), "INSUFFICIENT_DATA")) {
            return 4;
        }

        if (Objects.equals(warning.getStatus(), "LIMITED_DATA")) {
            return 3;
        }

        String level = warning.getCongestionLevel();

        if (level == null) {
            return 4;
        }

        switch (level.toUpperCase()) {
            case "HIGH":
                return 0;
            case "MEDIUM":
                return 1;
            case "LOW":
                return 2;
            default:
                return 3;
        }
    }
}
