# ssMath

A native Kotlin / Jetpack Compose Android app for practicing math. Android 8.0
(API 26) or newer; targets Android 16 (API 36).

## How it works

1. **Home screen** – use the **Rewards** (gift box) and **Pokémon** (Poké Ball) buttons
   at the top, each with its image to the left of the label, to open
   your collections; Back returns you to the home screen without losing your setup choices.
   Below the collections, choose **Practice Game** or **Rewards Game**.
   **Practice Game** opens the question setup in a dialog. Choose **Addition**, **Subtraction**,
   **Multiplication** or **Division**, enter the **Minimum number** (defaults to 1)
   above the **Maximum number** (both 1 to 10,000; minimum cannot exceed maximum),
   and answer **How many questions would you like?** using the **Number of questions**
   field (1 to 1,000; defaults to 10), then press **Submit**.
   For **Division**, the two inputs instead ask **What is the Maximum First number?**
   and **What is the Maximum Second number?** Each is an independent limit from
   1 to 10,000, for the dividend and divisor respectively; either limit may be larger.
   Your choices are remembered for next time, with division limits saved separately.
   Practice Game never awards prize fragments or Pokémon; completing its questions
   gives only **Other** animations. Once all five Other scenes have been collected,
   practice can repeat them.
   **Rewards Game** opens a dialog with the four math types. Choose one to use its
   saved parameters, or tap the **Setup** gear icon in the dialog's upper-right corner
   to open a screen with number limits and question counts for all four types.
   **Save defaults** saves all four together; **Back**
   discards unsaved edits. Division's two maximums are independent. Defaults are
   remembered on this device separately from Practice Game choices, and initially
   use your previous setup (or 1–10 and 10 questions on a new install).
   Rewards Game can award all available prizes and animation categories under the
   eligibility rules below. **Cancel** or Android Back closes either game dialog.
2. **Press Start when Ready** – press **Start** to begin. The timer starts now.
3. **Play** – in either mode, addition, subtraction and multiplication use two random numbers
   between the minimum and maximum, inclusive. Subtraction never goes below zero.
   Division always has a whole-number answer, using a first number from 1 to its first maximum
   and a second number from 1 to its second maximum, choosing only exactly divisible pairs.
   A correct answer earns a point; a wrong answer is added to
   the **Wrong** tally in the bottom-left corner. Either way, a new problem of the
   same type follows, with your question progress shown above it. A progress bar fills
   after each answered question, correct or wrong, as an icon jumps to the next point.
   Each game randomly picks one of 10 colorful offline icons: rocket, star, balloon,
   ball, butterfly, fish, frog, bunny, cat, or robot. The same icon stays for every
   jump throughout the test; a new random selection happens only when the next test starts.
   Tap **Quit game** in the upper-right corner (or use Android Back) to leave an unfinished test.
   Confirm **Quit** to return home without saving that game's progress, or choose
   **Keep practicing** to continue.
4. **Results** – after the chosen number of questions (correct or wrong), or
   the wrong-answer limit (or an optional time limit), whichever comes first, the practice stops and shows how many you
   got right, plus every answered problem with a green check (correct) or a red X
   (wrong, with the correct answer if **Show correct answers** is enabled).
   Results are saved with the date and time.
   In Rewards Game, if all the chosen questions were answered, even with mistakes, one of 162 randomly chosen
   congratulations animations plays: dolphins, whales, or anchovies jumping out
   of the water, a balloon-and-confetti party, a candy shower, Pikachu running toward
   the screen and zapping lightning, Squirtle shooting water from his mouth,
   Bulbasaur shooting leaves from his bulb, Charmander shooting fire into the air,
   Jigglypuff rolling and jumping, or Palafin, Finizen, Wailmer, Wailord, Bouffalant,
   Veluza, Mantyke, or Mantine celebrating, plus 144 additional Pokémon from Ivysaur
   through Mew in species-specific habitats. Their offline vector scenes feature
   crawling caterpillars, hanging cocoons, flying birds, swimming aquatic creatures,
   burrowing moles, and more, with matching effects such as leaves, bubbles, fire,
   and psychic rings. Related species have different scenery layouts and motion.
   The 157 Pokémon scenes belong to the
   **Pokémons** category and can only appear after completing **15 or more questions**.
   The remaining five scenes belong to **Other** and can appear at any game length;
   below 15 questions, only **Other** scenes appear. Only scenes not yet in **My Pokémons**
   are selected until all 162 animations have been collected; after that, repeats are allowed.
   If all five **Other** scenes are collected but the full collection is incomplete,
   Rewards Games below 15 questions go straight to results without an animation.
   Practice Game uses only the five Other scenes, regardless of question count,
   without awarding prizes or Pokémon.
   Each scene says **Hurray!!**. These animations
   are drawn in the app and work offline. Press **View results** to
   skip the animation, or wait for it to finish.
   The wrong-answer limit is 5 for up to 50 questions; above 50 it is 10% of the
   chosen total, rounded up to a whole answer (51 questions allows 6 wrong;
   100 questions ends at 10 wrong).
   If the wrong-answer limit ends practice before the last question, a dialog says
   **Nice try! You got {XX} right out of {TOTAL}**, using the chosen number of
   questions as the total. Press **View results** to dismiss it.
   Press **Done** to return to the home screen.

The semi-transparent (50% opacity) settings button in the bottom-right corner opens
**Settings**:

- **App updates** – at the top, check GitHub for a newer release using the button
  beside the installed version, then download and install it.
- **My Rewards** – directly below App updates, view your collected prizes and
  one-third fragments in a four-column grid grouped by **Tier 1** and **Tier 2**,
  even when the rewards system is disabled.
  Tap **Use rewards** in the upper-right corner to see whole reward pictures and
  available counts grouped by tier. Tap a reward and confirm **Yes** to use one; **No** cancels.
  Only whole rewards can be used. Remaining counts are saved on this device;
  fragments and practice history are unchanged.
- **My Pokémons** – directly below My Rewards, collect a still image of each
  celebration you see after completing a game, even when rewards are disabled.
  Practice Game contributes only to **Other**; Pokémon require Rewards Game.
  Each celebration is collected only once and labelled with its name, grouped under
  **Pokémons** or **Other**. Pokémon are sorted by their National Pokédex number
  (Ndex) and labelled with a four-digit number and name, such as **#0025 Pikachu**.
  Other celebrations keep their names without numbers. Each section heading shows its own collected/total
  fraction (out of 157 Pokémon or 5 Other), including zero when none are collected.
  Tap an image to replay its animation; close it or wait for it to finish to return to your collection.
  The collection is saved on this device. Each presented celebration is also recorded
  with its practice result, including repeat celebrations; replaying an image does not award it again.
- **Practice History** – review any previously saved result, delete one, or clear all.
  Rewards Game entries have a translucent theme-colored background to distinguish
  them from Practice Game entries, even when no prize was earned.
  Deleting results also removes their claimed reward fragments from the available
  balance, breaking whole prizes back into fragments when necessary. Clearing history
  does this for every deleted result. Balances never go below zero if rewards were
  already used. The result's My Pokémons reward is also removed unless another saved
  result earned the same celebration. Older collection entries without a linked result
  are kept, as are unrelated celebrations added through Admin.
- **Rewards system** – on by default. In **Rewards Game**, earn
  prizes for completing **25 or more questions** with **more than 90% correct**.
  This setting never enables prizes in Practice Game.
  After the celebration, tap the hopping gift box to open it and release confetti.
  **Tier 1** rewards are for sessions of **25–49 questions**: a random Lollipop,
  Ice Cream Cone, Gummi Bear, Ramen, or Bed Time Fragment. **Tier 2** rewards are for sessions
  of **50 or more questions**: a random Video Game or Restaurant Fragment.
  Bed Time features a sleeping child, floating Zs, and twinkling stars; Restaurant
  features a swaying striped awning and a steaming meal. Their artwork animates
  when revealed and in My Rewards and Use rewards.
  Individual rewards can be enabled or disabled in the **Admin** dialog described below.
  Only enabled rewards from the qualifying tier are selected; if none are enabled in
  that tier, no prize is awarded. Existing prizes and fragments remain available.
  Fewer than 25 questions and exactly 90% correct do not qualify.
  Every 3 fragments of the same type automatically become 1 whole prize, with the
  fragment counter returning to 0. Prizes and their cumulative totals are saved
  with the result; deleting a result removes its claimed fragment from your available rewards.
  A gift dismissed before opening can still be claimed from its history entry.
  All reward illustrations and animations are drawn in the app and work offline.
- **Show timer** – off by default. Show or hide the timer while practicing. The timer pauses while
  Settings, Practice History, My Rewards, or My Pokémons is open, or the app is in the background.
  When both Rewards system and Show timer are enabled, **Time limit** offers
  **None** or **5–60 minutes** in 5-minute increments. The selection applies to the
  next Rewards Game, not Practice Game. A timed game shows time remaining and ends when time runs out,
  saving the partial result without a prize. Changing settings during practice does
  not remove its active time limit.
- **Show correct answers** – off by default. Turn on to reveal correct answers after
  mistakes during practice, in Results, and in Practice History. Wrong answers are
  still marked and counted, and saved attempts are unchanged. This choice is remembered.
- **Vertical equations** – off by default. Below Show correct answers, switch from horizontal
  equations to stacked numbers in practice, Results, and Practice History. Ones,
  tens, hundreds, and higher places line up, with the operator to the left and the
  answer (or `?` during practice) below a line. Works with all four operations;
  the choice is remembered and does not change saved answers.
- **Appearance** – collapsed by default; tap the heading to expand or collapse
  text size, theme, and skin controls without changing your choices. Choose the
  **Blue Wave** look or a **Color theme** (midnight, royal purple, gold, green,
  pink, black) with a **Dark appearance** switch.
- **Text size** – use the slider under Appearance to adjust app text from 80% to
  200% in 10% steps (default 100%). Changes apply immediately, work with your
  device's font-size setting, and are remembered for next time.
- **Skins** – 17 optional offline background images under Appearance: Cherry
  Blossom Sunset, Starry City Sunset, Ocean Wave, Ocean Moonlight, Galaxy, Tropical,
  Mechanics, Aurora Borealis, Desert Dunes, Bamboo Grove, Autumn River, Alpine Dawn,
  Rainy Window, Volcanic Ember, Crystal Cavern, Paper Lanterns, and Synthwave Grid.
  The ten additions come from [ssMusic Player PR #155](https://github.com/skystream006/ssMusic_Player/pull/155).
  Your color theme stays the same.
- **Debug logging** – off by default. See [Debug logging](#debug-logging).

## Build and Install

Open this folder as a project in Android Studio, select a Java 17 Gradle JDK,
install Android SDK Platform 36, and let Gradle sync. Alternatively, with Java 17
on `JAVA_HOME` and the SDK on `ANDROID_HOME`:

```sh
sh ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows use `.\gradlew.bat` with the same tasks. The checked-in Gradle wrapper
verifies its distribution's SHA-256 checksum.

The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Android CI** runs the unit tests, lint and a debug build for every pull request
and push to `main`, and uploads the debug APK as an artifact.

### Versions

Build from a full Git checkout (`git fetch --unshallow` if needed). Versions advance
automatically with every pull request merged into `main` (each merge adds one
first-parent commit). The version name is `0.01.NN`, starting at **v0.01.00** for the
first merged pull request, and the version code is `<first-parent commit count> + 1`.
Rebuilding the same commit keeps its version.

### Debug keystore

Every build is signed with the repository's checked-in debug key
(`app/debug.keystore`, alias `androiddebugkey`, passwords `android`), so local
builds, CI builds and manual releases all share one signing certificate and can
update each other. `applicationId` is `com.ssmath.app`.

### APK updates

The app checks GitHub's latest release when it opens and shows a notice when a
newer version is available. Open **Settings > App updates**, select **Check for
updates**, then **Download and install** and confirm the download.

The download shows progress and can be cancelled. The app checks the package,
version and signing certificate before handing the APK to Android. If prompted,
allow **Install unknown apps** for ssMath, then return to the app to confirm
installation. Android still asks for final approval; a compatible update keeps your
settings and practice history.

For the first installation, download `ssMath-v<version>.apk` from this repository's
**Releases** and open it on the phone. Extract Actions artifact ZIPs before opening
an APK.

### Publishing update-compatible releases

Run **Actions > Manual Android Release > Run workflow** to build latest `main`,
verify the signed APK, and publish one universal APK as the latest `v0.01.NN`
release. The in-app updater reads this repository's latest release, not Actions
artifacts. Re-running a published version leaves its release unchanged; merge a
newer pull request for a new update.

After a PR merges into `main`, **PR Release Reminder** posts a comment linking to
**Manual Android Release**. Follow the link and select **Run workflow** on `main`
when ready; the reminder does not start a release.

The release workflow uses the bundled debug key by default. For a stronger
production key instead, configure these optional repository Actions secrets:

| Secret | Value |
| --- | --- |
| `APK_SIGNING_KEYSTORE_BASE64` | Base64-encoded keystore containing the permanent app signing key |
| `APK_SIGNING_STORE_PASSWORD` | Keystore password |
| `APK_SIGNING_KEY_ALIAS` | Signing key alias |
| `APK_SIGNING_KEY_PASSWORD` | Signing key password |

Configure all four together, or leave all four unset to use the bundled debug key;
a partial set fails the workflow. Never commit a distribution keystore or its
passwords. Local release builds use the same configuration through
`APK_SIGNING_STORE_FILE` and the three password/alias environment variables above.

Switching between the bundled debug key and a distribution key changes the app's
signing certificate. If an update reports a signature mismatch, obtain an APK signed
with the same key as the installed app; do not uninstall to bypass it, as that
removes your practice history.

## Debug logging

**Settings > Debug logging** is off by default. When enabled, choose **Full**
(rotating log files, up to 64 KB) or **Reactive** (only the latest 100 events).
Only event names, status numbers and exception class names are recorded — never
answers, practice results or other personal content. Logs are stored privately on
the device and can be viewed, shared or cleared from Settings.

Tap the description beginning **Off by default** below **Enable debug logging**
seven times to open the **Admin** dialog. Opening or closing it does not add animations.
Press **Add all animations** to add every celebration to **My Pokémons**, without
duplicating ones already collected or changing history and rewards.

Press **Remove rewards** to view available balances by tier. Choose **Remove 1 fragment**
or **Remove 1 whole reward** for a reward, then confirm **Remove**; **Cancel** leaves it
unchanged. Each action removes one of the selected units, without breaking whole rewards
into fragments or changing the other units, practice history, or My Pokémons. Empty balances
cannot be removed. Changes are saved on this device and cannot be undone; claimed gifts
cannot be claimed again. **Back to admin** returns to the Admin dialog.

Under **Available rewards**, enable or disable each reward, grouped by tier. Choices
are saved on this device and apply when a practice finishes, including a practice
already in progress. Disabling every reward in a tier means no prize for that tier;
it does not prevent celebrations or remove existing prizes and fragments, including
unopened gifts in history. Re-enable a reward to make it available for future awards.
All admin sections work even while debug logging and the rewards system are disabled.
