/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.core.test;

import org.graalvm.compiler.nodes.FrameState;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import org.junit.Test;

public class IntegerEqualsCanonicalizerTest extends GraalCompilerTest {

    @Test
    public void testSubtractEqualsZero() {
        test("testSubtractEqualsZeroSnippet", "testSubtractEqualsZeroReference");
    }

    public static int testSubtractEqualsZeroReference(int a, int b) {
        if (a == b) {
            return 1;
        }
        return 0;
    }

    public static int testSubtractEqualsZeroSnippet(int a, int b) {
        if (a - b == 0) {
            return 1;
        }
        return 0;
    }

    @Test
    public void testSubtractEqualsZeroLong() {
        test("testSubtractEqualsZeroLongSnippet", "testSubtractEqualsZeroLongReference");
    }

    public static int testSubtractEqualsZeroLongReference(long a, long b) {
        if (a == b) {
            return 1;
        }
        return 0;
    }

    public static int testSubtractEqualsZeroLongSnippet(long a, long b) {
        if (a - b == 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Tests the canonicalization of (x >>> const) == 0 to x |test| (-1 << const).
     */
    @Test
    public void testShiftEquals() {
        test("testShiftEqualsSnippet", "testShiftEqualsReference");
    }

    @SuppressWarnings("unused") private static int field;

    public static void testShiftEqualsSnippet(int x, int[] array, int y) {
        // optimize
        field = (x >>> 10) == 0 ? 1 : 0;
        field = (array.length >> 10) == 0 ? 1 : 0;
        field = (x << 10) == 0 ? 1 : 0;
        // don't optimize
        field = (x >> 10) == 0 ? 1 : 0;
        field = (x >>> y) == 0 ? 1 : 0;
        field = (x >> y) == 0 ? 1 : 0;
        field = (x << y) == 0 ? 1 : 0;
        field = (x >>> y) == 1 ? 1 : 0;
        field = (x >> y) == 1 ? 1 : 0;
        field = (x << y) == 1 ? 1 : 0;
    }

    public static void testShiftEqualsReference(int x, int[] array, int y) {
        field = (x & 0xfffffc00) == 0 ? 1 : 0;
        field = (array.length & 0xfffffc00) == 0 ? 1 : 0;
        field = (x & 0x3fffff) == 0 ? 1 : 0;
        // don't optimize signed right shifts
        field = (x >> 10) == 0 ? 1 : 0;
        // don't optimize no-constant shift amounts
        field = (x >>> y) == 0 ? 1 : 0;
        field = (x >> y) == 0 ? 1 : 0;
        field = (x << y) == 0 ? 1 : 0;
        // don't optimize non-zero comparisons
        field = (x >>> y) == 1 ? 1 : 0;
        field = (x >> y) == 1 ? 1 : 0;
        field = (x << y) == 1 ? 1 : 0;
    }

    @Test
    public void testCompare() {
        test("testCompareSnippet", "testCompareReference");
    }

    public static void testCompareSnippet(int x, int y, int[] array1, int[] array2) {
        int tempX = x;
        int array1Length = array1.length;
        int array2Length = array2.length;
        // optimize
        field = x == tempX ? 1 : 0;
        field = x != tempX ? 1 : 0;
        field = array1Length != (-1 - array2Length) ? 1 : 0;
        field = array1Length == (-1 - array2Length) ? 1 : 0;
        // don't optimize
        field = x == y ? 1 : 0;
        field = array1Length == array2Length ? 1 : 0;
        field = array1Length == (-array2Length) ? 1 : 0;
    }

    public static void testCompareReference(int x, int y, int[] array1, int[] array2) {
        int array1Length = array1.length;
        int array2Length = array2.length;
        // optimize
        field = 1;
        field = 0;
        field = 1;
        field = 0;
        // don't optimize (overlapping value ranges)
        field = x == y ? 1 : 0;
        field = array1Length == array2Length ? 1 : 0;
        field = array1Length == (-array2Length) ? 1 : 0;
    }

    public static boolean testNormalIntegerTest(int a) {
        return (a & 8) != 0;
    }

    public static boolean testAlternateIntegerTest(int a) {
        return (a & 8) == 8;
    }

    @Test
    public void testIntegerTest() {
        test("testNormalIntegerTest", "testAlternateIntegerTest");
    }

    private void test(String snippet, String referenceSnippet) {
        StructuredGraph graph = getCanonicalizedGraph(snippet);
        StructuredGraph referenceGraph = getCanonicalizedGraph(referenceSnippet);
        assertEquals(referenceGraph, graph);
    }

    private StructuredGraph getCanonicalizedGraph(String snippet) {
        StructuredGraph graph = parseEager(snippet, AllowAssumptions.YES);
        createCanonicalizerPhase().apply(graph, getProviders());
        for (FrameState state : graph.getNodes(FrameState.TYPE).snapshot()) {
            state.replaceAtUsages(null);
            state.safeDelete();
        }
        return graph;
    }
}
