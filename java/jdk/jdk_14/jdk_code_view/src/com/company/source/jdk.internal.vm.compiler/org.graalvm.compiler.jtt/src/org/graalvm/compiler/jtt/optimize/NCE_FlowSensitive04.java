/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.jtt.optimize;

import org.junit.Test;

import org.graalvm.compiler.jtt.JTTTest;

/*
 */
public class NCE_FlowSensitive04 extends JTTTest {

    public static String test(String arg2) {
        String arg = arg2;
        if (arg == null) {
            arg = "null";
        }
        // arg cannot be null here
        return arg.toString();
    }

    @Test
    public void run0() throws Throwable {
        runTest("test", (Object) null);
    }

    @Test
    public void run1() throws Throwable {
        runTest("test", "x");
    }

    @Test
    public void run2() throws Throwable {
        runTest("test", "yay");
    }

}
