/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.hotspot.stubs;

import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.NativeCall;
import static org.graalvm.compiler.hotspot.GraalHotSpotVMConfigBase.INJECTED_OPTIONVALUES;
import static org.graalvm.compiler.hotspot.HotSpotForeignCallLinkage.Reexecutability.REEXECUTABLE;
import static org.graalvm.compiler.hotspot.HotSpotForeignCallLinkage.Transition.SAFEPOINT;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.clearPendingException;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.registerAsWord;
import static jdk.internal.vm.compiler.word.LocationIdentity.any;

import org.graalvm.compiler.api.replacements.Fold;
import org.graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import org.graalvm.compiler.graph.Node.ConstantNodeParameter;
import org.graalvm.compiler.graph.Node.NodeIntrinsic;
import org.graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import org.graalvm.compiler.hotspot.GraalHotSpotVMConfigBase;
import org.graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import org.graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl;
import org.graalvm.compiler.hotspot.meta.HotSpotProviders;
import org.graalvm.compiler.hotspot.nodes.DeoptimizeWithExceptionInCallerNode;
import org.graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import org.graalvm.compiler.hotspot.word.KlassPointer;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.options.OptionKey;
import org.graalvm.compiler.options.OptionType;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.replacements.nodes.CStringConstant;
import org.graalvm.compiler.word.Word;
import jdk.internal.vm.compiler.word.WordFactory;

import jdk.vm.ci.code.Register;

/**
 * Base class for stubs that create a runtime exception.
 */
public class CreateExceptionStub extends SnippetStub {

    public static class Options {
        @Option(help = "Testing only option that forces deopts for exception throws", type = OptionType.Expert)//
        public static final OptionKey<Boolean> HotSpotDeoptExplicitExceptions = new OptionKey<>(false);
    }

    protected CreateExceptionStub(String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super(snippetMethodName, options, providers, linkage);
    }

    @Fold
    static boolean reportsDeoptimization(@Fold.InjectedParameter GraalHotSpotVMConfig config) {
        return config.deoptBlobUnpackWithExceptionInTLS != 0;
    }

    @Fold
    static boolean alwayDeoptimize(@Fold.InjectedParameter OptionValues options) {
        return Options.HotSpotDeoptExplicitExceptions.getValue(options);
    }

    @Fold
    static String getInternalClassName(Class<?> cls) {
        return cls.getName().replace('.', '/');
    }

    private static Word classAsCString(Class<?> cls) {
        return CStringConstant.cstring(getInternalClassName(cls));
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception) {
        Word message = WordFactory.zero();
        return createException(threadRegister, exception, message);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, Word message) {
        Word thread = registerAsWord(threadRegister);
        int deoptimized = throwAndPostJvmtiException(THROW_AND_POST_JVMTI_EXCEPTION, thread, classAsCString(exception), message);
        return handleExceptionReturn(thread, deoptimized);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer klass) {
        Word thread = registerAsWord(threadRegister);
        int deoptimized = throwKlassExternalNameException(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, thread, classAsCString(exception), klass);
        return handleExceptionReturn(thread, deoptimized);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer objKlass, KlassPointer targetKlass) {
        Word thread = registerAsWord(threadRegister);
        int deoptimized = throwClassCastException(THROW_CLASS_CAST_EXCEPTION, thread, classAsCString(exception), objKlass, targetKlass);
        return handleExceptionReturn(thread, deoptimized);
    }

    private static Object handleExceptionReturn(Word thread, int deoptimized) {
        Object clearPendingException = clearPendingException(thread);
        if (alwayDeoptimize(INJECTED_OPTIONVALUES) || (reportsDeoptimization(GraalHotSpotVMConfigBase.INJECTED_VMCONFIG) && deoptimized != 0)) {
            DeoptimizeWithExceptionInCallerNode.deopt(clearPendingException);
        }
        return clearPendingException;
    }

    private static final ForeignCallDescriptor THROW_AND_POST_JVMTI_EXCEPTION = new ForeignCallDescriptor("throw_and_post_jvmti_exception", int.class, Word.class, Word.class, Word.class);
    private static final ForeignCallDescriptor THROW_KLASS_EXTERNAL_NAME_EXCEPTION = new ForeignCallDescriptor("throw_klass_external_name_exception", int.class, Word.class, Word.class,
                    KlassPointer.class);
    private static final ForeignCallDescriptor THROW_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("throw_class_cast_exception", int.class, Word.class, Word.class, KlassPointer.class,
                    KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native int throwAndPostJvmtiException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, Word message);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native int throwKlassExternalNameException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer klass);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native int throwClassCastException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer objKlass, KlassPointer targetKlass);

    public static void registerForeignCalls(GraalHotSpotVMConfig c, HotSpotForeignCallsProviderImpl foreignCalls) {
        foreignCalls.registerForeignCall(THROW_AND_POST_JVMTI_EXCEPTION, c.throwAndPostJvmtiExceptionAddress, NativeCall, SAFEPOINT, REEXECUTABLE, any());
        foreignCalls.registerForeignCall(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, c.throwKlassExternalNameExceptionAddress, NativeCall, SAFEPOINT, REEXECUTABLE, any());
        foreignCalls.registerForeignCall(THROW_CLASS_CAST_EXCEPTION, c.throwClassCastExceptionAddress, NativeCall, SAFEPOINT, REEXECUTABLE, any());
    }
}
