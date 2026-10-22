<!-- Snapshot of Tinnitus_obsidian/wiki/app/red-flags.md, copied 2026-10-22 so this folder stands alone. [[links]] point into that wiki. -->

---
title: "Medical Red Flags & Safety"
type: app
aliases: ["Red Flags", "의료적 위험 신호"]
tags: [app/safety]
evidence: n/a
status: stable
sources: ["[[wiki/sources/bauer-2018]]"]
last_reviewed: 2026-04-28
---

# Medical Red Flags & Safety

## TL;DR
The app is a wellness and supportive tool, not a replacement for medical diagnosis or acute intervention. Certain symptoms indicate immediate medical evaluation by an ENT (otolaryngologist) or neurologist is necessary. 

## The Rules
Based on the clinical guidelines from the American Academy of Otolaryngology (as summarized in NEJM 2018), any feature capturing user health status must screen for these symptoms. If any of these are present, the app must generate an **urgent escalation prompt** advising the user to see a doctor. This prompt must never be softened.

### 1. Unilateral Tinnitus (Localized to one ear)
Tinnitus only in one ear can be an early sign of an acoustic neuroma (vestibular schwannoma) or other retrocochlear lesion. Normal subjective tinnitus is usually bilateral or perceived generally in the head.

### 2. Pulsatile Tinnitus
A rhythmic sound synchronous with the user's heartbeat is an objective tinnitus subtype often caused by vascular abnormalities (arteriovenous fistulas, glomus tumors, carotid stenosis). It requires diagnostic imaging.

### 3. Associated with Vertigo or Imbalance
If the onset of tinnitus is accompanied by severe dizziness, vertigo, or loss of balance, it strongly suggests inner ear pathology like Meniere's disease or vestibular neuritis, rather than simple subjective tinnitus.

### 4. Sudden or Asymmetric Hearing Loss
Sudden sensorineural hearing loss is an otologic emergency requiring immediate corticosteroid treatment within days to save hearing. If the patient reports a sudden drop in hearing alongside the tinnitus, they must go to the ER or an ENT immediately.

### 5. Focal Neurological Abnormalities
Any accompanying symptoms like facial numbness, weakness, severe headaches, or difficulty speaking indicate a neurological etiology. 

## App UX Implementation
During the onboarding assessment (when we collect the user's Tinnitus Handicap Inventory scores), include explicit screening questions for these red flags. If a flag is raised, the user flow halts and provides a clear recommendation to seek a medical evaluation before proceeding to use the app for long-term symptom management. 
Additionally, track severe depression or suicidal ideation answers on the PHQ-9 (e.g. THI > 38 / high distress combined with hopelessness) and offer immediate crisis lifeline numbers.
