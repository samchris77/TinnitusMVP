<!-- Snapshot of Tinnitus_obsidian/wiki/clinical/tmi.md, copied 2026-10-22 so this folder stands alone. [[links]] point into that wiki. -->

---
title: "Tinnitus Magnitude Index (TMI / K-TMI)"
type: assessment
aliases: ["TMI", "K-TMI", "Tinnitus Magnitude Index", "이명규모지수"]
tags: [assessment/perception, assessment/short-form, evidence/moderate]
evidence: moderate
status: stable
sources: ["[[wiki/sources/hwang-bahng-2018-ktmi]]"]
last_reviewed: 2026-05-12
---

# Tinnitus Magnitude Index (TMI / K-TMI)

## TL;DR

The TMI is a 3-item self-report instrument that measures **only the perception (자각)** of tinnitus — loudness, awareness time, perceived severity — distinct from instruments like [[wiki/clinical/thi]] and [[wiki/clinical/tfi]] that also measure reaction. Derived from the TFI development dataset (Schmidt 2014). A Korean adaptation is validated and published with item text ([[wiki/sources/hwang-bahng-2018-ktmi]]): Cronbach's α=0.852, moderate correlation with K-THI (rs=0.40–0.53). Suited to **high-frequency, low-friction logging** in the app — complementary to a monthly TFI.

## What it measures

The conceptual claim (Hwang & Bahng 2018): tinnitus assessment splits into two domains:
- **Perception (자각)** — auditory-cognitive characteristics: loudness, audibility, awareness. Measured by psychoacoustic tests OR by self-report instruments like TMI.
- **Reaction (반응)** — emotional, cognitive, functional impact. Measured by THI, TFI, etc.

TMI deliberately measures perception only. Theoretical justification: under the Cognitive-Behavioral Mediation Model + Adaptation Level Theory, perception predicts reaction via mediators (cognition, contextual features, intervention), so a perception-only score still has clinical value — and is more responsive to acoustic interventions like sound therapy.

## Items (Korean, validated)

Source: [[wiki/sources/hwang-bahng-2018-ktmi]], Appendix.

1. "지난 한 주 동안, 이명이 얼마나 크게 들렸습니까?"
   Scale: **0–10** (0 = 전혀 크지 않았다, 10 = 매우 크게 들렸다).
2. "깨어 있는 시간 중에 이명을 인지하는 시간은 몇 % 정도 되었습니까?"
   Scale: **0%–100%** in 10% steps (0% = 전혀 인지하지 못하였다, 100% = 항상 인지하였다).
3. "지난 한 주 동안, 이명이 얼마나 심각하게 느껴졌습니까?"
   Scale: **100mm VAS** (0 = 전혀 심각하지 않았다, 100 = 매우 심각하다고 느꼈다).

Recall window: "지난 한 주 동안" (over the past week).
Average completion time: ~2 minutes.

## Scoring algorithm

**Per-item:**
- Q1 score: 0–10 (use as-is, or ×10 to put on a 0–100 scale).
- Q2 score: 0–100 (percent value).
- Q3 score: 0–100 (mm measurement on the VAS line).

**Composite score on 0–100 scale (weighted, validated):**

```
TMI_score = (Q1 × 10) × 0.5856 + Q2 × 0.0423 + Q3 × 0.3721
```

Weights optimized on N=215 Korean subjects to minimize |TMI − THI| absolute error. Note: Q2 carries a very low weight (0.04) because of reliability issues (see below).

**Unweighted alternative (simple mean):** average the three 0–100-scaled items. Correlates with THI at rs=0.535 (vs rs=0.553 weighted). Acceptable approximation if the weighted formula is impractical.

## Psychometrics (Korean version)

- **Cronbach's α = 0.852** (vs original English α = 0.86).
- α if Q1 removed: 0.804. α if Q3 removed: 0.698. α if Q2 removed: **0.883** — Q2 is the least internally consistent item; consider deemphasizing in display.
- Correlation with K-THI total: rs=0.403–0.530 (per item, all p<0.001).
- Correlation strongest with THI functional subscale (rs up to 0.562), weakest with catastrophe subscale — pattern is consistent with TMI's design intent.
- **Test-retest reliability**: not reported for the Korean version (limitation; future work).
- **Severity bands / treatment threshold**: not established for K-TMI (future work).

## Comparison with related instruments

- **vs [[wiki/clinical/tfi]]** — TMI = 3-item subset focused on perception (intrusiveness domain). TFI = 25-item full coverage of perception + reaction across 8 domains. TMI is more sensitive to acoustic intervention effects; TFI is more comprehensive.
- **vs [[wiki/clinical/thi]]** — THI = 25 items, 3 response choices, perception + reaction with emphasis on handicap. THI vocabulary uses vague frequency words ("종종", "가끔") that the K-TMI paper criticizes as imprecise. TMI's numeric scales are claimed to be more measurement-friendly.
- **vs psychoacoustic loudness matching** — both purport to measure perceived loudness, but matching is a single-point lab test; TMI captures a self-reported weekly average that integrates daily fluctuation.

## App relevance

K-TMI is the natural **high-cadence companion** to the monthly TFI in the app:

- **Cadence**: daily or weekly entry in the records tab ([[wiki/app/navigation]] 기록 screen).
- **Why it fits**: short (3 items, ~2 min), validated Korean wording exists (Appendix of the source paper), no licensing barrier (academic publication; author obtained permission for translation).
- **Display**: trend line of weighted composite score over time; small-multiple of Q1 (loudness) and Q3 (severity) as the most reliable items; suppress or de-emphasize Q2 (awareness time) given its α-removal behavior.
- **Pairing with TFI**: weekly TMI for momentum tracking; monthly TFI for clinical-grade outcome measurement; both visible on the same trend chart to show that perception trends can lead reaction trends after sound-therapy initiation.

Implementation notes:
- Q3's 100mm VAS does not map cleanly to a touchscreen — adopt either (a) a continuous slider with a long bar, or (b) a 0–10 discrete fallback with a noted scale change (would slightly degrade comparability to the published validation). Decision pending UX testing.
- Use the validated weights (Q1=0.5856, Q2=0.0423, Q3=0.3721) when computing the displayed composite.

## Open questions / contested claims

- The original TMI authors and the K-TMI authors both flag Q2 as problematic. Should the app revise Q2's wording or replace it with a different perception item?
- No published cutoffs for "needs treatment" on the K-TMI. Severity bands shown in-app should be tentative (color thresholds based on quartiles of the user's own historical data, not absolute clinical bands).
- Should the Q3 VAS be retained as VAS in-app, or simplified to a 0–10 scale at the cost of strict comparability to the validation paper?

## Sources

- [[wiki/sources/hwang-bahng-2018-ktmi]] — Korean validation (primary)
