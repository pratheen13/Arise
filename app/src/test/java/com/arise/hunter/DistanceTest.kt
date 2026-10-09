package com.arise.hunter
import org.junit.Assert.*
import org.junit.Test
class DistanceTest {
 @Test fun importsDistanceWithoutWorkout() {assertEquals(DistanceValue(1.25,false),DistanceValue.resolve(1250.0,2000))}
 @Test fun stepsOnlyHasLabeledEstimate() {assertEquals(DistanceValue(1.4,true),DistanceValue.resolve(null,2000));assertEquals(DistanceValue(1.4,true),DistanceValue.resolve(0.0,2000))}
 @Test fun measuredDistanceReplacesEstimateWithoutAddingThem() {assertEquals(DistanceValue(.8,false),DistanceValue.resolve(800.0,2000))}
 @Test fun noDataIsNotInvented() {assertNull(DistanceValue.resolve(null,null));assertEquals(DistanceValue(0.0,false),DistanceValue.resolve(null,0))}
 @Test fun invalidDistanceFallsBack() {assertEquals(DistanceValue(.7,true),DistanceValue.resolve(Double.NaN,1000))}
}
