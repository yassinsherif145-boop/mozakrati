# START_HERE — "مذاكرتي" (My Studying Session) Android app
Guide for editing the app. The project is in `app/` (real files, ready to upload to GitHub). Do NOT read everything: read this guide, then open only the files the task needs (section 5 map for index.html; section 3 for Java). Current page version: **APP_VER=41** (next release = 42).

## 0a. HOW TO DELIVER (current flow) - READ FIRST
The code is in the PUBLIC GitHub repo `yassinsherif145-boop/mozakrati` (branch main). Every push to main builds the APK on GitHub Actions and publishes a Release; the app has a "تحديث التطبيق" button / banner that downloads and installs it. So the user never moves an APK by hand.
- **If your session can push** (GitHub connected, see 8b): edit, commit, push to main. Done. Then check the build (section 8b).
- **If you cannot push** (most chats): send ONLY the changed files, complete, each with its exact repo path (e.g. `app/src/main/java/com/mzk/app/Notifs.java`, `app/src/main/assets/index.html`). Tiny files, no zip of the whole project. Then tell him, in Egyptian Arabic, exactly these steps:
  1. افتح github.com/yassinsherif145-boop/mozakrati وروح للفولدر اللي فيه الملف (مثلًا app ← src ← main ← java ← com ← mzk ← app).
  2. دوس Add file ← Upload files، واسحب الملف المعدّل (لازم بنفس الاسم بالظبط).
  3. انزل دوس Commit changes (على main).
  4. استنى حوالي ٤ دقايق (تاب Actions لازم يبقى فيه علامة ✓ خضرا. لو طلع ✗ ابعتلي صورة الخطأ، أو دوس Re-run jobs لو الخطأ 429).
  5. افتح التطبيق، هيظهر كارت «فيه نسخة جديدة»، دوس «تحديث الآن» وثبّت.
  Page-only change (index.html only): same steps, or ⚙ ← 🔄 التحديثات ← اختار الملف (instant, no APK).
- Always bump `const APP_VER=N;` in index.html on every change. Ask the user at most ONE batched question, only if truly blocked.
- Java in this repo IS compiled by Actions: if it fails the user sends the error; fix and re-send only the broken file.

## 0b. HANDOFF CHAIN (mandatory, every AI repeats it)
The user moves work between AIs because each has a small free limit. So when you finish (or are about to run out):
1. Send the changed files (only those), each with its exact repo path, plus the Arabic GitHub upload steps from 0a. The user uploads Java/XML/HTML files himself through the GitHub website (Add file > Upload files); he does NOT have push access like some AIs do. Never assume he can run git.
2. ALSO create a tiny handoff package for the NEXT AI: this START_HERE.md (updated with what you changed, new APP_VER, known open problems) + PROMPT_FOR_AI.txt + only the files the open task touches (not the whole project). If you have code tools, zip it (target < 150KB) and send it.
3. In that package, KEEP this 0b section and tell the next AI to do exactly the same at the end, so the chain continues AI after AI.
4. Update the notes in this file (version history + open issues) before packaging.

## 0c. GET PUSH ACCESS (so you update the app directly, no manual upload)
1. If you have GitHub/repo tools (e.g. an `add_repo` tool or a GitHub connector): call it for `yassinsherif145-boop/mozakrati` with push access, clone, edit, commit, push to `main`. Do not pre-check the repo with curl (private-style 404s mislead).
2. If you have NO such tool, do not give up silently: tell the user, in Egyptian Arabic, the linking steps for YOUR platform. For Claude (claude.ai) they are: Settings > Connectors > GitHub > Connect; then github.com > Settings > Applications > install the "Claude" GitHub App on the repo `mozakrati` (Only select repositories); then open a NEW chat and ask again. For other platforms: say whether it supports a GitHub connector/plugin and where to enable it; if it cannot, fall back to 0a (send changed files).
3. After the push, check the Actions run (green = ok; if a Maven 429 error, re-run / push an empty commit).

## 0. Rules for you (the AI)
1. User = student, ADHD, **free plan with tiny limits**. Be efficient: deliver working files, no essays, no recap of what you read.
2. Reply in **Egyptian Arabic, short, step by step**. Start every delivery with **"ده محتاج APK"** or **"ده ملف واحد بس (index.html)"**.
3. Never invent features/science. If something is impossible/risky, say so and propose the nearest alternative. Complete missing details logically.
4. UI: few colors, no clutter, explanations only inside Settings (small, faint). 12-hour clock (5:30 م). Arabic RTL. Never tell him to uninstall or lose data.
5. **Token saving**: do NOT load the whole project. Use the MAP (section 5) to open only the needed line ranges of `index.html`. Edit with **exact-string patches** with count asserts (never rewrite or slice-replace between markers; read what lies between). Return only changed files.
6. Every release: bump `const APP_VER=N;`. After editing: extract `<script>` -> `node --check`; run a headless smoke test if you have tools (section 7).
7. Delivery: return ONLY the changed files, complete, with their path under `app/` (e.g. `app/src/main/assets/index.html`). If you have code tools, also zip the whole `app/` folder. Page-only change -> just `index.html`.

## 1. Platform
Thin Java shell + WebView loading `app/src/main/assets/index.html` (HTML+CSS+JS, ~171KB, one script, global state `S`). package `com.mzk.app`, minSdk 26, target 33, compile 34, Java 17. Phone: **Honor 7c, Android 8.0, EMUI, old WebView -> avoid new JS/CSS** (wrap in try/catch).
GitHub repo `yassinsherif145-boop/mozakrati` -> Actions builds `app-debug.apk` (artifact `mozakrati-apk`), signed with committed `app/debug.keystore` (updates install over old app, data kept). `versionCode=100+GITHUB_RUN_NUMBER`.

## 2. Release channels
- **A) OTA (page only)**: higher `APP_VER` in index.html. User: ⚙ ← 🔄 التحديثات ← pick file; or uploads to GitHub and the app auto-fetches the raw URL (`S.updUrl`, repo must be Public) 5s after open (`updAuto`). Native: `Util.pageUrl()` uses `files/index_override.html` if `prefs.updVc==versionCode`; a new APK drops the override; previous kept as `index_prev.html` (rollback). Safety: override page must call `Android.pageOk()` within 8s else deleted. `updApply()` checks `APP_VER=number` and `new Function(script)` parses.
- **B) APK (Java/res change)**: user uploads `app/` to GitHub (Add file -> Upload files), waits for ✓ in Actions, downloads artifact, installs OVER the old app. If "app not installed" -> old key differs -> backup (⚙ ← 💾 البيانات) then reinstall once.

## 3. Android shell (`app/src/main/java/com/mzk/app/`)
MainActivity (WebView host, file chooser, ringtone picker, battery prompt) · AlarmActivity (fullscreen over lock screen; hash routes `#alarm #reason #warn #pend #wk #pz`) · ActionActivity (notification "تمت") · AlarmService (foreground; modes normal/reason; loop 2s reopens AlarmActivity, volume max; `SILENCE`) · AlarmReceiver (`RING RSN CUT` + weekly; re-arms) · BootReceiver · Sched (AlarmManager: ring via `setAlarmClock`, snooze, cut, rsn, wk; prefs `sched` JSON `{rings,cuts,rsn,wk}` epoch ms) · Notifs (single ongoing notification id 1000, RemoteViews `notif_task/notif_big/notif_row`, `ugLabel`, `timer`) · TimerReceiver (action PAUSE -> `OverlayUi.showPause` or `#pz`) · AppWatch (foreground service, UsageStatsManager: logs time per tracked app, prompts overlay check) · OverlayUi (SYSTEM_ALERT_WINDOW overlays: check + pause) · Speak (TTS, Arabic) · UsageCalc (per-app usage math) · Bridge · Util. AppWatch's mandatory foreground notification is the SAME notification id 1000 as the tasks notification (`Notifs.idleNote/idle`; `Notifs.clear` re-posts the idle one while `AppWatch.running`; `onDestroy` detaches) - Android forces a notification for any background service, it cannot be removed, only merged. OverlayUi check screen asks a DURATION (hours + minutes), not a clock time.
Permissions: INTERNET, POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT, FOREGROUND_SERVICE, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW.

`window.Android` (JS->native, always optional-chain): `onAlarm(ev,int)` (started|reasoned|snooze|reason|silence|closeui|openmain), `setTasks(json)`, `setSchedule(json)`, `testRing(min)`, `saveState/loadState(json)` (native backup; JS keeps newer `S.ts`), `saveText(name,text)`, `pickTone()/toneName()`, `saveUpdate(html,ver)/rollbackUpdate()/resetUpdate()/reloadPage()/pageOk()`, `speak(t)`, `setTimer(json)`, `watchStatus()`, `openUsageSettings()`, `openOverlaySettings()`, `appsList()`, `setTracked(json)`, `takePz()`, `usageData()`.

## 4. Web app model
Persisted by `save()` to `localStorage[K]` AND `Android.saveState`; `reSync()` on focus. `draw()` re-renders everything; input drafts live in `DR` + `drRestore()`.
`S` keys: `items[]` (deliverables `{id,type:'hw'|'exam',sid,title,due,show,start,created,big,done,m,kw,subs:[{t,d,on,a,kids}]}`), `miss[] log{} notes{} pp[] accu[] rsd{} kw[] exLead{} al{} tt{} subjects[] qpool seen ts ttv updUrl`, plus newer: `pat{}` (pattern memory `{days,samples<=5,stable}`), `tlog[] brl[]` (time/break logs), `watch{apps,wait,on}`, `run brk pz` (running timer / break / pause-screen), `wrs{} tms{} wkd{}` (weekly reasons / estimates / weekly done), `apx{}` (user edits/deletes of app-usage log entries by entry id: `{w:text}` or `{x:1}`).
`S.al` defaults: on, after:2, hol:'06:00', gap:10, max:4, nq:3, types:['m'], lvl:1, scope:'all', pick:[], cut:20, warn, skip, hwd:1, exd:4, wn:3, qo, late, rsn:'06:10', rsnS:'06:30', rsnE:'07:10', rsnI:10, rsnOn, wkf, learn, rest:4.
Urgency = `urg(it)` (3 levels, fixed phrases: "على المسار" / "من الأفضل أن تبدأ دلوقتي" / "لا يوجد وقت آخر"); cards = board grid (`boxHtml`/`card`).

## 5. MAP of index.html (valid for APP_VER=31 (lines drift by a few); after edits search by function name; line numbers refer to app/src/main/assets/index.html)
- **L6-~370** CSS (23KB, skip unless styling) + static HTML (add form, lists)
- **L366-387** state/load/save/norm + date helpers
  `load@366 norm@368`
- **L388-412** subject/due helpers, pickers, date sheet (dpCb callback)
  `nextClass@388 lbl@389 sname@390 pick@393 dueOf@394 closeSheet@395 subjSheet@396 pickSubj@400 dateBtn@401 dateSheet@403 dpDraw@404 dpNav@407 dpPick@408 subjBtn@410 msg@412`
- **L413-435** items: toggle/subs/kids(editKid)/subjects/type
  `find@413 toggle@414 delItem@415 addSub@416 setSub@417 chkDone@418 togSub@419 kidSync@420 togK@421 addKid@422 togKid@423 delKid@424 kidsHtml@425 delSub@427 addSubj@428 setType@429 togDay@430 expo@431 impo@432 delSubj@433`
- **L436-455** rest-day helpers, extra study days (xd*), planned(), acc(), adder
  `wdays@436 restDates@437 xdOpts@439 daysLeft@440 xdAdd@441 exFit@442 xdDel@443 xdChip@444 xdBtns@445 backWd@446 pinOpen@451 bigSubs@452 adderHtml@453`
- **L456-486** urgency + board cards (boxHtml/card/extra); far card taps open editItem
  `urg@456 ugHtml@466 dyHtml@467 boxHtml@469 card@471 extra@484`
- **L487-529** refresh, attHtml (attendance inside the calendar), place, note, logToday
  `refresh@487 mv@509 attHtml@510 attDraw@511 rvDay@512 rvWeek@513 place@514 note@520 logToday@521`
- **L530-669** logs: weekData, bars/line, wkDetail/wkRender, moDetail, yrRender, commitStats, demo/test mode
  `clp@530 weekData@531 weeksAll@541 mInfo@547 ring@554 lab2@556 bars@557 line@565 net@573 dayItems@582 closeDay@583 showDay@585 daysPage@598 wkDetail@604 wkRender@624 moDetail@631 moRender@638 yrPick@640 tlStats@641 avgRows@642 moAvg@643 allAvg@644 avgNote@645 avgOk@646 yrRender@647 goWeek@654 commitStats@655 newCode@658 setCode@659 syncDays@660 demoData@661 resetUI@662 enterTest@663 exitTest@664 fakeSet@665 fakeNext@666 subjIn@667`
- **L670-687** calendar + attendance stats (calStats/calRender)
  `actMark@670 activeSet@671 calStats@672 calNav@674 calRender@675`
- **L688-738** school timetable loader + timetable editor (tt*)
  `mkCode@688 loadSchool@689 ttOpen@699 ttClose@703 askDlg@705 ttDraw@706 ttAfter@717 ttPaintTog@718 ttCell@719 ttSheet@720 ttSet@722 ttMove@723 ttGhost@724 ttBind@725 ttTimes@730 ttTimesDraw@731 ttSetT@735 ttShift@736 ttPhoto@737 ttLoad@738`
- **L739-769** vis/shw/sweep (missed), restoreMiss0, redoDone
  `fsQueue@739 vis@740 shw@741 sweep@742 ntToggle@751 restoreMiss0@754 redoDone@760`
- **L770-813** lessons popup, todayTasks, readiness, hwRate, exam warning
  `prevLessons@770 lsBody@771 lsPop@773 lsDone@777 isLate@780 lastEnd@783 alarmMinD@785 todayTasks@787 readiness@794 examsWeek@803 closeWarn@804 hwRate@805 ringW@806 warnRows@808 showWarn@809`
- **L814-832** alarm screens + reasonScreen
  `alState@814 closeAlarm@815 showAlarm@816 alStart@819 toast@820 alLater@821 closeReason@824 reasonScreen@825 rsChk@829 rsSave@830`
- **L833-900** settings UI (stDraw)
  `setClose@833 setOpen@834 setAl@837 toneSet@841 stTabSet@842 stDraw@843`
- **L901-967** route, alarm challenges, drafts DR, reSync, notifPayload(force)
  `route@901 alarmTasks@909 togType@911 togPick@912 alTap@913 pickFrom@919 pickQuotes@922 fetchQuotes@923 mkMath@926 mk1@931 mkQs@937 alInfo@944 drIn@950 softDraw@951 drRestore@952 reSync@953 alReg@955 alNone@956 partIn@958 partDone@959 ttMig@960 firstStart@961 schoolNow@962 notifPayload@963`
- **L968-1017** edit dialogs, TASK EDIT SHEET (editItem/eiDraw/exAddDay/exDelDay/exSetDue/editKid), OTA updater (APP_VER)
  `editDlg@968 editSub@971 editKid@972 closeEI@973 editItem@974 exStudyDays@976 exPrev@977 eiSetSub@978 eiDays@979 eiDraw@982 eiSave@988 eiDel@990 exAddDay@991 exDelDay@994 eiDate@997 exSetDue@998 updApply@1005 updInstall@1008 updFile@1009 updCheck@1010 updBack@1011 updReset@1012 partBlur@1014 adderBlur@1015`
- **L1018-1063** accList, pendFor, hardConfirm, restoreMiss, keywords
  `accList@1018 pendFor@1024 hardConfirm@1031 testRun@1034 testMenu@1035 testPend@1048 testTasks@1049 testNotif@1050 testPrompt@1051 restoreMiss@1052 kwList@1054 kwUpdate@1055 kwTog@1059 setLead@1060 kwSet@1061 kwDel@1062 kwAdd@1063`
- **L1064-1129** alarm draw/check/go, scheduleList, nativeSync, draw() master render
  `alDraw@1064 alCheck@1076 alNew@1079 alGo@1080 wrAct@1081 wrCheck@1082 warnOk@1084 scheduleList@1085 nativeSync@1094 draw@1095`
- **L1130-1184** PATTERN MEMORY, exam-days rows (exDayList/exEdit), szUpdate, addItem
  `patKeys@1130 patFor@1133 patLearn@1134 patVal@1135 patRate@1136 patEval@1137 patAsk@1141 patFix@1143 patLbl@1144 patName@1145 ask2@1146 exDayList@1147 exEdit@1149 szUpdate@1152 pdAdj@1168 addItem@1169`
- **L1185-1214** forced pending-reasons screens (pend*, rsChk2, sentGate)
  `rsDays@1185 pendList@1186 pendScreen@1190 pendAsk@1194 pendGo@1198 pendLater@1199 pendForm@1200 rsChk2@1205 pendSave@1206 sentGate@1210 pendCheck@1213`
- **L1215-1243** weekly log (review day), wrSet/ppEdit/ppDel/wrDel/agr(auto-grow)/rrow
  `wkMig@1215 closeWkS@1216 wkNeed@1217 weeklyScreen@1218 wkFin@1223 wkLater@1224 wkCheck@1225 tmAvg@1226 moStab@1227 wrSet@1230 ppEdit@1231 ppDel@1232 wrDel@1233 agr@1234 agAll@1235 spanTxt@1237 rrow@1240`
- **L1244-1277** settings: patterns + reasons settings
  `patSet@1244 patDel@1245 patRun@1246 stPat@1247 stRsn@1258`
- **L1278-1367** ESTIMATES + TIMERS per part (tm*), breaks, forced pause screen (pz*), stop gate (tmStop)
  `tmGet@1278 tmElapsed@1283 tmPartMs@1284 tmPartEst@1285 tmTaskMs@1286 tmTogP@1288 tmEst@1289 tmSumEst@1290 brMs@1291 brTog@1292 estBad@1293 estGate@1294 tmPanel@1295 tmHtml@1298 tmStart@1299 tmPauseSilent@1305 tmPause@1306 brkTog@1308 tmEndBreak@1309 ringOn@1312 mkChal@1316 tmStop@1321 tmStopDo@1327 tmResume@1328 tmStopFor@1329 tmFin@1333 pzShow@1341 pzChk@1344 pzOk@1345 pzCheck@1346 pzRoute@1347 tmSync@1348 runBanner@1353 brkBanner@1354 delBrl@1359 tlBars@1360 wkTime@1361`
- **L1368-1424** weekly times/apps (apxEdit/appsInline), tracking settings (stWatch), tmShift/tmClean
  `usageData@1368 subjOf@1369 wkEst@1373 wkAdd@1378 apxEdit@1380 appsInline@1382 appsLoad@1390 chTog@1391 sentSet@1392 wtSync@1394 wSet@1395 wApp@1396 wList@1397 stWatch@1400 pzPoll@1411 tmShift@1417 tmClean@1418`
- **L1425-1484** apps-usage STATS screen (st*)
  `stSync@1425 stRange@1426 stLabel@1432 iconsFor@1433 stAgg@1435 barChart@1440 stOpts@1447 stHtml@1451 stRender@1472 stMode@1473 stNav@1474 stN@1475 stPick@1476 stDay@1477`

## 6. Hard-won gotchas (do not repeat)
1. RemoteViews allow only specific classes (LinearLayout, FrameLayout, RelativeLayout, TextView, ImageView, Button, ProgressBar, Chronometer). A plain `<View>` crashed the app on notify -> use FrameLayout.
2. WebView doesn't blur `<input>` on tapping empty space -> global touchstart blur + explicit commit on input/checkbox/＋.
3. `<input type=date>` crashes on his WebView -> custom `dateSheet()`. No `confirm/prompt/alert` -> `askDlg/editDlg/hardConfirm`.
4. file:// override needs `setAllowFileAccess(true)`; localStorage shared by 3 WebView activities -> `S.ts` + `reSync()` + native `saveState` (stale-state overwrite bug).
5. EMUI clears ongoing notifications -> `Notifs.show` re-posts if id 1000 isn't active.
6. Forced screens: AlarmService loop reopens AlarmActivity; sound silenced while typing (`onAlarm('silence',20)`), resumes if idle.
7. Honor kills background apps: user sets App launch=manual (3 toggles), ignores battery optimisation, locks app in recents.
8. JS newer than ~Chrome 64 only inside try/catch (`\p{L}` regex already wrapped). `fetch` timeout via AbortController in try.
9. `draw()` re-renders cards: never keep DOM state, use `DR`.
10. Escape nested template literals carefully in patches; always `node --check`.
11. (v31) The tasks notification now shows ALL day whenever tasks exist (no time window, no hush during school/timers); only the explicit "stop today" (`S.al.ntOff`) hides it. It lives on channel `tasks2` (IMPORTANCE_MIN) so it stays COLLAPSED until the user expands it (Android auto-expands the top LOW notification). Trade-off: no status-bar icon; change to IMPORTANCE_LOW in `Notifs.ensure` if he wants the icon back.
12. An exam's `show` must equal its `start` (else its first-day part stays hidden when a rest day lies inside the range) - fixed in v13 `addItem`.
13. Task edit sheet: day chips are contiguous study days ending the day before the exam; adding a day unshifts an empty sub and starts earlier, removing one only drops an EMPTY first sub (sub index = day index; `S.run/brk/pz` indices are shifted with it).
14. Long reasons: `textarea.ag` auto-grows via `agr()/agAll()` (MutationObserver) - never fixed-height boxes.

## 7. QA recipe
Extract `<script>` -> `node --check`. Headless Chromium (puppeteer), viewport 360x740 mobile, `evaluateOnNewDocument` defining a fake `window.Android` with all methods in section 3 (`setTasks(j){window.__t=j}`, `saveState:s=>{st=s}`, `loadState:()=>st`, `toneName:()=>'x'`, `saveUpdate:()=>true`, others no-op). Then `page.evaluate(()=>{S.items.push({...});draw();...})` and inspect DOM / `__t` (notification payload). Java has no CI here: keep changes small, re-read for brace balance.

## 8. DONE (do not redo)
Cards/parts/sub-parts, big hw split, exam auto slots, keywords, per-subject exam lead, alarm + challenges + quotes, forced morning reasons screens (start / not started -> sentence gate -> reasons; test mode skips the gate), accumulation/missed logic, exam warning ring, P1 visual system + urgency + board grid, P2 pattern memory (+ settings), Monday weekly log + forced weekly screen, estimates + timers per part + breaks + forced pause screen, app tracking + overlay + stats + TTS, OTA updater, native state backup, timetable editor, calendar. v10-v12: Thursday (rest day `S.al.rest`) rules + `item.xd` extra study days. **v13**: attendance counter only inside the calendar; exam form shows day CHIPS (+ يوم / × on the earliest) instead of a counter; task edit sheet (tap an upcoming card or "تعديل": title, subject, exam date, study days, delete); small tasks editable; reasons shown in full (auto-grow), per-reason delete, weekly reason delete; app-usage log entries editable/deletable; test notification ignores the time window; single shared notification (monitor merged); app prompt asks duration.

## 8b. DELIVERY (v34) - READ FIRST
- The code lives in the PUBLIC repo `yassinsherif145-boop/mozakrati` (branch main). If your session can attach GitHub (`add_repo` with push access; the user already connected GitHub + the Claude GitHub App to claude.ai), clone it, edit, commit and push to main. Do NOT ask the user to upload zips.
- Every push to main runs `.github/workflows/build.yml`: builds the debug APK (signed with `app/debug.keystore`, so it installs over the old one), uploads the artifact and publishes a GitHub Release `build-<run_number>` with `mozakrati.apk`. `versionCode = 100 + run_number`.
- In the app: `Updater.java` (Bridge: `apkCheck/apkStatus/apkInstall`) reads `releases/latest`, downloads the APK, installs it through `androidx.core FileProvider` (`${applicationId}.fp`, `res/xml/file_paths.xml`, permission REQUEST_INSTALL_PACKAGES). UI: Settings > التحديث > "تحديث التطبيق (APK)" + a bottom banner shown ~6s after launch when a newer build exists. So after you push, the user just taps "تحديث الآن" in the app.
- Check the build with `https://api.github.com/repos/yassinsherif145-boop/mozakrati/actions/runs?per_page=2` (WebFetch). Java is compiled for real by Actions: a red run = read the log link and fix.
- Page-only changes still work through the OTA/file picker, but a push + in-app update is now the normal route for everything.

## 9. NOT DONE / decisions
- **CANCELLED by the user (v33): "التصنيفات والجلسات" (مسؤول / التطوير اليومي / sessions / category chart). Do NOT rebuild them.** Also the old spec item "notification redesign" belonged to that idea: do not do it unless he asks again.
- Removed in v33: the "تعديل" button inside each task card (he edits by tapping the task itself).
- DND bypass (v32, APK) stays: channels `tasks3`/`alarm2` (`Notifs.CH`, `Notifs.ALARM`) with `setBypassDnd(true)`, `Bridge.openDndSettings/dndOk`, Settings > notifications row.
- Floating bubble (v33): "إنهاء العداد" now starts the ring natively first (`startRing`, if `S.al.ringStop!==false`, sent as `rs` in the timer JSON) and opens `#stop`; JS `route()` for `#stop` closes the screen + stops the ring if nothing is running. Panel redesigned (dark rounded card, icons, red stop row, closes on outside tap/10s). Settings: size `S.al.bubSz` s/m/l (sent as `sz`), reset position `S.al.bubRp` (sent as `rp`; position persisted natively in prefs `bubX/bubY`).
- v35: app-watch prompt: leaving the app while the "extra time" prompt shows marks the package `fresh` (`AppWatch.fresh`) so the next entry is a normal first prompt (no forced challenge); prompt dismiss polling 500ms. Break = count-up timer with no fixed length (`brkTxt`, `fz` frozen ms in timer JSON when paused); `tmStopDo` logs the break (`tmEndBreak`) so its real duration appears in the log. Bubble formats H:MM:SS. Tasks notification is collapsed by default (no big view, `setNumber`), toggle `S.al.ntOpen`; while the timer notification (id 1001) is active the tasks one is hidden (`Notifs.holdForTimer`) and re-posted from prefs `lastTasks` when the timer ends. New challenge type `calc` (JS `mkChal`, Java `chal`). Perf: `prs` memoised, `ymd/mid/diff` without Date copies.
- v36: floating bubble REMOVED by the user (JS sends `bub:false`, settings rows removed; Java bubble code left dead). App-watch prompt (`OverlayUi.showCheck`): NO alarm/challenge/countdown any more unless the chosen duration is > 15 min (custom field); then the challenge box (types `ct`, count `chn`, set in Settings) shows with the ring, and hides again if he lowers the time. Nothing is logged unless a reason is written. Tasks notification: expandable again (`ntOpen` default on), count number smaller (13sp).
- v37: tasks notification redesigned: mode `S.al.ntMode` sent as `nm` ('g' default = Android notification GROUP: summary id 1000 + one child per task ids 1010..1017 via `setGroup("mzk_tasks")`, collapsed by default and pulled down with a swipe; 'o' = single notification with big view (EMUI auto-expands the top one); 's' = one line only). `Notifs.cancelKids` cleans the children. Row layout/badge restyled (`notif_row.xml`, `nc_badge.xml`). Needs the user's confirmation on his Honor phone.
- `PROMPT_FOR_AI.txt` = tiny message the user pastes to any other AI.
- Unclear user request (asked him): "وابقي شيل الدايره ده تعديل" - which circle to remove.
- STILL OPEN: ringing as an independent service that survives the app being killed. All v31-v33 Java was never compiled on a real Android toolchain (javac syntax parse + cross-file call/resource check only); if Actions fails ask for the error text.
- Delivery idea: page-only changes need NO GitHub: user can pick `index.html` in the app (⚙ ← 🔄 التحديثات) or upload just that file to the repo for auto-fetch. Java changes need the APK. If the user connects GitHub to claude.ai, the AI can push to `yassinsherif145-boop/mozakrati` directly (add_repo with push access).

## 10. User task for this chat
(The user writes it in the chat message.) Free plan: user cannot pay. Always finish by producing a fresh MZK_ALL.txt (guide + all source) reflecting the latest state so the user can continue in another AI. The old v14 batch (T1-T6) is DONE, see the log.

## 11. Version log (condensed; v14-v30 = a previous AI session, v13 and v30b = this one)
- v13: attendance only inside the calendar; task edit sheet (`editItem`/`eiDraw`, taps on upcoming cards); small tasks editable; reasons auto-grow + per-reason delete (`ppDel/wrDel/agr`); app-log entries editable (`apxEdit`, `S.apx`); exam `show` = `start`; test notification ignores the time window.
- v14-v18: notifications appear as soon as tasks exist; urgency: lv2 only on the last study day, lv1 when a part is accumulated; exam study days = rows per day (one part per day, editable inline), no adding days beyond the available ones (only the rest-day system); exam day itself never a study day, today counts; every part/kid needs an estimate (`estGate`), live sum of estimates replaces the whole-task time box; weekly log `wkEst` ("وقت المهام"); kids styled as chips; patterns learned when a task is finished (`patEval`, `S.patQ`, threshold `S.al.patThr`), not at creation.
- v19-v22: hotfix (a slice deleted `szUpdate`); AlarmActivity watchdog (`nat('ready')`, reload then emergency stop button after 15s), AlarmService auto-stop of reason mode after 10 min; red error banner (`showErr/sf/safeS`); `sf(norm);sf(draw);sf(route);` then `Android.pageOk()`.
- v23-v26: review day setting `S.al.rvd` (default Monday); weekly log reasons collapsible, grouped by day; weekday names in `wkEst`.
- v27-v28: notification shows task count + other names; patterns/keywords changes; timers count DOWN (`S.al.brkLen`), tasks notification hidden (`hush`) during timer/break/school hours/"stop today" (`S.al.ntOff`), window ends at `S.al.bed`; ⏹ stop gate with English sentences (`tmStop`, `S.al.stopGate/stopN`).
- v28b-v30 (Java): app prompt = reason box + duration chips 1/5/15/custom minutes + "اقفل التطبيق"; over-time re-prompt requires challenges (copy / UPPER / reverse words / initials; `OverlayUi.chal`, JS `mkChal`; settings `S.al.chTypes`, `S.al.sents`, `S.watch.ch/chn` synced via `Bridge.setTracked`); ringing (`OverlayUi.startRing/stopRing`, `Android.ring`); floating ring bubble (`OverlayUi.bubble`, `S.al.bubble`).
- v30b: fixed stale tasks notification during hush while AppWatch (foreground service, same notification id 1000) runs; idle notification on silent channel "idle"; removed a corrupted `ic_u2.xml`. Verified: node fuzz (seeds 1-5) + real Chromium smoke = 0 errors; XML parses; Java only syntax-checked (no android.jar), never compiled, never run on the phone.
- v31 (this session, on top of v30): ALL typed sentences everywhere (alarm challenge, alarm "write sentence", reasons gate `sentGate`, `hardConfirm`, stop gate `tmStop`, native app-check challenge) are English productivity/work/thinking quotes WITH the author. Pool = `QUOTES` (57 curated) + `S.qpool` fetched online (`fetchQuotes`: quotable.io then dummyjson, filtered by `qOk`/`QKW`, max once per 4h, kept 120) + optional user lines `S.al.sents`; helpers `QALL/qShort/qSel/qAu`; native gets `text\u0001author` strings via `ensList()` -> `Bridge.setTracked(sn)`. Typing in ANY alarm challenge hushes the sound (`wrAct` on inputs/`alTap`; native `OverlayUi.hushRing` 20s; JS `tmStop` toggles `ringOn`). Leaving test mode (`exitTest`) clears overlays and suppresses forced screens for 30 min (`TMX`). Floating bubble: smaller (58dp), hidden while `AlarmActivity` is on screen, tap opens a panel (استراحة / فتح التطبيق / إخفاء العداد / إنهاء العداد -> `#stop` route -> `tmStop()`); removed the useless "اقفل التطبيق" button in the app-check overlay. Break counter keeps counting past 0 as "+mm:ss" (`brkTxt`) and the log stores the real elapsed ms. Homework add form no longer has the big/"how many days" option (hw is always small). Tasks notification: see gotcha 11.
- v32: categories/sessions module (REMOVED in v33), DND bypass (APK), `APP_VER=32`. Checked: `node --check`, headless Chromium smoke (0 errors), javac parse + call/resource cross-check. Notification redesign NOT done.
- v33: removed the categories/sessions module (cancelled by the user), removed the card "تعديل" button, bubble stop/ring fix + new panel + size/position settings, `APP_VER=33`. Checks: node --check, Chromium smoke 0 errors, javac parse.
- (v32 added categories/sessions: now REMOVED) OLD OPEN: ringing as an independent service that survives the app being killed; bubble size/position settings; section 9 items 1-3 and 5 (categories, sessions, weekly category chart, DND bypass).

- v40: pattern value = number of DISTINCT days parts were actually finished (`patVal`, uses `s.on`), so accumulated parts finished together count as one day; items created with the pattern's day-count get `it.pt=1` and show a small "نمط" tag at the end of the urgency row (`ugHtml`, `.pn`); parts added with the "+ جزء إضافي" adder get `s.ad=today` and stay on that day (`planned` honors it, `exFit` ignores them).
- v41: exam/hw part rows in the task card are flat (no boxes/borders), small gap, own checkbox each, red 'متراكم' tag beside accumulated parts (CSS override at end of <style>: .subs>.sb).
