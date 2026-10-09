package com.arise.hunter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class ProgressTest {
 @Test fun firstDayStartsLight() { val p=Progress();assertEquals(5,p.reps);assertEquals(1500,p.stepTarget);assertEquals(.5,p.distanceTarget,0.0) }
 @Test fun repeatedImportsAndConfirmationsRewardOnlyOnce() {val p=Progress(steps=1600,km=.6).award();assertEquals(80,p.xp);assertEquals(p,p.award());val q=p.confirm(0,p.day);assertEquals(120,q.xp);assertEquals(q,q.confirm(0,q.day))}
 @Test fun manualInputCannotCompleteImportedQuests() {val p=Progress(manual=31).award();assertEquals(28,p.earned);assertEquals(120,p.xp)}
 @Test fun midnightResetsDailyProgressAndKeepsXp() {val p=Progress(day=LocalDate.now().minusDays(1).toString(),xp=2200,earned=31,steps=5000,manual=28).rollover();assertEquals(2200,p.xp);assertEquals(12,p.dayLevel);assertEquals(0,p.earned);assertEquals(0L,p.steps);assertEquals(0,p.manual)}
 @Test fun oldConfirmationDoesNotCompleteNewDay() {val p=Progress(day=LocalDate.now().minusDays(1).toString());assertEquals(0,p.confirm(0,p.day).earned)}
 @Test fun targetDoesNotMoveWhenLevelChangesMidday() {val p=Progress(xp=160,steps=2000).award();assertEquals(2,p.level);assertEquals(1500,p.stepTarget)}
 @Test fun rankUnlocksAtExpectedLevels() {assertEquals(0,Progress(xp=1999).rank);assertEquals(1,Progress(xp=2000).rank);assertEquals(5,Progress(xp=10000).rank)}
 @Test fun sourceCorrectionDoesNotAwardAgain() {val p=Progress(steps=1600).award();assertEquals(p.xp,p.copy(steps=0).award().xp);assertEquals(p.xp,p.copy(steps=1800).award().xp)}
}
